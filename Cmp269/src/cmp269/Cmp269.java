package cmp269;

import it.unimi.di.mg4j.document.AbstractDocumentSequence;
import it.unimi.di.mg4j.document.DocumentCollection;
import it.unimi.di.mg4j.index.Index;
import it.unimi.di.mg4j.index.TermProcessor;
import static it.unimi.di.mg4j.query.Query.MAX_STEMMING;
import it.unimi.di.mg4j.query.QueryEngine;
import it.unimi.di.mg4j.query.SelectedInterval;
import it.unimi.di.mg4j.query.nodes.QueryBuilderVisitorException;
import it.unimi.di.mg4j.query.parser.QueryParserException;
import it.unimi.di.mg4j.query.parser.SimpleParser;
import it.unimi.di.mg4j.search.DocumentIteratorBuilderVisitor;
import it.unimi.di.mg4j.search.score.BM25Scorer;
import it.unimi.di.mg4j.search.score.DocumentScoreInfo;
import it.unimi.di.mg4j.search.score.Scorer;
import it.unimi.di.mg4j.search.score.VignaScorer;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.lang.MutableString;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.apache.commons.configuration.ConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Adaptado de it.unimi.di.mg4j.query.Query:
 * 
 * Criar o indice, executa as pesquisas e escreve o resultado em arquivo de texto.
 * Pressupoe que a coleção para indexação está em /tmp/efe/*.sgml
 * Escreve arquivo de saida em /tmp/output.txt
 * 
 * As consultas tem o formato: 
 * <top>
 * <num> 142 </num>
 * <ES-title> Christo envuelve el edificio del Reichstag alemán </ES-title>
 * <ES-desc> Encontrar documentos que hablen de este acto del artista alemán Christo en el Reichstag alemán en Berlín. </ES-desc>
 * <ES-narr> El artista Christo tardó dos semanas en junio de 1995 en envolver el Reichstag alemán utilizando una enorme cantidad de material. Encontrar documentos sobre este evento artístico. Cualquier información sobre los preparativos o el evento mismo es relevante, incluyendo los debates políticos, las decisiones y los preparativos técnicos. </ES-narr>
 * </top>
 * O programa busca o numero da consulta, o titulo e a descricao. 
 * Faz o stemming e retira os stopwords. Para cada termo que restou, contabiliza
 * e ordena os termos pela quantidade de documentos em que o termo aparece.
 * Tenta executar a consulta com todos os termos, se nao conseguir, vai retirando
 * os termos que aparece em mais documentos e refazendo a consulta até encontrar
 * algum termo em que a consulta é bem sucedida.
 *
 * @author diego
 */
public class Cmp269 {

    private static final String ENCODING = "ISO-8859-1";
    private static final String STEMMER = "cmp269.SpanishStopwordStemmer";
    private static final String FILENAME = "/tmp/output.txt";
    
