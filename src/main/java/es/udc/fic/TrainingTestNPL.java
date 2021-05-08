package es.udc.fic;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;

public class TrainingTestNPL {

	private TrainingTestNPL() {
	}
	
	private static int max(float[][] a) {
		int max=0;
		for(int i=0; i<11;i++) {
			if (a[11][i]>a[11][max]) max=i;
		}
		return max;
	}
	
	private static float[] transform(QueryFeatures[] a) {
		float[] x = new float[a.length];
		for (int i = 0; i<a.length;i++) x[i]=a[i].getValorMetrica();
		return x;
	}
	
	private static void resulatdos(float[][] train, float[] test, float valor,float best,int trainini, int testini, String type,PrintWriter pw) {
		System.out.println("Training:");
		if(type.equals("λ")) System.out.printf("%10s %10.4f %10.1f %10.1f %10.1f %10.1f %10.1f %10.1f %10.1f %10.1f %10.1f %10.1f %n",type,valor*0.001f,valor,valor*2,valor*3,valor*4,valor*5,valor*6,valor*7,valor*8,valor*9,valor*10);
		else System.out.printf("%10s %10.1f %10.1f %10.1f %10.1f %10.1f %10.1f %10.1f %10.1f %10.1f %10.1f %10.1f %n",type,0f,valor,valor*2,valor*3,valor*4,valor*5,valor*6,valor*7,valor*8,valor*9,valor*10);
		for (int j=0; j<train[0].length;j++) {
			System.out.printf("%10d", trainini+j);
			for (int i=0; i<11;i++) {
				System.out.printf(" %10f", train[i][j]);
			}
			System.out.println("");
		}
		System.out.printf("%10s", "promedio");
		for (int i=0; i<train.length-1;i++) {
			System.out.printf(" %10f", train[11][i]);
		}
		System.out.println("");
		
		System.out.println("Test ( "+type+" = "+best+" ):");
		for (int i=0; i<test.length;i++) {
			System.out.printf("%10d %10f%n", testini+i, test[i]);
			pw.printf("<%d;%f>%n", testini+i, test[i]);
		}
		float aux = 0f;
		System.out.printf("%10s", "promedio");
		for (int i=0; i<test.length;i++) if(test[i]>0) aux+=test[i];
		System.out.printf(" %10f%n", aux/test.length);
	}
	
	private static float[][] training(float min,int[] q, boolean jm, String metr,String index,int cut) {
		float[][] results = new float[12][];
		if (jm) {
			results[0]=transform(SearchEvalNPL.comenzarBusqueda(Path.of(index),metr,cut,q[0]+"-"+q[1],"jm",min*0.001f));
		}else {
			results[0]=transform(SearchEvalNPL.comenzarBusqueda(Path.of(index),metr,cut,q[0]+"-"+q[1],"dir",0));
		}
		for(int i=1;i<11;i++) {
			if (jm) {
				results[i]=transform(SearchEvalNPL.comenzarBusqueda(Path.of(index),metr,cut,q[0]+"-"+q[1],"jm",min*i));
			}else {
				results[i]=transform(SearchEvalNPL.comenzarBusqueda(Path.of(index),metr,cut,q[0]+"-"+q[1],"dir",min*i));
			}
		}
		results[11]=new float[11];
		for(int i=0; i<11;i++) {
			for (int j=0; j < results[i].length-1; j++) if(results[i][j]>0) results[11][i] += results[i][j];
			results[11][i]=results[11][i]/results[i].length;
		}
		return results;
	}

	/** Index all text files under a directory. */
	public static void main(String[] args) {
String usage = "java es.udc.fic.TrainingTestNPL" + " -evaljm INT1-INT2 INT3-INT4 -evaldir INT1-INT2 INT3-INT4"
		+ "-cut N -metrica P | R | MAP -indexin PATHNAME -outfile RESULTS\n\n";
		
		
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
		
		FileWriter fichero=null;
		try {
			fichero = new FileWriter(out);
			PrintWriter pw = new PrintWriter(fichero);
			
			if (jm) {
				float[][] jmTrainResults = training(0.1f,trainjm,true,metr,indexPath,n);
				float lambda=0.1f * max(jmTrainResults);
				if (lambda==0) lambda=0.0001f;
				float[] jmTestResults= transform(SearchEvalNPL.comenzarBusqueda(Path.of(indexPath),metr,n,testjm[0]+"-"+testjm[1],"jm",lambda));
				
				if(dir) {
					float[][] dirTrainResults = training(500f,traindir,false,metr,indexPath,n);
					float mu=500f * max(dirTrainResults);
					
					float[] dirTestResults=transform(SearchEvalNPL.comenzarBusqueda(Path.of(indexPath),metr,n,testdir[0]+"-"+testdir[1],"dir",mu));
					System.out.println("\n*****************************************************************RESULTADOS*****************************************************************\n");
					resulatdos(dirTrainResults, dirTestResults, 500f ,mu ,traindir[0], testdir[0],"µ",pw);
				}else System.out.println("\n*****************************************************************RESULTADOS*****************************************************************\n");
				resulatdos(jmTrainResults, jmTestResults, 0.1f ,lambda ,trainjm[0], testjm[0],"λ",pw);
			}else {
				
				float[][] dirTrainResults = training(500f,traindir,false,metr,indexPath,n);
				float mu=500f * max(dirTrainResults);
				float[] dirTestResults=transform(SearchEvalNPL.comenzarBusqueda(Path.of(indexPath),metr,n,testdir[0]+"-"+testdir[1],"dir",mu));
				System.out.println("\n*****************************************************************RESULTADOS*****************************************************************\n");
				resulatdos(dirTrainResults, dirTestResults, 500f ,mu ,traindir[0], testdir[0],"µ",pw);
			}
		} catch (IOException e) {
			
			e.printStackTrace();
		}finally {
	           try {
	           if (null != fichero)
	              fichero.close();
	           } catch (Exception e2) {
	              e2.printStackTrace();
	           }
	        }
         
			
	}
	
}
