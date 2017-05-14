/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cmp269;

import it.unimi.di.mg4j.document.AbstractDocument;
import it.unimi.di.mg4j.document.PropertyBasedDocumentFactory;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.io.WordReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

/**
 *
 * @author diego
 */
public class EfeDocument extends AbstractDocument {
    
    private String docno = "";
    private String title = "";
    private String text = "";
    
    public static final String[] FIELD_NAME = { "docno", "title", "text" };
    /**
     * The map from field names to field indices.
     */
    public static final Object2IntOpenHashMap<String> FIELD2INDEX = new Object2IntOpenHashMap<>(FIELD_NAME.length, .5f);

    static {
        FIELD2INDEX.defaultReturnValue(-1);
        for (int i = 0; i < FIELD_NAME.length; i++) {
            FIELD2INDEX.put(FIELD_NAME[i], i);
        }
    }

    protected final Reference2ObjectMap<Enum<?>,Object> metadata;
    protected final InputStream rawContent;
    
    protected EfeDocument(final InputStream rawContent, final Reference2ObjectMap<Enum<?>,Object> metadata) {
        this.metadata = metadata;
        this.rawContent = rawContent;
    }

    @Override
    public CharSequence title() {
        return title;
    }

    @Override
    public String toString() {
        return docno;
    }

    @Override
    public CharSequence uri() {
        return (CharSequence) metadata.get(PropertyBasedDocumentFactory.MetadataKeys.URI);
    }

    @Override
    public Object content( final int field ) throws IOException {
        switch(field) {
            case 0:
                return new StringReader(docno);
            case 1:
                return new StringReader(title);
            case 2:
                return new StringReader(text);
            default:
                throw new IOException("Campo invalido");
        }   
    }

    @Override
    public WordReader wordReader(int i) {
        return new FastBufferedReader();
    }
    
    /* ********************* Getters and Setters **************************** */
    
    public String getDocno() {
        return docno;
    }

    public void setDocno(String docno) {
        this.docno = docno.trim();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title.trim();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text.trim();
    }

}