    /** 
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            fw = new FileWriter(FILENAME);
            bw = new BufferedWriter(fw);

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
            trecParams.add("encoding="+ENCODING);
            trecParams.add("/tmp/efe.collection");
            for (String file : files) {
                trecParams.add("/tmp/efe/" + file);
            }
            String[] tParams = new String[trecParams.size()];
            tParams = trecParams.toArray(tParams);
            // Descomentar para indexar
//            TRECDocumentCollection.main(tParams);
            String[] params = new String[] {
                "-S", "/tmp/efe.collection", "-p", "encoding="+ENCODING, "-t", STEMMER, "efe"
            };
            
            // Descomentar para indexar
//            IndexBuilder.main(params);
            // Fim da parte inicial referente a indexacao

            // Busca e configura os parametros do indice
            // Defina campos, processadores de termos, encoding, tudo buscando os
            // metadados do indice já gerado
            final DocumentCollection documentCollection = (DocumentCollection) AbstractDocumentSequence.load( "/tmp/efe.collection" );
            final Object2ReferenceLinkedOpenHashMap<String,Index> indexMap = new Object2ReferenceLinkedOpenHashMap<>( Hash.DEFAULT_INITIAL_SIZE, .5f );
            final Reference2DoubleOpenHashMap<Index> index2Weight = new Reference2DoubleOpenHashMap<>();
            final String[] basenameWeight = new String[] { "efe-text" };
            final boolean loadSizes = true;
            loadIndicesFromSpec( basenameWeight, loadSizes, documentCollection, indexMap, index2Weight );
            final Object2ObjectOpenHashMap<String,TermProcessor> termProcessors = new Object2ObjectOpenHashMap<>( indexMap.size() );
            for( String alias: indexMap.keySet() ) {
                termProcessors.put( alias, indexMap.get( alias ).termProcessor );
            }
            final SimpleParser simpleParser = new SimpleParser( indexMap.keySet(), indexMap.firstKey(), termProcessors );
            final Reference2ReferenceMap<Index, Object> index2Parser = new Reference2ReferenceOpenHashMap<>();
            final QueryEngine queryEngine = new QueryEngine( simpleParser, new DocumentIteratorBuilderVisitor( indexMap, index2Parser, indexMap.get( indexMap.firstKey() ), MAX_STEMMING ), indexMap );
            queryEngine.setWeights( index2Weight );
            queryEngine.score( new Scorer[] { new BM25Scorer(), new VignaScorer() }, new double[] { 1, 1 } );
            queryEngine.equalize( 1000 );
            final ObjectArrayList<DocumentScoreInfo<Reference2ObjectMap<Index,SelectedInterval[]>>> results = new ObjectArrayList<DocumentScoreInfo<Reference2ObjectMap<Index,SelectedInterval[]>>>();

            // Classe especial com Stemming e stopwords, pode ser substituida
            // pela SpanishStemmer
            SpanishStopwordStemmer stemmer = new SpanishStopwordStemmer();

            // Processar as consultas
            // ler as consultas
            final String QUERYFILE = "/tmp/Consultas.txt";
            InputStream inputStream = new FileInputStream(QUERYFILE);
            Reader reader = new InputStreamReader(inputStream, ENCODING);
            InputSource is = new InputSource(reader);
            is.setEncoding(ENCODING);
            
            // Fazer o parse do arquivo XML de consultas
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbFactory.newDocumentBuilder();
            Document doc = builder.parse(is);
            

            TreeSet<QueryTerm> queryTerms = new TreeSet();
            NodeList consultas = doc.getElementsByTagName("top");
            // Para cada consulta (elemento <top>)
            for (int i = 0; i < consultas.getLength(); i++) {
                Node node = consultas.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    NodeList fields = node.getChildNodes();
                    String q = "";
                    int num = -1;
                    // Agora, varre dentro do elemento <top> para encontrar os 
                    // dados numero da consulta, titulo e descricao
                    for (int j = 0; j < fields.getLength(); j++) {
                        Node child = fields.item(j);
                        if (child.getNodeName().equalsIgnoreCase("num")) {
                            // Encontrou o numero
                            num = Integer.parseInt(child.getTextContent().trim());
                        } else if (child.getNodeName().equalsIgnoreCase("ES-title") ||
                                child.getNodeName().equalsIgnoreCase("ES-desc")) {
                            // Encontrou o titulo ou descricao, adiciona para consulta
                            String t = child.getTextContent().replaceAll("[^\\w\\s]"," ");
                            // Remove caracteres especias como $%&#@|
                            q += t.replaceAll("\\s+", " ").trim() + " ";
                        }
                    }
                    // A consulta agora é uma longa sequencia de termos contendo
                    // stopwords e sem stemming
                    q = q.trim();
                    
                    // Processar termo por termo
                    for (String t : q.split("\\s+")) {
                        MutableString m = new MutableString(t);
                        if (stemmer.processTerm(m)) {
                            // Mas apenas adiciona os termos que nao forem stopwords
                            // e que tenham tamanho maior que 1 e que consigam
                            // recuperar algum resultado
                            if (m.length() > 1) {
                                try {
                                    queryEngine.process(m.toString(), 0, 100, results);
                                    if (results.size() > 0) {
                                        QueryTerm queryTerm = new QueryTerm(m.toString(), results.size());
                                        queryTerms.add(queryTerm);
                                    }
                                } catch(QueryBuilderVisitorException | QueryParserException | IOException e) {
                                }
                            }
                        }
                    }
                    
                    // Nesse ponto, queryTerms tem os termos na ordem para ser processados
                    // Vamos tentar processar todos os termos da query
                    // Em caso de não encontrar documentos, usamos a lista prioritária
                    // para descartar termos menos relevantes
                    q = format(queryTerms);
                    try {
                        while (results.isEmpty() && !queryTerms.isEmpty()) {
                            queryEngine.process(q, 0, 100, results);
                            // Termo que aparece em mais documentos (menos importante primeiro)
                            QueryTerm last = queryTerms.last();
                            queryTerms.remove(last);
                        }
                        if (results.isEmpty()) {
                            Logger.getLogger(Cmp269.class.getName()).log(Level.SEVERE, "Nenhum termo da pesquisa pode ser usado: {0}", num);
                        } else {
                            // Finalmente, encontramos resultados usando o maior numero possivel de termos
                            System.out.println(String.format("Consulta: [%d] Query Usada: [%s] Resultados: [%d]", num, q, results.size()));
                            DocumentScoreInfo<Reference2ObjectMap<Index,SelectedInterval[]>> dsi;
                            for(int j = 0; j < results.size(); j++) {
                                dsi = results.get(j);
                                it.unimi.di.mg4j.document.Document d = documentCollection.document(dsi.document);
                                // Escreve no arquivo de saida os dados da consulta
                                bw.write(String.format(Locale.US, "%d Q0 %s %d %f %s\n", num, d.title().toString().trim(), j, dsi.score, "diego"));
                                d.close();
                            }
                        }
                    } catch(QueryParserException | QueryBuilderVisitorException | IOException e) {
                        Logger.getLogger(Cmp269.class.getName()).log(Level.SEVERE, null, e);
                    }
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(Cmp269.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try { if (bw != null) bw.close(); } catch(Exception e) {};
            try { if (fw != null) fw.close(); } catch(Exception e) {};
        }
    }
    
    private static String format(TreeSet<QueryTerm> queryTerms) {
        StringBuilder s = new StringBuilder();
        for (QueryTerm queryTerm : queryTerms) {
            s.append(queryTerm.getTerm()).append(" ");
        }
        return s.toString().trim();
    }
    
    
    /** Parses a given array of index URIs/weights, loading the correspoding indices
     * and writing the result of parsing in the given maps.
     * 
     * @param basenameWeight an array of index URIs of the form <samp><var>uri</var>[:<var>weight</var>]</samp>, specifying
     * the URI of an index and the weight for the index (1, if missing).
     * @param loadSizes forces size loading.
     * @param documentCollection an optional document collection, or <code>null</code>.
     * @param name2Index an empty, writable map that will be filled with pairs given by an index basename (or field name, if available) and an {@link Index}.
     * @param index2Weight an empty, writable map that will be filled with a map from indices to respective weights.
     */
    private static void loadIndicesFromSpec( final String[] basenameWeight, boolean loadSizes, final DocumentCollection documentCollection, final Object2ReferenceMap<String,Index> name2Index, final Reference2DoubleMap<Index> index2Weight ) throws IOException, ConfigurationException, URISyntaxException, ClassNotFoundException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        for (int i = 0; i < basenameWeight.length; i++) {

            // We must be careful, as ":" is used by Windows to separate the device from the path.
            final int split = basenameWeight[i].lastIndexOf(':');
            double weight = 1;

            if (split != -1) {
                try {
                    weight = Double.parseDouble(basenameWeight[i].substring(split + 1));
                } catch (NumberFormatException e) {
                }
            }

            final Index index;

            if (split == -1 || basenameWeight[i].startsWith("mg4j://")) {
                index = Index.getInstance(basenameWeight[i], true, loadSizes);
                index2Weight.put(index, 1);
            } else {
                index = Index.getInstance(basenameWeight[i].substring(0, split));
                index2Weight.put(index, weight);
            }
            if (documentCollection != null && index.numberOfDocuments != documentCollection.size()) {
                System.out.println("Index " + index + " has " + index.numberOfDocuments + " documents, but the document collection has size " + documentCollection.size());
            }
            name2Index.put(index.field != null ? index.field : basenameWeight[i], index);
        }
    }
    
}
