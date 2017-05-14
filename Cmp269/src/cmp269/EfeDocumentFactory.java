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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.configuration.ConfigurationException;

/**
 * @author diego
 */
public class EfeDocumentFactory extends PropertyBasedDocumentFactory {
    
    private static final long serialVersionUID = -1L;
    
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
//            fbis.close();
            String titleAbr = efeDocument.getTitle().trim();
            titleAbr = (titleAbr.length() > 20) ? titleAbr.substring(0,20) : titleAbr;
            String textAbr = efeDocument.getText().trim();
            textAbr = (textAbr.length() > 20) ? textAbr.substring(0,20) : textAbr;
//            System.out.println(String.format("%s,title=[%s],text=[%s]\n",efeDocument.getDocno(),titleAbr,textAbr));
            return efeDocument;
        } catch(IOException | IllegalStateException e) {
            Logger.getLogger(EfeDocumentFactory.class.getName()).log(Level.SEVERE, null, e);
            return null;
        }
    }

    @Override
    public DocumentFactory copy() {
        return this;
    }
}
