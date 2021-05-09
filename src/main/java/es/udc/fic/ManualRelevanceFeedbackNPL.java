package es.udc.fic;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Scanner;

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

public class ManualRelevanceFeedbackNPL {
	
	private static String leerQueryText(String pathString,int n) {
        Path path = Path.of(pathString);
        String[] docs = null;
        String query=null;
        try (InputStream stream = Files.newInputStream(path)) {
            String s = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            docs = s.split("\n/\n");
            for(int i=0; i<docs.length;i++) if(i==n-1){
                int part = docs[i].indexOf("\n");
                query  = docs[i].substring(part+1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return query;
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
                }
            }
        }
        queryFeatures.setNumRelevantes(cont);
    }

    private static void CalcularMetrica(QueryFeatures queryFeatures, String metrica) {
        float precision=0,cont = 0;
        DocFeatures[] docFeatures;

            if (queryFeatures == null) return;
            docFeatures = queryFeatures.getDocFeatures();

            if (metrica.equals("P")) {
                queryFeatures.setValorMetrica(docFeatures[docFeatures.length-1].getNumRelevantes()/(float)docFeatures.length);
            } else if (metrica.equals("R")) {
                if (queryFeatures.getNumRelevantes() == 0) queryFeatures.setValorMetrica(0);
                else queryFeatures.setValorMetrica(docFeatures[docFeatures.length-1].getNumRelevantes()/queryFeatures.getNumRelevantes());
            } else if (metrica.equals("MAP")) {
                for (int j = 0;j<docFeatures.length;j++) {
                    if (docFeatures[j].isRelevante()) {
                        precision = precision + docFeatures[j].getNumRelevantes()/(float)(j+1);
                        cont++;
                    }
                }
                queryFeatures.setValorMetrica(precision/cont);
            }
        
    }

    private static void escribirResultados(QueryFeatures queryFeatures,IndexSearcher searcher) throws IOException {
            if (queryFeatures == null) return;
            DocFeatures[] docs=queryFeatures.getDocFeatures();
            System.out.println("Query: " + queryFeatures.getNombreQuerie()
            	+"\nMetrica: " + queryFeatures.getValorMetrica());
            for(int i=0; i<docs.length; i++) 
            if (docs[i].isRelevante()) {
            	System.out.println("\n1º Relevante encontrado para esta query:\n\nRank: "+(i+1)
            			+"\nID en indice: "+docs[i].getDocId()
            			+"\nScore: "+docs[i].getScore()
            			+"\nDocIDNPL:"+searcher.doc(docs[i].getDocId()).getField("DocIDNPL").stringValue()
            			+"\nContent:\n"+searcher.doc(docs[i].getDocId()).getField("contents").stringValue()+"\n");
            	return;
            }
            System.out.println("\nNo se devolvieron documentos relevantes para esta query\n");

    }

    public static QueryFeatures busquedaInicial( String metrica, int cut,String [] docIdsRelevanciaQuery ,Query query,IndexSearcher searcher) {
       

        QueryFeatures queryFeatures = null;
        try {
            TopDocs topDocs;
            DocFeatures[] docFeatures;

            

           

            int numDocsQuerie = searcher.count(query);
            if (numDocsQuerie<cut) docFeatures = new DocFeatures[numDocsQuerie];
            else docFeatures = new DocFeatures[cut];
            if (numDocsQuerie == 0) {
            	System.err.println("No existen documentos para esta query");
           	 	System.exit(1);
            }
            topDocs = searcher.search(query,numDocsQuerie);
            for (int j=0;j<cut&&j<numDocsQuerie;j++) docFeatures[j] = new DocFeatures(topDocs.scoreDocs[j].doc,topDocs.scoreDocs[j].score,false);
            queryFeatures = new QueryFeatures(query.toString("contents"),docFeatures);
                 
                 if (docIdsRelevanciaQuery == null) {
                	 System.err.println("No existen Ids relevantes para esta query");
                	 System.exit(1);
                 }
                 verRelevancia(docIdsRelevanciaQuery,queryFeatures,topDocs.scoreDocs);
            
            CalcularMetrica(queryFeatures,metrica);
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }finally {
        	if (queryFeatures == null) {
                System.err.println("Ha habido un error con el valor de las queries");
                System.exit(-1);
                
            }
        }

        return queryFeatures;

    }

    public static void main(String[] args) {
        String usage = "java es.udc.fic.IndexNPL" + " -indexin INDEX_PATH -cut n -metrica P|R|MAP" +
                "-queries Q -retmodel jm lambda| dir mu| tfidf            \n\n"; 

        String indexPath = null;
        int cut = -1;
        String metrica = null;
        int querynum = -1;
        String search = null;
        float smooth = -1.f;
        QueryFeatures queryFeatures;
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
            } else if ("-queries".equals(args[i])) {
                querynum =Integer.valueOf(args[i + 1]);
                i++;
            } else if ("-retmodel".equals(args[i])) {
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

        if(indexPath == null || cut == -1 || metrica == null || querynum == -1 || smooth == -1.f) {
            System.err.println("Usage: " + usage);
            System.exit(1);
        }

        final Path indexDir = Paths.get(indexPath);
        if (!Files.isReadable(indexDir)) {
            System.out.println("Document directory '" + indexDir.toAbsolutePath()
                    + "' does not exist or is not readable, please check the path");
            System.exit(1);
        }
        
        Properties p = new Properties();
        try {
            p.load(Files.newInputStream(Path.of("./src/main/resources/config.properties")));
        
        
        String querytext = p.getProperty("query-text");
        String relevancetext = p.getProperty("relevance-text");
        Analyzer analyzer = new StandardAnalyzer();
        Directory dir = FSDirectory.open(indexDir);
        IndexReader readerIndex = DirectoryReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(readerIndex);
        Boolean aux=true;
        Scanner in = new Scanner(System.in);
        
        QueryParser parser = new QueryParser("contents", analyzer);
        String [] docIdsRelevanciaQuery = leerRelevancia(relevancetext,querynum);
        
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
        
        queryFeatures = busquedaInicial(metrica, cut,docIdsRelevanciaQuery, parser.parse(leerQueryText(querytext,querynum)),searcher);
        escribirResultados(queryFeatures, searcher);
        
        System.out.print("Desea reformular la query?(S/N) ");
        if (!in.nextLine().toLowerCase().equals("s")) aux=false;
        while(aux) {
        	System.out.print("Nueva query: ");
        	queryFeatures = busquedaInicial(metrica, cut,docIdsRelevanciaQuery,parser.parse(in.nextLine()),searcher);
            escribirResultados(queryFeatures, searcher);
            System.out.print("Desea reformular la query?(S/N) ");
            if (!in.nextLine().toLowerCase().equals("s")) aux=false;
        }
        System.out.print("ending program "+aux);
        
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (ParseException e) {
			e.printStackTrace();
		}

    }
}
