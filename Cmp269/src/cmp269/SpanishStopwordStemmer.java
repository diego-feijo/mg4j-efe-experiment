/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cmp269;

import it.unimi.di.mg4j.index.snowball.SpanishStemmer;
import it.unimi.dsi.lang.MutableString;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author diego
 */
public class SpanishStopwordStemmer extends SpanishStemmer {
    
    private HashSet<MutableString> STOPWORDS;
    
    public SpanishStopwordStemmer() {
        try {
            List<MutableString> lines;
            try (InputStream in = getClass().getResourceAsStream("/stopwords.txt"); BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                String line = null;
                lines = new ArrayList();
                while ((line = reader.readLine()) != null) {
                    lines.add(new MutableString(line));
                }              }
            STOPWORDS = new HashSet(lines);
        } catch (IOException ex) {
            Logger.getLogger(SpanishStopwordStemmer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public boolean processTerm( final MutableString term ) {
        if (term != null) {
            if (STOPWORDS.contains(term)) {
                return false;
            } else {
                return super.processTerm(term);
            }
        } else {
            return false;
        }
    }
    
}
