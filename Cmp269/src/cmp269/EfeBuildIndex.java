/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cmp269;

import it.unimi.di.mg4j.tool.IndexBuilder;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pressupoe que a coleção para indexação está em /tmp/efe/*.sgml
 * Prepara os descritores da coleção de documentos, serializa os objetos que
 * representam os documentos e construi os indices
 * 
 * @author diego
 */
public class EfeBuildIndex {

    private static final String STEMMER = "cmp269.SpanishStopwordStemmer";
    
    public static void main(String[] args) {
        try {
            // Parte inicial referente a indexação
            // Busca a lista de arquivos a serem indexados
            String[] files = new File("/tmp/efe/").list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".sgml");
                }
            });
            
            List<String> trecParams = new ArrayList();
            trecParams.add("-p");
            trecParams.add("encoding="+Cmp269.ENCODING);
            trecParams.add(Cmp269.COLLECTION);
            trecParams.add("-f");
            trecParams.add("cmp269.EfeDocumentFactory");
            for (String file : files) {
                trecParams.add("/tmp/efe/" + file);
            }
            String[] tParams = new String[trecParams.size()];
            tParams = trecParams.toArray(tParams);
            // Descomentar para indexar
            EfeDocumentCollection.main(tParams);
//            TRECDocumentCollection.main(tParams);
            String[] params = new String[] {
                "-S", Cmp269.COLLECTION, "-p", "encoding="+Cmp269.ENCODING, "-t", STEMMER,
                "-f", "cmp269.EfeDocumentFactory", "/tmp/efe"
            };
            
            // Descomentar para indexar
            IndexBuilder.main(params);
            // Fim da parte inicial referente a indexacao
        } catch(Exception e) {
            Logger.getLogger(Cmp269.class.getName()).log(Level.SEVERE, null, e);
        }
        
    }
    
}
