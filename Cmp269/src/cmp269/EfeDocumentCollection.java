package cmp269;

/*		 
 * MG4J: Managing Gigabytes for Java
 *
 * Copyright (C) 2006-2016 Sebastiano Vigna
 *
 *  This library is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as published by the Free
 *  Software Foundation; either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This library is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses/>.
 *
 */
import it.unimi.di.mg4j.document.PropertyBasedDocumentFactory.MetadataKeys;
import it.unimi.di.mg4j.util.MG4JClassParser;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.io.SegmentedInputStream;
import it.unimi.dsi.logging.ProgressLogger;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.UnflaggedOption;
import it.unimi.di.mg4j.document.AbstractDocumentCollection;
import it.unimi.di.mg4j.document.AbstractDocumentIterator;
import it.unimi.di.mg4j.document.Document;
import it.unimi.di.mg4j.document.DocumentFactory;
import it.unimi.di.mg4j.document.DocumentIterator;
import it.unimi.di.mg4j.document.PropertyBasedDocumentFactory;

/**
 * A collection for the TREC GOV2 data set.
 *
 * <p>
 * The documents are stored as a set of descriptors, representing the (possibly
 * gzipped) file they are contained in and the start and stop position in that
 * file. To manage descriptors later we rely on {@link SegmentedInputStream}.
 *
 * <p>
 * To interpret a file, we read up to <samp>&lt;DOC&gt;</samp> and place a start
 * marker there, we advance to the header and store the URI. An intermediate
 * marker is placed at the end of the doc header tag and a stop marker just
 * before <samp>&lt;/DOC&gt;</samp>.
 *
 * <p>
 * The resulting {@link SegmentedInputStream} has two segments per document. By
 * using a {@link it.unimi.di.mg4j.document.CompositeDocumentFactory}, the first
 * segment is parsed by a
 * {@link it.unimi.di.mg4j.document.TRECHeaderDocumentFactory}, whereas the
 * second segment is parsed by a user-provided factory&mdash;usually, an
 * {@link it.unimi.di.mg4j.document.HtmlDocumentFactory}.
 *
 * <p>
 * The collection provides both sequential access to all documents via the
 * iterator and random access to a given document. However, the two operations
 * are performed very differently as the sequential operation is much more
 * efficient than calling {@link #document(int)} repeatedly.
 *
 * @author Alessio Orlandi
 * @author Luca Natali
 */
