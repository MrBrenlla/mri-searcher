package es.udc.fic;


import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Properties;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * Index all text files under a directory.
 * <p>
 * This is a command-line application demonstrating simple Lucene indexing. Run
 * it with no command-line arguments for usage information.
 */
public class IndexNPL {

	private IndexNPL() {
	}

	/** Index all text files under a directory. */
	public static void main(String[] args) {
String usage = "java es.udc.fic.IndexNPL" + " [-index INDEX_PATH] [-openmode append|create|create_or_append]\n\n";
		
		Properties p = new Properties();
		try {
			p.load(Files.newInputStream(Path.of("./src/main/resources/config.properties")));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String indexPath = "index";
		String docsPath = p.getProperty("docs");
		String indexingmodel = p.getProperty("indexingmodel");
		String openmode = null;
		
		for (int i = 0; i < args.length; i++) {
			if ("-index".equals(args[i])) {
				indexPath = args[i + 1];
				i++;
			} else if ("-openmode".equals(args[i])) {
				openmode = args[i + 1];
				i++;
			}
		} 

		if(indexPath.equals("index")) {
			System.err.println("Usage: " + usage);
			System.exit(1);
		}
			
		
		final Path docDir = Paths.get(docsPath);
		if (!Files.isReadable(docDir)) {
			System.out.println("Document directory '" + docDir.toAbsolutePath()
					+ "' does not exist or is not readable, please check the path");
			System.exit(1);
		}

		Date start = new Date();
		try {
			System.out.println("Indexing to directory '" + indexPath + "'...");

			Directory dir = FSDirectory.open(Paths.get(indexPath));
			Analyzer analyzer = new StandardAnalyzer();
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
			if (openmode==null){
				System.out.println("openMode not specified: Correct formats are append, create or create_or_append. Running create_or_append as default" );
				iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
			} else if (openmode.equals("create_or_append")) {
				iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
			} else if (openmode.equals("create")) {
				iwc.setOpenMode(OpenMode.CREATE);
			} else if ( openmode.equals("append")) {
				iwc.setOpenMode(OpenMode.APPEND);
			} else {
				System.out.println("openMode error: Correct formats are append, create or create_or_append. Running create_or_append as default" );
				iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
			}
			
			if (indexingmodel==null){
				System.out.println("indexingmodel not specified: Correct formats are jm LAMBDA, dir MU and tfidf. Running tfidf as default" );
				iwc.setSimilarity(new ClassicSimilarity());
			} else {
				String[] aux = indexingmodel.split(" ");
				if (aux[0].equals("jm")) {
					if(aux.length==1) {
						System.out.println("Missing lambda value");
						System.exit(1);
					}
					iwc.setSimilarity(new LMJelinekMercerSimilarity(Float.valueOf(aux[1])));
				} else if (aux[0].equals("dir")) {
					if(aux.length==1) {
						System.out.println("Missing mu value");
						System.exit(1);
					}
					iwc.setSimilarity(new LMDirichletSimilarity(Float.valueOf(aux[1])));
				} else if ( aux[0].equals("tfidf")) {
					iwc.setSimilarity(new ClassicSimilarity());
				} else {
					System.out.println("indexingmodel error: Correct formats are jm LAMBDA, dir MU and tfidf. Running tfidf as default" );
					iwc.setSimilarity(new ClassicSimilarity());
				}
			}


			IndexWriter writer = new IndexWriter(dir, iwc);
			indexDocs(writer, docDir);


			writer.close();

			Date end = new Date();
			System.out.println(end.getTime() - start.getTime() + " total milliseconds");

		} catch (IOException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		}
	}

	static void indexDocs(final IndexWriter writer, Path path) throws IOException {
		try (InputStream stream = Files.newInputStream(path)) {
			String s = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
			String[] docs = s.split("\n   /\n");
			for(int i=0; i<docs.length;i++) {
				int part = docs[i].indexOf("\n");
				String docIDNPL  = docs[i].substring(0, part);
				String content  = docs[i].substring(part+1);
				indexDoc(writer,docIDNPL,content);
			}
		}
	}

	/** Indexes a single document */
	static void indexDoc(IndexWriter writer, String id, String content) throws IOException {
			Document doc = new Document();

			FieldType type = new FieldType();
			type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
			type.setTokenized(true);
			type.setStored(true);
			type.setStoreTermVectors(true);
			
			Field pathField = new Field("DocIDNPL", id, type);
			doc.add(pathField);

			doc.add(new Field("contents",content,type));

			if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
				// New index, so we just add the document (no old document can be there):
				System.out.println("adding " + id);
				writer.addDocument(doc);
			} else {
				System.out.println("updating " + id);
				writer.updateDocument(new Term("DocIDNPL",id), doc);
			}
	}
}
