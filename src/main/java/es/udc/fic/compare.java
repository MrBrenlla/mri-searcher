package es.udc.fic;
import com.sun.jdi.Value;
import org.apache.commons.math3.stat.inference.TTest;
import org.apache.commons.math3.stat.inference.WilcoxonSignedRankTest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;


public class compare {

    private static double [] leerResults(String pathString) {

        Path path = Path.of(pathString);
        double[] result = null;
        String [] parts;
        try (InputStream stream = Files.newInputStream(path)) {
            String s = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            s = s.replaceAll("<","");
            s = s.replaceAll(">","");
            s = s.replaceAll("\n", ";");
            s = s.replaceAll(",",".");
            parts = s.split(";");
            int i = 1;
            int j= 0;
            result = new double[parts.length/2];
            while (i<parts.length) {
                result[j] = Double.valueOf(parts[i]);
                i = i+2;
                j++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    private static void realizarTest (double [] result1, double [] result2, String test, Double alpha) {
        double pValue = -1;
        if (test.equals("t")) {
            TTest t = new TTest();
            pValue = t.pairedTTest(result1,result2);
        } else if (test.equals("wilcoxon")) {
            WilcoxonSignedRankTest wilcoxon = new WilcoxonSignedRankTest();
            pValue = wilcoxon.wilcoxonSignedRank(result1, result2);
        } else {
            System.err.println("No existe este test.");
            System.exit(-1);
        }
        if (pValue == -1) {
            System.err.println("No se ha podido calcular el pValue.");
            System.exit(-1);
        }
        System.out.println("Resultado test: " + pValue);
        if (pValue<alpha) {
        System.out.println("Dado que nuestro pValue es menor que el alpha podemos rechazar las null hypothesis.");
        } else {
            System.out.println("Dado que nuestro pValue es mayor que el alpha podemos aceptar las null hypothesis.");
        }

    }
    public static void main(String[] args) {
        String usage = "java es.udc.fic.compare" + " [-results result1-result2] [-test t|wilcoxon alpha]\n\n";
        String [] allResults;
        String result1 = null;
        String result2 = null;
        String test = null;
        double alpha = -1.0;

        for (int i = 0; i < args.length; i++) {
            if ("-results".equals(args[i])) {
                allResults = args[i+1].split("-");
                result1 = allResults[0];
                result2 = allResults[1];
                i++;
            } else if ("-test".equals(args[i])) {
                test = args[i + 1];
                alpha = Double.valueOf(args[i + 2]);
                i++;
            }
        }

        if(result1 == null || result2 == null || alpha == -1.0 || test == null) {
            System.err.println("Usage: " + usage);
            System.exit(1);
        }

        double[] FirstResult = leerResults(result1);
        double[] SecondResult = leerResults(result2);
        //for (double aux : FirstResult) System.out.println(aux);
        //for (double aux : SecondResult) System.out.println(aux);
        realizarTest(FirstResult,SecondResult,test,alpha);
    }
}
