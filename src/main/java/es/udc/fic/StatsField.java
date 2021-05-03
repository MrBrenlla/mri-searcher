package es.udc.fic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import org.apache.lucene.index.*;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class StatsField {


    public static void main(final String[] args) {

        String usage = "java es.udc.fic.StatsField" + " [-index INDEX_PATH] [-field FIELD]\n\n";

        Properties p = new Properties();
        try {
            p.load(Files.newInputStream(Path.of("./src/main/resources/config.properties")));
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        String indexPath = null;
        String field = null;

        for (int i = 0; i < args.length; i++) {
            if ("-index".equals(args[i])) {
                indexPath = args[i + 1];
                i++;
            } else if ("-field".equals(args[i])) {
                field = args[i + 1];
                i++;
            }
        }
        if(indexPath==null) {
            System.err.println("Usage: " + usage);
            System.exit(1);
        }

        final Path indexDir = Paths.get(indexPath);
        if (!Files.isReadable(indexDir)) {
            System.out.println("Index in '" + indexDir.toAbsolutePath()
                    + "' does not exist or is not readable, please check the path");
            System.exit(1);
        }
        Directory dir;
        try {
            CollectionStatistics collectionStatistics;
            dir = FSDirectory.open(Paths.get(indexPath));
            DirectoryReader reader= DirectoryReader.open(dir);
            if (field != null) {
                try {
                    collectionStatistics = new CollectionStatistics(
                            field,
                            reader.maxDoc(),
                            reader.getDocCount(field),
                            reader.getSumTotalTermFreq(field),
                            reader.getSumDocFreq(field)
                    );
                    System.out.println("Stadictics of field " + field + ":");
                    System.out.println(collectionStatistics.toString());
                } catch (Exception e) {
                    System.err.println("An error ocurred with field: " + field + " ("+ e +")");
                }

            } else {
                List<IndexableField> fields = reader.document(0).getFields();
                fields.get(0).toString();
                try {
                    for(IndexableField fieldItem : fields) {
                        field = fieldItem.name();
                        collectionStatistics = new CollectionStatistics(
                                field,
                                reader.maxDoc(),
                                reader.getDocCount(field),
                                reader.getSumTotalTermFreq(field),
                                reader.getSumDocFreq(field)
                        );
                        System.out.println("Stadictics of field " + field + ":");
                        System.out.println(collectionStatistics.toString());
                    }
                } catch (Exception e) {
                    System.err.println("An error ocurred with field: " + field + " ("+ e +")");
                }


            }
        reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
