/*
 * Alumnos: Francisco Ferraz (737312) y Guillermo Cruz (682433)
 * Nombre fichero: Evaluation.java
 * Fecha: 15 de diciembre de 2019
 * Descripcion: Programa que evalua los resultados devueltos por el sistema tradicional
 * 				para unas determinadas necesidades de informacion
 * */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;

public class Evaluation {
	
	//Para medida total
	static double sumaPrecision = 0;
	static double sumaRecall = 0;
	static double sumaF1 = 0;
	static double sumaPrecA10 = 0;
	static double sumaMap = 0;
	static double sumaInterpolatedPrecision[] = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
	

	public static void main(String[] args) throws IOException {
		//Manejar argumentos
		String usage = "java Evaluation"
                + " -qrels <qrelsFileName> -results <resultsFileName> -output <outputFileName>\n\n";
		
		String qrelsFileName = null;
		String resultsFileName = null;
	    String outputFileName = null;
	    
	    for(int i=0;i<args.length;i++) {
	        if ("-qrels".equals(args[i])) {
	        	qrelsFileName = args[i+1];
	        	i++;
	        } else if ("-results".equals(args[i])) {
	        	resultsFileName = args[i+1];
		        i++;
	        }
	        else if ("-output".equals(args[i])) {
	        	outputFileName = args[i+1];
		        i++;
	        }
	    }
	    
	    if (qrelsFileName == null || resultsFileName == null || outputFileName == null) {
	        System.err.println("Usage: " + usage);
	        System.exit(1);
	    }
	    
	    //Almacenar juicios de relevancia de los jueces
	    HashMap<String, Integer> qReels = leeQrels(qrelsFileName);
	    //Almacenar resultados devueltos por el sistema
	    String[] results = leeResults(resultsFileName);
	    
	    //Sacar numero de consultas
	    int numberNeeds = 1;
	    String need = results[0].substring(0, 4);
	    for(int i=1; i<results.length; i++) {
	    	if(!need.equals(results[i].substring(0, 4))) {
				numberNeeds++;
				need = results[i].substring(0, 4);
			}
		}
	    
	    //Formato de decimales para la salida
	    DecimalFormatSymbols s = new DecimalFormatSymbols();
		s.setDecimalSeparator('.');
		DecimalFormat f = new DecimalFormat("0.000", s);
	    
		//Generar salida por necesidad de informacion
	    BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName));
	    for(int i=0; i<numberNeeds; i++) {
	    	generarSalida(qReels, results, i+1, writer, f);
	    	writer.newLine();
	    	writer.flush();
	    }
	    
	    //Generar evaluacion global del sistema
	    writer.write("TOTAL");
	    writer.newLine();
	    writer.write("precision\t"+f.format((double)sumaPrecision/numberNeeds));
		writer.newLine();
		writer.write("recall\t"+f.format((double)sumaRecall/numberNeeds));
		writer.newLine();
		writer.write("F1\t"+f.format((double)sumaF1/numberNeeds));
		writer.newLine();
		writer.write("prec@10\t"+f.format((double)sumaPrecA10/numberNeeds));
		writer.newLine();
		writer.write("MAP\t"+f.format((double)sumaMap/numberNeeds));
		writer.newLine();
		writer.write("interpolated_recall_precision");
		writer.newLine();
		double nivelRecall = 0.000;
		for(int i=0; i<sumaInterpolatedPrecision.length; i++) {
			writer.write(f.format(nivelRecall)+"\t"+f.format((double)sumaInterpolatedPrecision[i]/numberNeeds));
			writer.newLine();
			nivelRecall = nivelRecall + 0.100;
		}
	    
	    writer.close();
	}
	
	//Devuelve una estructura de datos HashMap que almacena el documento junto a su juicio de relevancia
	static HashMap<String, Integer> leeQrels(String fileName) throws FileNotFoundException, IOException {
		HashMap<String, Integer> res = new HashMap<String, Integer>();
		String linea;
		FileReader f = new FileReader(fileName);
        BufferedReader b = new BufferedReader(f);
        
        String[] parts;
        while((linea = b.readLine())!=null) {
        	parts = linea.split("\t");
        	res.put(parts[0]+"_"+parts[1], Integer.parseInt(parts[2]));
        }
        b.close();
        f.close();
        return res;
	}
	
	//Devuelve un array de cadenas que representan cada documento devuelto por el sistema tradicional
	static String[] leeResults(String fileName) throws FileNotFoundException, IOException {
		String aux = "";
		String linea;
		FileReader f = new FileReader(fileName);
        BufferedReader b = new BufferedReader(f);
        
        String[] parts;
        while((linea = b.readLine())!=null) {
        	parts = linea.split("\t");
        	aux = aux + parts[0]+"_"+parts[1]+",";
        }
        b.close();
        f.close();
        return aux.substring(0, aux.length()-1).split(",");
	}
	
	//Genera la salida para cada una de las necesidades de informacion
	static void generarSalida(HashMap<String, Integer> qReels, String[] results, int need, BufferedWriter writer, DecimalFormat f) throws IOException {
		//Sacar la necesidad
		String needId = results[0].substring(0, 4);
		int x = 1;
		if(need != 1) {
			for(int i=1; i<results.length; i++) {
		    	if(!needId.equals(results[i].substring(0, 4))) {
					x++;
					needId = results[i].substring(0, 4);
					if(x==need) {
						break;
					}
				}
			}
		}
		writer.write("INFORMATION_NEED\t"+needId+"\n");
		
		//Sacamos precision
		int tp=0, fp=0;
		int relevante;
		for(int i=0; i<results.length; i++){
			if(needId.equals(results[i].substring(0, 4))) {
				if(qReels.containsKey(results[i])) {
					relevante = qReels.get(results[i]);
					if(relevante==1) {
						tp++;
					}
					else {
						fp++;
					}
				}
				else {
					fp++;
				}
			}
		}
		double precision = (double)tp/(tp+fp);
		sumaPrecision = sumaPrecision + precision;
		
		//Sacamos recall
		int fn=0;
		for (String i : qReels.keySet()) {
			if(needId.equals(i.substring(0, 4))) {
				relevante = qReels.get(i);
				if(relevante==1 && !haSidoDevuelto(i, results)) {
					fn++;
				}
			}
		}
		double recall = (double)tp/(tp+fn);
		sumaRecall = sumaRecall + recall;
		
		//Sacamos F1 balanceada con B=1
		double F1 = 0;
		if(precision+recall > 0) {
			F1 = (double)(2*precision*recall)/(precision+recall);
		}
		sumaF1 = sumaF1 + F1;
		
		//Sacamos precision a 10
		int tp_10=0, cont=0;
		for(int i=0; i<results.length; i++){
			if(needId.equals(results[i].substring(0, 4))) {
				cont++;
				if(qReels.containsKey(results[i])) {
					relevante = qReels.get(results[i]);
					if(relevante==1) {
						tp_10++;
					}
				}
			}
			if(cont == 10) {
				break;
			}
		}
		double precA10 = (double)tp_10/10;
		sumaPrecA10 = sumaPrecA10 + precA10;
		
		//Sacamos precision promedio
		int numRel=0, numDocs=0;
		double precAn=0, suma=0;
		for(int i=0; i<results.length; i++){
			if(needId.equals(results[i].substring(0, 4))) {
				numDocs++;
				if(qReels.containsKey(results[i])) {
					relevante = qReels.get(results[i]);
					if(relevante==1) {
						numRel++;
						precAn = (double)numRel/numDocs;
						suma = suma + precAn;
					}
				}
			}
		}
		double precisionPromedio = 0;
		if(numRel>0) {
			precisionPromedio = (double)suma/numRel;
		}
		sumaMap = sumaMap + precisionPromedio;
		
		writer.write("precision\t"+f.format(precision));
		writer.newLine();
		writer.write("recall\t"+f.format(recall));
		writer.newLine();
		writer.write("F1\t"+f.format(F1));
		writer.newLine();
		writer.write("prec@10\t"+f.format(precA10));
		writer.newLine();
		writer.write("average_precision\t"+f.format(precisionPromedio));
		writer.newLine();
		
		//Sacamos puntos exhaustividad-precision
		writer.write("recall_precision");
		writer.newLine();
		int tp2=0, fp2=0;
		double[] precision2 = new double[tp];
		double[] recall2 = new double[tp];
		for(int i=0; i<results.length; i++){
			if(needId.equals(results[i].substring(0, 4))) {
				if(qReels.containsKey(results[i])) {
					relevante = qReels.get(results[i]);
					if(relevante==1) {
						tp2++;
						precision2[tp2-1] = (double)tp2/(tp2+fp2);
						//fn = tp+fn-tp2
						recall2[tp2-1] = (double)tp2/(tp2+(tp+fn-tp2));
						writer.write(f.format(recall2[tp2-1])+"\t"+f.format(precision2[tp2-1]));
						writer.newLine();
					}
					else {
						fp2++;
					}
				}
				else {
					fp2++;
				}
			}		
		}
		
		//Sacamos puntos exhaustividad-precision interpolados
		writer.write("interpolated_recall_precision");
		writer.newLine();
		double nivelRecall[] = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
		double precision3[] = new double[11];
		int aPartirDe=0;
		for(int i=0; i<nivelRecall.length; i++) {
			aPartirDe = indiceMayor(nivelRecall[i], recall2);
			if(aPartirDe==-1) {
				precision3[i] = 0;
			}
			else {
				precision3[i] = mayor(aPartirDe, precision2);
			}
			sumaInterpolatedPrecision[i] = sumaInterpolatedPrecision[i] + precision3[i];
			writer.write(f.format(nivelRecall[i])+"\t"+f.format(precision3[i]));
			writer.newLine();
		}
	}
	
	//Verdadero si y solo si <doc> se encuentra en el vector <results>.
	//Falso en caso contrario
	static boolean haSidoDevuelto(String doc, String[] results) {
		for(int i=0; i<results.length; i++) {
			if(doc.equals(results[i])) {
				return true;
			}
		}
		return false;
	}
	
	//Devuelve el primer indice encontrado tal que v[indice] >= <nivel>
	static int indiceMayor(double nivel, double[] v) {
		for(int i=0; i<v.length; i++) {
			if(nivel<=v[i]) {
				return i;
			}
		}
		return -1;
	}
	
	//Devuelve el elemento mayor del array <v> a partir del indice <n>
	static double mayor(int n, double[] v) {
		double max = v[n];
		for(int i=n+1; i<v.length; i++) {
			if(v[i]>max) {
				max=v[i];
			}
		}	
		return max;
	}

}

