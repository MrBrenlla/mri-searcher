package es.udc.fic;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Properties;

class docFeaturesAntiguo {
    private int docId;
    private float score;
    private boolean relevante;

    public docFeaturesAntiguo(int docId, float score, boolean relevante) {
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

}

public class SearchEvalNPLAntiguo {

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

    private static Boolean esRelevante(int docId, String pathString, int querie) {
        Path path = Path.of(pathString);
        Boolean relevante = false;
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
                    if (docs[i].substring(0, part).equals(querie+"")){
                        for (int j =0; j<docIds.length;j++) {
                            if (!docIds[j].equals("") && Integer.valueOf(docIds[j]) == docId ) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return relevante;
    }

    private static int numRelevantesQuerie(String pathString, int querie, TopDocs topDocs) {
        Path path = Path.of(pathString);
        int relevante = 0;
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
                    if (docs[i].substring(0, part).equals(querie+"")){
                        for (int x = 0; x<topDocs.scoreDocs.length; x++) {
                            for(int y = 0; y<docIds.length;y++) {
                                if (!docIds[y].equals("") && docIds[y].equals(topDocs.scoreDocs[x].doc+ ""))
                                    relevante++;
                            }
                        }
                        return relevante;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return relevante;
    }

    private static void escribirResultado(docFeaturesAntiguo[][] docsFeatures, int cut, float[] allValues, int contNoRelevantes,
                                          String[] queriesArray, String querie) {
        int i,j,cont=0;
        float allMetricas = 0;
        for (i=0; i<queriesArray.length; i++) {
            if (allValues[i] == -1) continue;
            System.out.println("Querie: " + queriesArray[i]);
            System.out.println("Documentos: ");
            for (j=0; j<docsFeatures[i].length; j++) {
                System.out.println("DocId: " + docsFeatures[i][j].getDocId());
                System.out.println("Score: " + docsFeatures[i][j].getScore());
                System.out.println("Es relevante? : " + docsFeatures[i][j].isRelevante());
                cont++;
            }
            System.out.println("Valor de la metrica: " + allValues[i]);
            allMetricas = allMetricas + allValues[i];
            System.out.println(" ");
        }
        System.out.println("Valor promediado metrica: "+ allMetricas/cont);


    }

    private static float[] leerQuerie(String queries, String querytext, int cut, QueryParser parser, IndexSearcher searcher,
                                   String relevancetext, String metrica) throws ParseException, IOException {
        String[] queriesArray, querieNames;
        queriesArray = leerQueryText(querytext);
        int i,j,contNoRelevantes = 0;
        float metricaValue = 0;
        docFeaturesAntiguo[][] docsFeatures;
        TopDocs topDocs;
        Query query;
        float numRelevantes = 0;
        float [] allValues;

        if (queries.equals("all")) {
            docsFeatures = new docFeaturesAntiguo[queriesArray.length][cut];
            allValues = new float[queriesArray.length];
            querieNames = queriesArray;
            for (i = 0; i<queriesArray.length ; i++){
                query = parser.parse(queriesArray[i]);
                int numDocsQuerie = searcher.count(query);
                if (numDocsQuerie == 0) {
                    allValues[i] = -1;
                    continue;
                }
                topDocs = searcher.search(query,numDocsQuerie);
                float numRelevantesQuerie = (float) numRelevantesQuerie(relevancetext,i+1,topDocs);

                if (numRelevantesQuerie == 0) {
                    allValues[i] = -1;
                    contNoRelevantes++;
                    continue;
                }
                for (j = 0; j<cut && j<topDocs.scoreDocs.length; j++) {
                    Boolean relevante = esRelevante(topDocs.scoreDocs[j].doc,relevancetext, i+1);
                    docsFeatures[i][j] = new docFeaturesAntiguo(
                            topDocs.scoreDocs[j].doc,
                            topDocs.scoreDocs[j].score,
                            relevante
                    );
                    if (relevante) {
                        numRelevantes++;
                        if (metrica.equals("MAP")) {
                            metricaValue = metricaValue + (numRelevantes/(j+1));
                        }
                    }
                }
                if (metrica.equals("P")) {
                    metricaValue = numRelevantes/(float)cut;
                    System.out.println(
                            metricaValue
                    );
                } else if (metrica.equals("R")) {
                    if (numRelevantes == 0) metricaValue = 0;
                    else metricaValue = numRelevantesQuerie/numRelevantes;
                } else if (metrica.equals("MAP")){
                    if (topDocs.scoreDocs.length<cut) metricaValue=metricaValue/topDocs.scoreDocs.length;
                    else metricaValue = metricaValue/cut;
                }
                allValues[i] = metricaValue;
                metricaValue=0;
                numRelevantes = 0;
            }
        } else {
            int iniQuerie;
            int finQuerie;
            String[] partsQuerie = queries.split("-");
            iniQuerie = Integer.valueOf(partsQuerie[0])-1;
            if (partsQuerie.length>1) {

                finQuerie = Integer.valueOf(partsQuerie[1])-1;
                allValues = new float[finQuerie-iniQuerie];
                querieNames = new String[finQuerie-iniQuerie];
                docsFeatures = new docFeaturesAntiguo[finQuerie-iniQuerie][cut];
                for (i = iniQuerie; i <=finQuerie;i++) {
                    querieNames[i] = queriesArray[i];
                    query = parser.parse(queriesArray[i]);
                    int numDocsQuerie = searcher.count(query);
                    if (numDocsQuerie == 0) {
                        allValues[i] = -1;
                        continue;
                    }
                    topDocs = searcher.search(query,numDocsQuerie);
                    float numRelevantesQuerie = (float) numRelevantesQuerie(relevancetext,i+1,topDocs);

                    if (numRelevantesQuerie == 0) {
                        contNoRelevantes++;
                        allValues[i] = -1;
                        continue;
                    }
                    for (j = 0; j<cut && j<topDocs.scoreDocs.length; j++) {
                        Boolean relevante = esRelevante(topDocs.scoreDocs[j].doc,relevancetext, i+1);
                        docsFeatures[i][j] = new docFeaturesAntiguo(
                                topDocs.scoreDocs[j].doc,
                                topDocs.scoreDocs[j].score,
                                relevante
                        );
                        if (relevante) {
                            numRelevantes++;
                            if (metrica.equals("MAP")) {
                                metricaValue = metricaValue + (numRelevantes/(j+1));
                            }
                        }
                    }
                    if (metrica.equals("P")) {
                        metricaValue = numRelevantes/(float) cut;
                    } else if (metrica.equals("R")) {
                        if (numRelevantes == 0) metricaValue = 0;
                        else metricaValue = numRelevantesQuerie/numRelevantes;
                    } else if (metrica.equals("MAP")){
                        if (topDocs.scoreDocs.length<cut) metricaValue=metricaValue/topDocs.scoreDocs.length;
                        else metricaValue = metricaValue/cut;
                    }
                    allValues[i] = metricaValue;
                    metricaValue = 0;
                    numRelevantes = 0;
                }

            } else {
                allValues = new float[1];
                docsFeatures = new docFeaturesAntiguo[1][cut];
                querieNames = new String[1];
                querieNames[0] = queriesArray[iniQuerie];
                query = parser.parse(queriesArray[iniQuerie]);
                int numDocsQuerie = searcher.count(query);
                if (numDocsQuerie == 0) return null;
                topDocs = searcher.search(query,numDocsQuerie);
                float numRelevantesQuerie = (float) numRelevantesQuerie(relevancetext,iniQuerie+1,topDocs);

                if (numRelevantesQuerie == 0) {
                    contNoRelevantes++;
                }
                for (j = 0; j<topDocs.scoreDocs.length; j++) {
                    //System.out.println(i+1);
                    Boolean relevante = esRelevante(topDocs.scoreDocs[j].doc,relevancetext, iniQuerie+1);
                    docsFeatures[0][j] = new docFeaturesAntiguo(
                            topDocs.scoreDocs[j].doc,
                            topDocs.scoreDocs[j].score,
                            relevante
                    );
                    if (relevante) {
                        numRelevantes++;
                        if (metrica.equals("MAP")) {
                            metricaValue = metricaValue + (numRelevantes/(j+1));
                        }
                    }
                }
                if (metrica.equals("P")) {
                    metricaValue = numRelevantes/cut;
                } else if (metrica.equals("R")) {
                    if (numRelevantes == 0) metricaValue = 0;
                    else metricaValue = numRelevantesQuerie/numRelevantes;
                } else if (metrica.equals("MAP")){
                    if (topDocs.scoreDocs.length<cut) metricaValue=metricaValue/topDocs.scoreDocs.length;
                    else metricaValue = metricaValue/cut;
                }
                allValues[0] = metricaValue;
            }
        }
        escribirResultado(docsFeatures,cut,allValues,contNoRelevantes, querieNames, queries);
        return allValues;

    }

    public static float[] comenzarBusqueda(Path indexDir, String search, float smooth, String queries, String querytext,
                                        int cut,String relevancetext, String metrica) {
        Directory dir;
        try {
            IndexReader readerIndex;
            dir = FSDirectory.open(indexDir);
            readerIndex = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(readerIndex);
            Analyzer analyzer = new StandardAnalyzer();
            QueryParser parser = new QueryParser("contents", analyzer);


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

            return leerQuerie(queries,querytext,cut,parser,searcher,relevancetext, metrica);

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args){
        String usage = "java es.udc.fic.IndexNPL" + " [-indexin INDEX_PATH] [-cut n] [-metrica P|R|MAP]" +
                       " [-top m] [-queries all|int1|int1-int2] [-search jm lambda| dir mu| tfidf]              \n\n";

        Properties p = new Properties();
        try {
            p.load(Files.newInputStream(Path.of("./src/main/resources/config.properties")));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        String querytext = p.getProperty("query-text");
        String relevancetext = p.getProperty("relevance-text");
        String indexPath = null;
        int cut = -1;
        String metrica = null;
        String top = null;
        String queries = null;
        String search = null;
        float smooth = -1.f;
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
                 top = args[i + 1];
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

        if(indexPath == null || cut == -1 || metrica == null || top == null || queries == null || smooth == -1.f) {
            System.err.println("Usage: " + usage);
            System.exit(1);
        }

        final Path indexDir = Paths.get(indexPath);
        if (!Files.isReadable(indexDir)) {
            System.out.println("Document directory '" + indexDir.toAbsolutePath()
                    + "' does not exist or is not readable, please check the path");
            System.exit(1);
        }

        Date start = new Date();

        comenzarBusqueda(indexDir,search,smooth,queries,querytext,cut,relevancetext,metrica);
    }
}
