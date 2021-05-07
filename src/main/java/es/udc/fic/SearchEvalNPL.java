package es.udc.fic;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

class DocFeatures {
    private int docId;
    private float score;
    private boolean relevante;
    private float numRelevantes;

    public DocFeatures(int docId, float score, boolean relevante) {
        this.docId = docId;
        this.score = score;
        this.relevante = relevante;
    }

    public int getDocId() {
        return docId;
    }

    public void setDocId(int docId) {
        this.docId = docId;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public boolean isRelevante() {
        return relevante;
    }

    public void setRelevante(boolean relevante) {
        this.relevante = relevante;
    }

    public float getNumRelevantes() {
        return numRelevantes;
    }

    public void setNumRelevantes(float numRelevantes) {
        this.numRelevantes = numRelevantes;
    }
}

class QueryFeatures {
    private String nombreQuerie;
    private DocFeatures[] docFeatures;
    private float numRelevantes;
    private float valorMetrica;

    public QueryFeatures(String nombreQuerie, DocFeatures[] docFeatures) {
        this.nombreQuerie = nombreQuerie;
        this.docFeatures = docFeatures;
    }

    public String getNombreQuerie() {
        return nombreQuerie;
    }

    public void setNombreQuerie(String nombreQuerie) {
        this.nombreQuerie = nombreQuerie;
    }

    public DocFeatures[] getDocFeatures() {
        return docFeatures;
    }

    public void setDocFeatures(DocFeatures[] docFeatures) {
        this.docFeatures = docFeatures;
    }

    public float getNumRelevantes() {
        return numRelevantes;
    }

    public void setNumRelevantes(float numRelevantes) {
        this.numRelevantes = numRelevantes;
    }

    public float getValorMetrica() {
        return valorMetrica;
    }

    public void setValorMetrica(float valorMetrica) {
        this.valorMetrica = valorMetrica;
    }
}

public class SearchEvalNPL {

