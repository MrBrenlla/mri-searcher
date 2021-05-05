package es.udc.fic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class TrainingTestNPL {

	private TrainingTestNPL() {
	}

	/** Index all text files under a directory. */
	public static void main(String[] args) {
String usage = "java es.udc.fic.TrainingTestNPL" + " -evaljm INT1-INT2 INT3-INT4 -evaldir INT1-INT2 INT3-INT4"
		+ "-cut N -metrica P | R | MAP -indexin PATHNAME -outfile RESULTS\n\n";
		
		Properties p = new Properties();
		try {
			p.load(Files.newInputStream(Path.of("./src/main/resources/config.properties")));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String indexPath = "index";
		String metr=null;
		String out=null;
		int n = -1;
		Boolean jm=false;
		Boolean dir=false;
		int[] trainjm= new int[2];
		int[] testjm= new int[2];
		int[] traindir= new int[2];
		int[] testdir= new int[2];
		String[] aux;
		
		
		for (int i = 0; i < args.length; i++) {
			if ("-indexin".equals(args[i])) {
				indexPath = args[i + 1];
				i++;
			} else if ("-evaljm".equals(args[i])) {
				jm=true;
				aux = args[i+1].split("-");
				i++;
				trainjm[0]=Integer.valueOf(aux[0]);
				trainjm[1]=Integer.valueOf(aux[1]);
				aux = args[i+1].split("-");
				i++;
				testjm[0]=Integer.valueOf(aux[0]);
				testjm[1]=Integer.valueOf(aux[1]);
			}else if ("-evaldir".equals(args[i])) {
				dir=true;
				aux = args[i+1].split("-");
				i++;
				traindir[0]=Integer.valueOf(aux[0]);
				traindir[1]=Integer.valueOf(aux[1]);
				aux = args[i+1].split("-");
				i++;
				testdir[0]=Integer.valueOf(aux[0]);
				testdir[1]=Integer.valueOf(aux[1]);
			}else if ("-cut".equals(args[i])) {
				n = Integer.valueOf(args[i + 1]);
				i++;
			}else if ("-metrica".equals(args[i])) {
				metr = args[i + 1].toUpperCase();
				i++;
			}else if ("-outfile".equals(args[i])) {
				out = args[i + 1];
				i++;
			}
		} 

		if(indexPath.equals("index")) {
			System.err.println("Usage: " + usage);
			System.exit(1);
		}
		if (metr==null || out==null || (!jm && !dir) || traindir==null || testdir==null) {
			System.out.print("Missing parameters: "+usage);
			System.exit(1);
		}
		
		if(n<1) {
			System.out.println("Error: -cut must be greater then 0");
			System.exit(1);
		}
		
		if((trainjm[0]>trainjm[1] || trainjm[0]<1 || testjm[0]>testjm[1] || testjm[0]<1) && jm) {
			System.out.println("Error: wrong intervals for -evaljm");
			System.exit(1);
		}
		
		if((traindir[0]>traindir[1] || traindir[0]<1 || testdir[0]>testdir[1] || testdir[0]<1) && dir) {
			System.out.println("Error: wrong intervals for -evaldir");
			System.exit(1);
		}
		
		if(!(metr.equals("P") || metr.equals("R") || metr.equals("MAP"))) {
			System.out.println("Error: -metrica can only by P, R or MAP");
			System.exit(1);
		}
		
	}
	
}
