/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cmp269;

/**
 *
 * @author diego
 */
public class QueryTerm implements Comparable<QueryTerm> {
    
    private String term;
    
    private Integer documentFrequency;

    public QueryTerm(String term, int documentFrequency) {
        this.term = term;
        this.documentFrequency = documentFrequency;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public int getDocumentFrequency() {
        return documentFrequency;
    }

    public void setDocumentFrequency(int documentFrequency) {
        this.documentFrequency = documentFrequency;
    }


    @Override
    public int compareTo(QueryTerm other) {
        return this.documentFrequency.compareTo(other.documentFrequency);
    }
    
}
