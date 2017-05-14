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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author diego
 */
@XmlRootElement(name="DOC")
public class EfeDocument extends AbstractDocument {
    
    private String docno = "";
    private String docid = "";
    private String date = "";
    private String time = "";
    private String scate = "";
    private String ficheros = "";
    private String destino = "";
    private String category = "";
    private String clave = "";
    private String num = "";
    private String prioridad = "";
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
        return title;
    }

    @Override
    public CharSequence uri() {
        return (CharSequence) metadata.get(PropertyBasedDocumentFactory.MetadataKeys.URI);
//        return "[no uri]";
    }

    @Override
    public Object content( final int field ) throws IOException {
        switch(field) {
            case 0:
                return new StringReader(docno);
            case 1:
                return new StringReader(docid);
            case 2:
                return new StringReader(date);
            case 3:
                return new StringReader(time);
            case 4:
                return new StringReader(scate);
            case 5:
                return new StringReader(ficheros);
            case 6:
                return new StringReader(destino);
            case 7:
                return new StringReader(category);
            case 8:
                return new StringReader(clave);
            case 9:
                return new StringReader(num);
            case 10:
                return new StringReader(prioridad);
            case 11:
                return new StringReader(title);
            case 12:
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

    @XmlElement(name="DOCNO")
    public void setDocno(String docno) {
        this.docno = docno.trim();
    }

    public String getDocid() {
        return docid;
    }

    @XmlElement(name="DOCID")
    public void setDocid(String docid) {
        this.docid = docid.trim();
    }

    public String getDate() {
        return date;
    }

    @XmlElement(name="DATE")
    public void setDate(String date) {
        this.date = date.trim();
    }

    public String getTime() {
        return time;
    }

    @XmlElement(name="TIME")
    public void setTime(String time) {
        this.time = time.trim();
    }

    public String getScate() {
        return scate;
    }

    @XmlElement(name="SCATE")
    public void setScate(String scate) {
        this.scate = scate.trim();
    }

    public String getFicheros() {
        return ficheros;
    }

    @XmlElement(name="FICHEROS")
    public void setFicheros(String ficheros) {
        this.ficheros = ficheros.trim();
    }

    public String getDestino() {
        return destino;
    }

    @XmlElement(name="DESTINO")
    public void setDestino(String destino) {
        this.destino = destino.trim();
    }

    public String getCategory() {
        return category;
    }

    @XmlElement(name="CATEGORY")
    public void setCategory(String category) {
        this.category = category.trim();
    }

    public String getClave() {
        return clave;
    }

    @XmlElement(name="CLAVE")
    public void setClave(String clave) {
        this.clave = clave.trim();
    }

    public String getNum() {
        return num;
    }

    @XmlElement(name="NUM")
    public void setNum(String num) {
        this.num = num.trim();
    }

    public String getPrioridad() {
        return prioridad;
    }

    @XmlElement(name="PRIORIDAD")
    public void setPrioridad(String prioridad) {
        this.prioridad = prioridad.trim();
    }

    public String getTitle() {
        return title;
    }

    @XmlElement(name="TITLE")
    public void setTitle(String title) {
        this.title = title.trim();
    }

    public String getText() {
        return text;
    }

    @XmlElement(name="TEXT")
    public void setText(String text) {
        this.text = text.trim();
    }

}
