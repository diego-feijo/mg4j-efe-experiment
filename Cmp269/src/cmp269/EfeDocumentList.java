/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cmp269;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

/**
 *
 * @author diego
 */
public class EfeDocumentList {
    
    private final Charset cs;
    
    private final File file;
    
    private List<EfeDocument> list;
    
    public EfeDocumentList(File file, Charset cs) {
        this.file = file;
        this.cs = cs;
    }

    public File getFile() {
        return file;
    }

    public List<EfeDocument> getList() {
        return list;
    }

    public void setList(List<EfeDocument> list) {
        this.list = list;
    }
    
    public void process() throws Exception {
        try (FileInputStream fis = new FileInputStream(file);
                InputStreamReader isr = new InputStreamReader(fis, cs);
                BufferedReader br = new BufferedReader(isr)) {
            StringBuilder buffer = new StringBuilder();
            for (String line; (line = br.readLine()) != null;) {
                buffer.append(line);
                if (line.startsWith("</DOC>")) {
                    JAXBContext jc = JAXBContext.newInstance(EfeDocument.class);
                    Unmarshaller unmarshaller = jc.createUnmarshaller();
                    System.out.println(buffer);
                    StringReader sr = new StringReader(buffer.toString());
                    list.add((EfeDocument) unmarshaller.unmarshal(sr));
                    
                    // Recreate buffer
                    buffer = new StringBuilder();
                }
                
            }
        }
        
    }
    
}