public class EfeDocumentCollection extends AbstractDocumentCollection implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(EfeDocumentCollection.class);
    private static final long serialVersionUID = -4251461013312968454L;

    private static final boolean DEBUG = false;
    /**
     * Default buffer size, set up after some experiments.
     */
    public static final String DEFAULT_BUFFER_SIZE = "64Ki";

    /**
     * The list of the files containing the documents.
     */
    private String[] file;
    /**
     * Whether the files in {@link #file} are gzipped.
     */
    private final boolean useGzip;
    /**
     * The document factory.
     */
    protected DocumentFactory factory;
    /**
     * The list of document descriptors. We assume that descriptors within the
     * same file are contiguous
     */
    protected transient ObjectArrayList<TRECDocumentDescriptor> descriptors;
    /**
     * The buffer size.
     */
    private final int bufferSize;
    /**
     * The last returned stream.
     */
    private SegmentedInputStream lastStream;

    /**
     * A compact description of the location and of the internal segmentation of
     * a TREC document inside a file.
     */
    private static class TRECDocumentDescriptor implements Cloneable {

        /**
         * A reference to the file containing this document.
         */
        public int fileIndex;
        /**
         * The starting position of this document in the file.
         */
        public long startMarker;
        /**
         * The ending position.
         */
        public int stopMarkerDiff;

        // TODO: this computation should be moved in the caller
        public TRECDocumentDescriptor(int findex, long start, long stop) {
            this.fileIndex = findex;
            this.startMarker = start;
            this.stopMarkerDiff = (int) (stop - start);

        }

        public TRECDocumentDescriptor(int findex, long start,
                int stopMarkerDiff) {
            this.fileIndex = findex;
            this.startMarker = start;
            this.stopMarkerDiff = stopMarkerDiff;
        }

        public final long[] toSegments() {
            return new long[]{startMarker, stopMarkerDiff + startMarker};

        }

        public Object clone() {
            return new TRECDocumentDescriptor(this.fileIndex, this.startMarker,
                    this.stopMarkerDiff + this.startMarker);
        }

    }

    protected final static byte[] DOC_OPEN, DOC_CLOSE;

    static {
        try {
            DOC_OPEN = "<DOC>".getBytes("ASCII");
            DOC_CLOSE = "</DOC>".getBytes("ASCII");
        } catch (UnsupportedEncodingException cantHappen) {
            throw new RuntimeException(cantHappen);
        }
    }

    protected static boolean equals(byte[] a, int len, byte[] b) {
        if (len != b.length) {
            return false;
        }
        while (len-- != 0) {
            if (a[len] != b[len]) {
                return false;
            }
        }
        return true;
    }


    byte buffer[] = new byte[8 * 1024];

    private void parseContent(int fileIndex, InputStream is) throws IOException {
        long currStart, currStop, oldPos;
        boolean startedBlock = false;

        LOGGER.debug("Processing file " + fileIndex + " (" + file[fileIndex] + ")");

        FastBufferedInputStream fbis = new FastBufferedInputStream(is, bufferSize);

        currStart = 0; // make java compiler happy.
        oldPos = 0;

        int l;

        while ((l = fbis.readLine(buffer)) != -1) {
            if (l == buffer.length) {
                // We filled the buffer, which means we have a very very long line. Let's skip it.
                while ((l = fbis.readLine(buffer)) == buffer.length);
            } else {
                if (!startedBlock && equals(buffer, l, DOC_OPEN)) {
                    currStart = oldPos;
                    startedBlock = true; // Start of the current block (includes <DOC> marker)
                } else if (startedBlock && equals(buffer, l, DOC_CLOSE)) {
                    currStop = oldPos;
//                    currStop = oldPos + DOC_CLOSE.length;
//                    currStop = fbis.position();
                    if (DEBUG) {
                        LOGGER.debug("Setting markers <" + currStart + "," + currStop + ">");
                    }
                    descriptors.add(new TRECDocumentDescriptor(fileIndex, currStart, currStop));
                    startedBlock = false;
                }
                oldPos = fbis.position();
            }
        }

        fbis.close();
    }

    /**
     * Copy constructor (that is, the one used by {@link #copy()}. Just
     * initializes final fields
     */
    protected EfeDocumentCollection(String[] file, DocumentFactory factory, ObjectArrayList<TRECDocumentDescriptor> descriptors, int bufferSize, boolean useGzip) {
        this.useGzip = useGzip;
        this.file = file;
        this.bufferSize = bufferSize;
        this.factory = factory;
        this.descriptors = descriptors;
    }

    public EfeDocumentCollection copy() {
        return new EfeDocumentCollection(file, factory.copy(), descriptors, bufferSize, useGzip);
    }

    private final InputStream openFileStream(String fileName) throws IOException {
        final InputStream s = new FileInputStream(fileName);
        if (useGzip) {
            return new GZIPInputStream(s);
        } else {
            return s;
        }
    }

    /**
     * Creates a new TREC collection by parsing the given files.
     *
     * @param file an array of file names containing documents in TREC GOV2
     * format.
     * @param factory the document factory (usually, a composite one).
     * @param bufferSize the buffer size.
     * @param useGzip true iff the files are gzipped.
     */
    public EfeDocumentCollection(String[] file, DocumentFactory factory, int bufferSize, boolean useGzip) throws IOException {
        this.file = file;
        this.factory = factory;
        this.bufferSize = bufferSize;
        this.descriptors = new ObjectArrayList<TRECDocumentDescriptor>();
        this.useGzip = useGzip;

        final ProgressLogger progressLogger = new ProgressLogger(LOGGER);
        progressLogger.expectedUpdates = file.length;
        progressLogger.itemsName = "files";

        progressLogger.start("Parsing " + (useGzip ? "GZip" : "plain") + " files");

        for (int i = 0; i < file.length; i++) {
            parseContent(i, openFileStream(file[i]));
            progressLogger.update();
        }

        progressLogger.done();
    }

    public int size() {
        return descriptors.size();
    }

    public Document document(int n) throws IOException {
        Reference2ObjectMap<Enum<?>, Object> metadata = metadata(n);
        return factory.getDocument(stream(n), metadata);
    }

    public InputStream stream(final int n) throws IOException {
        // Creates a Segmented Input Stream with only one segment in (the requested one).
        ensureDocumentIndex(n);
        IOUtils.closeQuietly(lastStream);
        final TRECDocumentDescriptor descr = descriptors.get(n);
        return lastStream = new SegmentedInputStream(openFileStream(file[descr.fileIndex]), descr.toSegments());
    }

    public Reference2ObjectMap<Enum<?>, Object> metadata(final int index) {
        ensureDocumentIndex(index);
        final Reference2ObjectArrayMap<Enum<?>, Object> metadata = new Reference2ObjectArrayMap<Enum<?>, Object>(4);

        metadata.put(MetadataKeys.URI, "Document #" + index);
        return metadata;
    }

    public DocumentFactory factory() {
        return this.factory;
    }

    public void close() throws IOException {
        super.close();
        if (lastStream != null) {
            lastStream.close();
        }
        descriptors = null;
    }

    /**
     * Merges a new collection in this one, by rebuilding the gzFile array and
     * appending the other object one, concatenating the descriptors while
     * rebuilding all.
     * <p>
     * It is supposed that the passed object contains no duplicates for the
     * local collection.
     */
    public void merge(EfeDocumentCollection other) {
        int oldLength = this.file.length;

        this.file = ObjectArrays.ensureCapacity(this.file, this.file.length + other.file.length);
        System.arraycopy(other.file, 0, this.file, oldLength, other.file.length);

        ObjectIterator<TRECDocumentDescriptor> iter = other.descriptors.iterator();
        while (iter.hasNext()) {
            final TRECDocumentDescriptor tdd = (TRECDocumentDescriptor) iter.next().clone();
            tdd.fileIndex += oldLength;
            this.descriptors.add(tdd);
        }
    }

    public DocumentIterator iterator() throws IOException {
        return new AbstractDocumentIterator() {
            /**
             * An iterator returning the descriptors of the documents in the
             * enveloping collection.
             */
            private final ObjectIterator<TRECDocumentDescriptor> descriptorIterator = descriptors.iterator();
            /**
             * The current stream.
             */
            private SegmentedInputStream siStream;
            /**
             * The current document.
             */
            private int currentDocument = 0;
            /**
             * The last returned document.
             */
            private Document last;
            /**
             * The first descriptor of the next file, if any, or
             * <code>null</code> if nextFile() has never been called.
             */
            private TRECDocumentDescriptor firstNextDescriptor;

            private boolean nextFile() throws FileNotFoundException, IOException {
                if (size() == 0) {
                    return false;
                }
                IOUtils.closeQuietly(siStream);
                if (!descriptorIterator.hasNext()) {
                    return false;
                }

                /*
                * We assume documents contained in the same gzip file are
                * contiguous so we collect all of them until we find a different
                * file index.
                 */
                TRECDocumentDescriptor currentDescriptor = firstNextDescriptor != null ? firstNextDescriptor : descriptorIterator.next();
                int currentFileIndex = currentDescriptor.fileIndex;

                if (DEBUG) {
                    LOGGER.debug("Skipping to contents file " + currentFileIndex + " (" + file[currentFileIndex] + ")");
                }

                /*
                * We create the segmented input stream with all just collected
                * descriptors
                 */
                siStream = new SegmentedInputStream(openFileStream(file[currentFileIndex]));

                do {
                    siStream.addBlock(currentDescriptor.toSegments());
                    if (!descriptorIterator.hasNext()) {
                        break;
                    }
                    currentDescriptor = descriptorIterator.next();
                } while (currentDescriptor.fileIndex == currentFileIndex);

                firstNextDescriptor = currentDescriptor; // The last assignment will be meaningless, but it won't be used anyway
                return true;
            }

            @Override
            public Document nextDocument() throws IOException {
                /* If necessary, skip to the next segment, else, try skipping to the next gzip file. */
                if (DEBUG) {
                    LOGGER.debug("nextDocument() has been called ");
                }

                if (last != null) {
                    last.close();
                    if (!siStream.hasMoreBlocks()) {
                        if (!nextFile()) {
                            return last = null;
                        }
                    } else {
                        siStream.nextBlock();
                    }
                } else if (!nextFile()) {
                    return null; // First call
                }
                return last = factory.getDocument(siStream, metadata(currentDocument++));
            }

            public void close() throws IOException {
                if (siStream != null) {
                    if (last != null) {
                        last.close();
                    }
                    super.close();
                    siStream.close();
                    siStream = null;
                }
            }
        };
    }

    private void readObject(final ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();

        final int size = s.readInt();
        final ObjectArrayList<TRECDocumentDescriptor> descriptors = new ObjectArrayList<TRECDocumentDescriptor>();
        descriptors.ensureCapacity(size);
        for (int i = 0; i < size; i++) {
            descriptors.add(new TRECDocumentDescriptor(s.readInt(), s.readLong(), s.readInt()));
        }
        this.descriptors = descriptors;
    }

    private void writeObject(final ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeInt(descriptors.size());

        for (TRECDocumentDescriptor descriptor : descriptors) {
            s.writeInt(descriptor.fileIndex);
            s.writeLong(descriptor.startMarker);
            s.writeInt(descriptor.stopMarkerDiff);
        }
    }

    public static void main(String[] arg) throws IOException, JSAPException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        SimpleJSAP jsap = new SimpleJSAP(
                EfeDocumentCollection.class.getName(), "Saves a serialised TREC document collection based on a set of file names (which will be sorted lexicographically).",
                new Parameter[]{
                    new FlaggedOption("factory", MG4JClassParser.getParser(), EfeDocumentFactory.class.getName(), JSAP.NOT_REQUIRED, 'f', "factory", "A document factory with a standard constructor."),
                    new FlaggedOption("property", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'p', "property", "A 'key=value' specification, or the name of a property file").setAllowMultipleDeclarations(true),
                    new Switch("gzipped", 'z', "gzipped", "The files are gzipped."),
                    new Switch("unsorted", 'u', "unsorted", "Keep the file list unsorted."),
                    new FlaggedOption("bufferSize", JSAP.INTSIZE_PARSER, DEFAULT_BUFFER_SIZE, JSAP.NOT_REQUIRED, 'b', "buffer-size", "The size of an I/O buffer."),
                    new UnflaggedOption("collection", JSAP.STRING_PARSER, JSAP.REQUIRED, "The filename for the serialised collection."),
                    new UnflaggedOption("file", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, JSAP.GREEDY, "A list of files that will be indexed. If missing, a list of files will be read from standard input.")
                });

        JSAPResult jsapResult = jsap.parse(arg);
        if (jsap.messagePrinted()) {
            return;
        }

        final DocumentFactory userFactory = PropertyBasedDocumentFactory.getInstance(jsapResult.getClass("factory"), jsapResult.getStringArray("property"));

        String[] file = jsapResult.getStringArray("file");
        if (file.length == 0) {
            final ObjectArrayList<String> files = new ObjectArrayList<String>();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            String s;
            while ((s = bufferedReader.readLine()) != null) {
                files.add(s);
            }
            file = files.toArray(new String[0]);
        }

        // To avoid problems with find and similar utilities, we sort the file names
        if (!jsapResult.getBoolean("unsorted")) {
            Arrays.sort(file);
        }

//        final DocumentFactory composite = CompositeDocumentFactory.getFactory(new TRECHeaderDocumentFactory(), userFactory);

        if (file.length == 0) {
            System.err.println("WARNING: empty file set.");
        }
        BinIO.storeObject(new EfeDocumentCollection(file, userFactory, jsapResult.getInt("bufferSize"), jsapResult.getBoolean("gzipped")), jsapResult.getString("collection"));
    }
}
