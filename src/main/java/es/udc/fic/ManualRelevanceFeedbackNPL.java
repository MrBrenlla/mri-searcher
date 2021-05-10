package es.udc.fic;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Scanner;
import java.util.HashSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
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

    private static void verRelevancia(String[] docIdsRelevancia, QueryFeatures queryFeatures) {
        int i,j;
        float cont = 0;
        DocFeatures[] docFeatures = queryFeatures.getDocFeatures();
        for (i=0;i<docFeatures.length;i++){
            for (j=0; j<docIdsRelevancia.length;j++) {
                if (!docIdsRelevancia[j].equals("") && Integer.valueOf(docIdsRelevancia[j]) == docFeatures[i].getDocId() ) {
                    cont++;
                    queryFeatures.getDocFeatures()[i].setRelevante(true);
                }
                queryFeatures.getDocFeatures()[i].setNumRelevantes(cont);
            }
        }
        queryFeatures.setNumRelevantes(docIdsRelevancia.length);
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

    private static void escribirResultados(QueryFeatures queryFeatures,IndexSearcher searcher, HashSet<Integer> founded,boolean res) throws IOException {
            if (queryFeatures == null) return;
            DocFeatures[] docs=queryFeatures.getDocFeatures();
            System.out.println("Query: " + queryFeatures.getNombreQuerie()
            	+"\nMetrica: " + queryFeatures.getValorMetrica()
            	+"\n\nBusqueda de relevantes para esta query:");
            for(int i=0; i<docs.length; i++) { 
            	if (docs[i].isRelevante())
	            if(founded.contains(docs[i].getDocId()) && res){
	            	System.out.println("\nSe encontró el documento con ID en Indice="+docs[i].getDocId()+" en el rank "+(i+1)+", pero se descarta porque ya fue utilizado");
	            }else {
	            	System.out.println("\nRank: "+(i+1)
	            			+"\nID en indice: "+docs[i].getDocId()
	            			+"\nScore: "+docs[i].getScore()
	            			+"\nDocIDNPL:"+searcher.doc(docs[i].getDocId()).getField("DocIDNPL").stringValue()
	            			+"\nContent:\n"+searcher.doc(docs[i].getDocId()).getField("contents").stringValue()+"\n");
	            	founded.add(docs[i].getDocId());
	            	return;
	            }
            }
            System.out.println("\nNo se devolvieron nuevos documentos relevantes para esta query\n");

    }

    public static QueryFeatures buscar( String metrica, int cut,String [] docIdsRelevanciaQuery ,Query query,IndexSearcher searcher,HashSet<Integer> founded,boolean res) {
       

        QueryFeatures queryFeatures = null;
        try {
            TopDocs topDocs;
            DocFeatures[] aux;
            DocFeatures[] docFeatures;
            int saltados=0;
            
            int numDocsQuery = searcher.count(query);
            
            /*if (numDocsQuerie<cut) aux = new DocFeatures[numDocsQuerie];
            else */
            aux = new DocFeatures[cut];
            if (numDocsQuery == 0) {
            	System.err.println("No existen documentos para esta query\n");
           	 	return null;
            }
            topDocs = searcher.search(query,numDocsQuery);
            for (int j=0;j<(cut+saltados)&&j<numDocsQuery;j++) 
            	if(founded.contains(topDocs.scoreDocs[j].doc)) {
            		System.out.println("saltouse "+topDocs.scoreDocs[j].doc);
            		saltados++;
            	}
            	else aux[j-saltados] = new DocFeatures(topDocs.scoreDocs[j].doc,topDocs.scoreDocs[j].score,false);
            
            if(aux[aux.length-1]==null) {
            	docFeatures = new DocFeatures[numDocsQuery-saltados];
            	for(int i=0; i<docFeatures.length;i++) {
            		docFeatures[i]=aux[i];
            	}
            }
            else docFeatures=aux;
            queryFeatures = new QueryFeatures(query.toString("contents"),docFeatures);
                 
                 if (docIdsRelevanciaQuery == null) {
                	 System.err.println("No existen Ids relevantes para esta query\n");
                	 return null;
                 }
                 verRelevancia(docIdsRelevanciaQuery,queryFeatures);
            
            CalcularMetrica(queryFeatures,metrica);

        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            System.err.println("Ha habido un error con la queries\n");
            return null;
        }

        return queryFeatures;

    }

    public static void main(String[] args) {
        String usage = "java es.udc.fic.IndexNPL" + " -indexin INDEX_PATH -cut n -metrica P|R|MAP" +
                "-queries Q -retmodel jm lambda| dir mu| tfidf [-residual T|F]           \n\n"; 

        String indexPath = null;
        int cut = -1;
        String metrica = null;
        int querynum = -1;
        String search = null;
        float smooth = -1.f;
        QueryFeatures queryFeatures;
        boolean res=false;
        
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
            }else if ("-residual".equals(args[i])) {
                if ("T".equals(args[i+1].toUpperCase())) res=true;
                i++;
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
        HashSet<Integer> founded = new HashSet<>();
        
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
        
        queryFeatures = buscar(metrica, cut,docIdsRelevanciaQuery, parser.parse(leerQueryText(querytext,querynum).toLowerCase()),searcher,founded,res);
        if(queryFeatures!=null) escribirResultados(queryFeatures, searcher,founded,res);
        
        System.out.print("Desea reformular la query?(S/N) ");
        if (!in.nextLine().toLowerCase().equals("s")) aux=false;
        while(aux) {
        	System.out.print("Nueva query: ");
        	queryFeatures = buscar(metrica, cut,docIdsRelevanciaQuery,parser.parse(in.nextLine().toLowerCase()),searcher,founded,res);
            if(queryFeatures!=null) escribirResultados(queryFeatures, searcher,founded,res);
            System.out.print("Desea reformular la query?(S/N) ");
            if (!in.nextLine().toLowerCase().equals("s")) aux=false;
        }
        System.out.print("ending program ");
        in.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (ParseException e) {
			e.printStackTrace();
		}

    }
}
