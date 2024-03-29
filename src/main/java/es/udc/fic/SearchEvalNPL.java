package es.udc.fic;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

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

    private static HashSet<String> leerRelevancia(String pathString, int querie) {
        Path path = Path.of(pathString);
        String[] docs;
        String[] docIds=null;
        HashSet<String> result=new HashSet<>();
        try (InputStream stream = Files.newInputStream(path)) {
            String s = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            docs = s.split("\n   /\n");
            for(int i=0; i<docs.length;i++) {
                int part = docs[i].indexOf("\n");
                String content  = docs[i].substring(part+1);
                if (docs[i].substring(0, part).equals(querie+"")) {
                    content = content.replaceAll("\\s+"," ");
                    docIds = content.split(" ");
                    break;
                }
            }
            for(String aux : docIds) {
                if(!aux.equals("")) result.add(aux);
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void verRelevancia(HashSet<String> docIdsRelevancia, QueryFeatures queryFeatures) {
        int i;
        float cont = 0;
        DocFeatures[] docFeatures = queryFeatures.getDocFeatures();
        for (i=0;i<docFeatures.length;i++){
            for(String aux: docIdsRelevancia) {
                if (Integer.valueOf(aux) == docFeatures[i].getDocId() ) {
                    cont++;
                    queryFeatures.getDocFeatures()[i].setRelevante(true);
                }
                queryFeatures.getDocFeatures()[i].setNumRelevantes(cont);
            }
        }
        queryFeatures.setNumRelevantes(docIdsRelevancia.size());
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

    private static void escribirResultados(QueryFeatures[] queryFeatures, int top, IndexReader readerIndex) throws IOException {
        int i,j, limit, docId;
        float metricaTotal = 0,totalQueries;
        Iterator<IndexableField> itFields;
        IndexableField field;
        totalQueries=queryFeatures.length;
        for (i=0;i<queryFeatures.length;i++) {
            if (queryFeatures[i] == null) {
                totalQueries--;
                continue;
            }
            System.out.println("Query: " + queryFeatures[i].getNombreQuerie());

            if (queryFeatures[i].getDocFeatures().length<top) limit=queryFeatures[i].getDocFeatures().length;
            else limit = top;

            System.out.println("Top Documentos: " );
            for(j=0;j<limit;j++) {
                docId = queryFeatures[i].getDocFeatures()[j].getDocId();
                itFields = readerIndex.document(docId).getFields().iterator();
                while (itFields.hasNext()) {
                    field = itFields.next();
                    System.out.println(field.name() + ": " + field.stringValue());
                }

                System.out.println("Score: " + queryFeatures[i].getDocFeatures()[j].getScore());
                System.out.println("Es relevante? : " + queryFeatures[i].getDocFeatures()[j].isRelevante());

            }
            System.out.println("Metrica individual: " + queryFeatures[i].getValorMetrica());
            metricaTotal+= queryFeatures[i].getValorMetrica();
        }
        System.out.println("Metrica total: " + (metricaTotal/totalQueries));
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
        QueryFeatures [] queryFeatures = null;
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
            HashSet<String> docIdsRelevanciaQuery;
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
                    query = parser.parse(queryArray[i].toLowerCase());
                    int numDocsQuerie = searcher.count(query);
                    if (numDocsQuerie<cut) docFeatures = new DocFeatures[numDocsQuerie];
                    else docFeatures = new DocFeatures[cut];
                    if (numDocsQuerie == 0) {
                        queryFeatures[i] = null;
                        continue;
                    }
                    topDocs = searcher.search(query,cut);

                    for (j=0;j<cut&&j<numDocsQuerie;j++) {
                        docFeatures[j] = new DocFeatures(topDocs.scoreDocs[j].doc,topDocs.scoreDocs[j].score,false);
                    }
                    queryFeatures[i] = new QueryFeatures(queryArray[i],docFeatures);
                    docIdsRelevanciaQuery = leerRelevancia(relevancetext,i+1);
                    if (docIdsRelevanciaQuery == null) {
                        System.err.println("No existen Ids relevantes para esta query");
                        System.exit(1);
                    }
                    verRelevancia(docIdsRelevanciaQuery,queryFeatures[i]);


                }


            } else {
                int iniQuery;
                int finQuery;
                String[] partsQuery = queries.split("-");
                iniQuery = Integer.valueOf(partsQuery[0])-1;
                if (partsQuery.length>1) {
                    finQuery = Integer.valueOf(partsQuery[1])-1;
                    queryFeatures = new QueryFeatures[(finQuery-iniQuery)+1];
                    for(i=0;i<queryFeatures.length;i++) {
                        query = parser.parse(queryArray[i+iniQuery].toLowerCase());
                        int numDocsQuerie = searcher.count(query);
                        if (numDocsQuerie<cut) docFeatures = new DocFeatures[numDocsQuerie];
                        else docFeatures = new DocFeatures[cut];
                        if (numDocsQuerie == 0) {
                            queryFeatures[i] = null;
                            continue;
                        }
                        topDocs = searcher.search(query,cut);

                        for (j=0;j<cut&&j<numDocsQuerie;j++) {
                            docFeatures[j] = new DocFeatures(topDocs.scoreDocs[j].doc,topDocs.scoreDocs[j].score,false);
                        }
                        queryFeatures[i] = new QueryFeatures(queryArray[i+iniQuery],docFeatures);
                        docIdsRelevanciaQuery = leerRelevancia(relevancetext,i+iniQuery+1);
                        if (docIdsRelevanciaQuery == null) {
                            System.err.println("No existen Ids relevantes para esta query");
                            System.exit(1);
                        }
                        verRelevancia(docIdsRelevanciaQuery,queryFeatures[i]);

                    }

                } else {
                    queryFeatures = new QueryFeatures[1];
                    query = parser.parse(queryArray[iniQuery].toLowerCase());
                    int numDocsQuerie = searcher.count(query);
                    if (numDocsQuerie<cut) docFeatures = new DocFeatures[numDocsQuerie];
                    else docFeatures = new DocFeatures[cut];
                    if (numDocsQuerie == 0) {
                        queryFeatures[0] = null;
                        return null;
                    }
                    topDocs = searcher.search(query,cut);

                    for (j=0;j<cut&&j<numDocsQuerie;j++) {
                        docFeatures[j] = new DocFeatures(topDocs.scoreDocs[j].doc,topDocs.scoreDocs[j].score,false);
                    }
                    queryFeatures[0] = new QueryFeatures(queryArray[iniQuery],docFeatures);
                    docIdsRelevanciaQuery = leerRelevancia(relevancetext,iniQuery+1);
                    if (docIdsRelevanciaQuery == null) {
                        System.err.println("No existen Ids relevantes para esta query");
                        System.exit(1);
                    }
                    verRelevancia(docIdsRelevanciaQuery,queryFeatures[0]);

                }
            }

            CalcularMetrica(queryFeatures,metrica);

            return queryFeatures;

        } catch (IOException | ParseException | NumberFormatException e) {
            e.printStackTrace();
        }finally {
        	if (queryFeatures == null) {
                System.err.println("Ha habido un error con el valor de las queries");
                System.exit(-1);
            }
        }

        return null;

    }

    public static void main(String[] args) {
        String usage = "java es.udc.fic.SeatchEvalNPL" + " -indexin INDEX_PATH -cut n -metrica P|R|MAP" +
                " -top m -queries all|int1|int1-int2 -search jm lambda| dir mu| tfidf             \n\n";

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
                    smooth=0f;
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
        if (queryFeatures==null) {
            System.err.println("Ha habido un error con el valor de las queries");
            System.exit(-1);
        }
        Directory dir;
        try {
            IndexReader readerIndex;
            dir = FSDirectory.open(indexDir);
            readerIndex = DirectoryReader.open(dir);

            escribirResultados(queryFeatures, top, readerIndex);

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
