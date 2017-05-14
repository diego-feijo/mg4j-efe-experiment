package cmp269;

import it.unimi.di.mg4j.document.Document;
import it.unimi.di.mg4j.document.DocumentFactory;
import it.unimi.di.mg4j.document.PropertyBasedDocumentFactory;
import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.io.WordReader;
import it.unimi.dsi.util.Properties;
import java.io.EOFException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.configuration.ConfigurationException;

/**
 * @author diego
 */
public class EfeDocumentFactory extends PropertyBasedDocumentFactory {
    
    private static final long serialVersionUID = -1L;
    
    public EfeDocumentFactory(String s) {
    }
    
    /**
     * The word reader used for all documents.
     */
    private WordReader wordReader = new FastBufferedReader();
    
    public EfeDocumentFactory( final Properties properties ) throws ConfigurationException {
        super( properties );
    }

    public EfeDocumentFactory( final Reference2ObjectMap<Enum<?>,Object> defaultMetadata ) {
        super( defaultMetadata );
    }

    public EfeDocumentFactory( final String[] property ) throws ConfigurationException {
        super( property );
    }

    public EfeDocumentFactory() {
        super();
    }
    
    
    @Override
    public int numberOfFields() {
        return EfeDocument.FIELD_NAME.length;
    }

    @Override
    public String fieldName( int fieldIndex ) {
        return EfeDocument.FIELD_NAME[fieldIndex];
    }

    @Override
    public int fieldIndex( String fieldName ) {
        return EfeDocument.FIELD2INDEX.getInt(fieldName);
    }

    public DocumentFactory.FieldType fieldType( int fieldIndex ) {
        return FieldType.TEXT;
    }
    
    private byte buffer[] = new byte[ 8 * 1024 ];

    protected static boolean startsWith(byte[] a, byte[] b) {
        if (a.length < b.length) {
            return false;
        }
        for (int i=0;i<b.length;i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public Document getDocument( InputStream rawContent, Reference2ObjectMap<Enum<?>,Object> metadata ) throws IOException {
        
//        SegmentedInputStream seg = (SegmentedInputStream) rawContent;
        final FastBufferedInputStream fbis = new FastBufferedInputStream( rawContent );
        try {
            int l = fbis.readLine( buffer );
            if ( l < 0 ) throw new EOFException();
            if ( ! EfeDocumentCollection.equals( buffer, l, "<DOC>".getBytes() ) ) throw new IllegalStateException ( "Document does not start with <DOC>: " + new String( buffer, 0, l ) );
            Charset cs = Charset.forName("ISO-8859-1");
            boolean inTitle = false;
            boolean inText = false;
            StringBuilder title = new StringBuilder();
            StringBuilder text = new StringBuilder();
            EfeDocument efeDocument = new EfeDocument(rawContent,metadata);
            while ( ( l = fbis.readLine( buffer ) ) != -1 ) {
                ByteBuffer bb = ByteBuffer.allocateDirect(l);
                byte[] slice = Arrays.copyOfRange(buffer, 0, l);
                bb.put(slice);
                bb.position(l);
                bb.flip();
                CharBuffer cb = cs.decode(bb.slice());
                String s = cb.toString();
                if (s.startsWith("<DOCNO>")) {
                    efeDocument.setDocno(s.substring("<DOCNO>".length(), s.length()-"</DOCNO>".length()));
                } else if (s.startsWith("<DOCID>")) {
                    if (s.startsWith("<DOCID>EFE19940103-00919")) {
                        String sa = "";
                    }
                    efeDocument.setDocid(s.substring("<DOCID>".length(), s.length()-"</DOCID>".length()));
//                } else if (s.startsWith("<DATE>")) {
//                    efeDocument.setDate(s.substring("<DATE>".length(), s.length()-"</DATE>".length()));
//                } else if (s.startsWith("<TIME>")) {
//                    efeDocument.setTime(s.substring("<TIME>".length(), s.length()-"</TIME>".length()));
//                } else if (s.startsWith("<SCATE>")) {
//                    efeDocument.setScate(s.substring("<SCATE>".length(), s.length()-"</SCATE>".length()));
//                } else if (s.startsWith("<FICHEROS>")) {
//                    efeDocument.setFicheros(s.substring("<FICHEROS>".length(), s.length()-"</FICHEROS>".length()));
//                } else if (s.startsWith("<DESTINO>")) {
//                    efeDocument.setDestino(s.substring("<DESTINO>".length(), s.length()-"</DESTINO>".length()));
//                } else if (s.startsWith("<CATEGORY>")) {
//                    efeDocument.setCategory(s.substring("<CATEGORY>".length(), s.length()-"</CATEGORY>".length()));
//                } else if (s.startsWith("<CLAVE>")) {
//                    efeDocument.setClave(s.substring("<CLAVE>".length(), s.length()-"</CLAVE>".length()));
//                } else if (s.startsWith("<NUM>")) {
//                    efeDocument.setNum(s.substring("<NUM>".length(), s.length()-"</NUM>".length()));
//                } else if (s.startsWith("<PRIORIDAD>")) {
//                    efeDocument.setPrioridad(s.substring("<PRIORIDAD>".length(), s.length()-"</PRIORIDAD>".length()));
                } else if (!inTitle && s.startsWith("<TITLE>")) {
                    inTitle = true;
                    title.append(s.substring("<TITLE>".length()));
                } else if (inTitle) {
                    if (s.startsWith("</TITLE>")) {
                        inTitle = false;
                        efeDocument.setTitle(title.toString());
                        metadata.put(MetadataKeys.TITLE, title.toString());
                    } else {
                        title.append(s);
                    }
                } else if (!inText && s.startsWith("<TEXT>")) {
                    inText = true;
                    text.append(s.substring("<TEXT>".length()));
                } else if (inText) {
                    if (s.startsWith("</TEXT>")) {
                        inText = false;
                        efeDocument.setText(text.toString());
                    } else {
                        text.append(s);
                    }
                }
            }
            fbis.close();
        
        /*try (InputStreamReader isr = new InputStreamReader(rawContent, Charset.forName("ISO-8859-1"));
                BufferedReader br = new BufferedReader(isr)) {
            StringBuilder s = new StringBuilder();
            for (String line; (line = br.readLine()) != null;) {
                s.append(line);
            }*/
//            System.out.println("full"+s.toString());
//            JAXBContext jc = JAXBContext.newInstance(EfeDocument.class);
//            Unmarshaller unmarshaller = jc.createUnmarshaller();
//            StringReader sr = new StringReader(s.toString());
//            EfeDocument efeDocument = (EfeDocument) unmarshaller.unmarshal(sr);
            System.out.println(String.format("docno=%s,docid=%s,title=%s",efeDocument.getDocno(), efeDocument.getDocid(), efeDocument.getTitle()));
            return efeDocument;
//        } catch (JAXBException ex) {
//            Logger.getLogger(EfeDocumentFactory.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        return null;
        } catch(Exception e) {
            Logger.getLogger(EfeDocumentFactory.class.getName()).log(Level.SEVERE, null, e);
            return null;
        }
    }

    @Override
    public DocumentFactory copy() {
        return this;
    }
}