    private static String[] leerQueryText(String pathString) {
        Path path = Path.of(pathString);
        String[] docs = null;
        try (InputStream stream = Files.newInputStream(path)) {
            String s = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            docs = s.split("\n/\n");
            for(int i=0; i<docs.length;i++) {
                int part = docs[i].indexOf("\n");
                docs[i]  = docs[i].substring(part+1);
                //System.out.println("Query string " + docs[i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return docs;
    }

    private static String[] leerRelevancia(String pathString, int querie) {
        Path path = Path.of(pathString);
        String[] docs;
        String[] docIds;
        try (InputStream stream = Files.newInputStream(path)) {
            String s = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            docs = s.split("\n   /\n");
            for(int i=0; i<docs.length;i++) {
                int part = docs[i].indexOf("\n");
                String content  = docs[i].substring(part+1);
                if (docs[i].substring(0, part).equals(querie+"")) {
                    content = content.replaceAll("\\s+"," ");
                    docIds = content.split(" ");
                    return docIds;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void verRelevancia(String[] docIdsRelevancia, QueryFeatures queryFeatures, ScoreDoc[] scoreDocs) {
        int i,j;
        float cont = 0;
        DocFeatures[] docFeatures = queryFeatures.getDocFeatures();
        for (i=0;i<scoreDocs.length;i++){
            for (j=0; j<docIdsRelevancia.length;j++) {
                if (!docIdsRelevancia[j].equals("") && Integer.valueOf(docIdsRelevancia[j]) == scoreDocs[i].doc ) {
                    cont++;
                    if( i<docFeatures.length && scoreDocs[i].doc == docFeatures[i].getDocId()) {
                            queryFeatures.getDocFeatures()[i].setRelevante(true);
                    }

                }
                if (i<docFeatures.length) {
                    queryFeatures.getDocFeatures()[i].setNumRelevantes(cont);
                    //System.out.println(" en la iteracion: "+ i + "contador: " + queryFeatures.getDocFeatures()[i].getNumRelevantes());
                }
            }
        }
        queryFeatures.setNumRelevantes(cont);
    }

    private static void CalcularMetrica(QueryFeatures[] queryFeatures, String metrica) {
        int i,j;
        float precision=0,cont = 0;
        DocFeatures[] docFeatures;
        for(i = 0;i<queryFeatures.length;i++) {
            if (queryFeatures[i] == null) continue;
            docFeatures = queryFeatures[i].getDocFeatures();

            if (metrica.equals("P")) {
                queryFeatures[i].setValorMetrica(docFeatures[docFeatures.length-1].getNumRelevantes()/(float)docFeatures.length);
            } else if (metrica.equals("R")) {
                if (queryFeatures[i].getNumRelevantes() == 0) queryFeatures[i].setValorMetrica(0);
                else queryFeatures[i].setValorMetrica(docFeatures[docFeatures.length-1].getNumRelevantes()/queryFeatures[i].getNumRelevantes());
            } else if (metrica.equals("MAP")) {
                for (j = 0;j<docFeatures.length;j++) {
                    if (docFeatures[j].isRelevante()) {
                        precision = precision + docFeatures[j].getNumRelevantes()/(float)(j+1);
                        cont++;
                    }

                }
                queryFeatures[i].setValorMetrica(precision/cont);
            }
        }
    }

    private static void escribirResultados(QueryFeatures[] queryFeatures, int top) {
        int i,j;
        for (i=0;i<queryFeatures.length;i++) {
            if (queryFeatures[i] == null) continue;
            System.out.println("Query: " + queryFeatures[i].getNombreQuerie());
            System.out.println("Top Documentos: " );
            for(j=0;j<queryFeatures[i].getDocFeatures().length;j++) {
                //TODO obtener los indices para printear los campos del documento
                System.out.println("DocId: " + queryFeatures[i].getDocFeatures()[j].getDocId());
                System.out.println("Score: " + queryFeatures[i].getDocFeatures()[j].getScore());
                System.out.println("Es relevante? : " + queryFeatures[i].getDocFeatures()[j].isRelevante());

            }
            System.out.println("Metrica individual: " + queryFeatures[i].getValorMetrica());
        }
        //TODO realizar la implementacion de la metrica total
        //System.out.println("Metrica total: " + queryFeatures[i].getValorMetrica());
    }

    public static QueryFeatures [] comenzarBusqueda(Path indexDir, String metrica, int cut, String queries, String search, float smooth) {
        Properties p = new Properties();
        try {
            p.load(Files.newInputStream(Path.of("./src/main/resources/config.properties")));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        String querytext = p.getProperty("query-text");
        String relevancetext = p.getProperty("relevance-text");

        Directory dir;
        try {
            IndexReader readerIndex;
            dir = FSDirectory.open(indexDir);
            readerIndex = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(readerIndex);
            Analyzer analyzer = new StandardAnalyzer();
            QueryParser parser = new QueryParser("contents", analyzer);
            Query query;
            TopDocs topDocs;

            String [] queryArray;
            String [] docIdsRelevanciaQuery;
            QueryFeatures [] queryFeatures = null;
            DocFeatures[] docFeatures;
            int i,j;



            if (search==null){
                System.out.println("search not specified: Correct formats are \"jm lambda\", \"dir mu\" and \"tfidf\". Running tfidf as default" );
                searcher.setSimilarity(new ClassicSimilarity());
            } else if (search.equals("jm")) {
                searcher.setSimilarity(new LMJelinekMercerSimilarity(smooth));
            } else if (search.equals("dir")) {
                searcher.setSimilarity(new LMDirichletSimilarity(smooth));
            } else if (search.equals("tfidf")) {
                searcher.setSimilarity(new ClassicSimilarity());
            } else {
                System.out.println("search error: Correct formats are \"jm lambda\", \"dir mu\" and \"tfidf\". Running tfidf as default" );
                searcher.setSimilarity(new ClassicSimilarity());
            }

            queryArray=leerQueryText(querytext);


            if (queries.equals("all")) {
                queryFeatures = new QueryFeatures[queryArray.length];
                for(i=0;i<queryArray.length;i++) {
                    query = parser.parse(queryArray[i]);
                    int numDocsQuerie = searcher.count(query);
                    if (numDocsQuerie<cut) docFeatures = new DocFeatures[numDocsQuerie];
                    else docFeatures = new DocFeatures[cut];
                    if (numDocsQuerie == 0) {
                        queryFeatures[i] = null;
                        continue;
                    }
                    topDocs = searcher.search(query,numDocsQuerie);

                    for (j=0;j<cut&&j<numDocsQuerie;j++) {
                        docFeatures[j] = new DocFeatures(topDocs.scoreDocs[j].doc,topDocs.scoreDocs[j].score,false);
                    }
                    queryFeatures[i] = new QueryFeatures(queryArray[i],docFeatures);
                    docIdsRelevanciaQuery = leerRelevancia(relevancetext,i+1);
                    if (docIdsRelevanciaQuery == null) {
                        System.err.println("No existen Ids relevantes para esta query");
                        System.exit(1);
                    }
                    verRelevancia(docIdsRelevanciaQuery,queryFeatures[i],topDocs.scoreDocs);


                }


            }


            CalcularMetrica(queryFeatures,metrica);

            return queryFeatures;

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return null;

    }

    public static void main(String[] args) {
        String usage = "java es.udc.fic.IndexNPL" + " [-indexin INDEX_PATH] [-cut n] [-metrica P|R|MAP]" +
                " [-top m] [-queries all|int1|int1-int2] [-search jm lambda| dir mu| tfidf]              \n\n";

        String indexPath = null;
        int cut = -1;
        String metrica = null;
        int top = -1;
        String queries = null;
        String search = null;
        float smooth = -1.f;
        QueryFeatures[] queryFeatures;
        for (int i = 0; i < args.length; i++) {
            if ("-indexin".equals(args[i])) {
                indexPath = args[i + 1];
                i++;
            } else if ("-cut".equals(args[i])) {
                cut = Integer.valueOf(args[i + 1]);
                i++;
            } else if ("-metrica".equals(args[i])) {
                metrica = args[i + 1];
                i++;
            } else if ("-top".equals(args[i])) {
                top = Integer.valueOf(args[i + 1]);
                i++;
            } else if ("-queries".equals(args[i])) {
                queries = args[i + 1];
                i++;
            } else if ("-search".equals(args[i])) {
                if ("jm".equals(args[i+1]) || "dir".equals(args[i+1])) {
                    search = args[i+1];
                    smooth = Float.valueOf(args[i+2]);
                    i = i+2;
                } else{
                    search = args[i + 1];
                    i++;
                }
            }
        }

        if(indexPath == null || cut == -1 || metrica == null || top == -1 || queries == null || smooth == -1.f) {
            System.err.println("Usage: " + usage);
            System.exit(1);
        }

        final Path indexDir = Paths.get(indexPath);
        if (!Files.isReadable(indexDir)) {
            System.out.println("Document directory '" + indexDir.toAbsolutePath()
                    + "' does not exist or is not readable, please check the path");
            System.exit(1);
        }


        queryFeatures = comenzarBusqueda(indexDir, metrica, cut, queries, search, smooth);

        escribirResultados(queryFeatures, top);

    }

}
