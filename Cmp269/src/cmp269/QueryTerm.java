/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cmp269;

import java.util.Objects;

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

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + Objects.hashCode(this.term);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final QueryTerm other = (QueryTerm) obj;
        if (!Objects.equals(this.term, other.term)) {
            return false;
        }
        return true;
    }
    
    
    
}
