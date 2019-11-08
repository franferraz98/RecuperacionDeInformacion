package org.apache.lucene.demo;

/*
 *  Alumnos: Francisco Ferraz (737312) y Guillermo Cruz (682433)
 * Nombre fichero: SearchFiles.java
 * Fecha: 08 de noviembre de 2019
 * Descripción: Archivo que procesa unas necesidades de información en lenguaje natural y
 * 				crea consultas atendiendo a dichas necesidades, guardando los documentos
 * 				relevantes devueltos de un índice previamente creado en un fichero de texto.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.util.Span;

/** Simple command-line based search demo. */
public class SearchFiles {

  private SearchFiles() {}
  
  static int prueba = 1;
  
  /** Simple command-line based search demo. */
  public static void main(String[] args) throws Exception {
    String usage =
      "Usage:\tjava org.apache.lucene.demo.SearchFiles -index <indexPath> -infoNeeds <infoNeedsFile> -output <resultsFile> [-paging hitsPerPage]\n";

    String index = "index";
    String output = "";
    String infoNeeds = null;
    String field = "contents";
    
    for(int i = 0;i < args.length;i++) {
      if ("-index".equals(args[i])) {
        index = args[i+1];
        i++;
      }
      else if ("-output".equals(args[i])) {
         output = args[i+1];
      }
      else if("-infoNeeds".equals(args[i])) {
    	  infoNeeds = args[i+1];
          i++;
      }
    }
    
    if (infoNeeds == null) {
        System.err.println("Usage: " + usage);
        System.exit(1);
    }
    if (output.equals("")) {
        System.err.println("Usage: " + usage);
        System.exit(1);
    }
    
    final File needsDir = new File(infoNeeds);
    if (!needsDir.exists() || !needsDir.canRead()) {
      System.out.println("Document directory '" +needsDir.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
      System.exit(1);
    }
    
    processingNeeds(needsDir, index, field, output);
  }
  
  /*
   * Procesa las necesidades de informacion del fichero <file> y realiza las consultas equivalentes
   */
  public static void processingNeeds(File file, String index, String field, String output)
	    throws IOException, ParserConfigurationException, SAXException, Exception {
	    // do not try to index files that cannot be read
	    if (file.canRead()) {
	      //Si es un directorio
	      if (file.isDirectory()) {
	        String[] files = file.list();	//Recuperamos lista de los ficheros
	        // an IO error could occur
	        if (files != null) {
	          for (int i = 0; i < files.length; i++) {
	        	//Recursividad fichero a fichero
	        	  processingNeeds(new File(file, files[i]), index, field, output);
	          }
	        }
	      //es archivo
	      } else {

	        FileInputStream fis;
	        //Intentamos abrir el fichero
	        try {
	          fis = new FileInputStream(file);
	        } catch (FileNotFoundException fnfe) {
	          // at least on windows, some temporary files raise this exception with an "access denied" message
	          // checking if the file can be read doesn't help
	          return;
	        }

	        try {
	        	
	          //Para escribir el resultado en el fichero de salida
	          BufferedWriter writer = new BufferedWriter(new FileWriter(output));
	          
	          //Extraemos la información relevante de las necesidades de información
	          DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
	          DocumentBuilder builder = builderFactory.newDocumentBuilder();
	          org.w3c.dom.Document document = builder.parse(file.getPath());
	          
	          NodeList listIds = document.getElementsByTagName("identifier");	//Lista de ids de las necesidades
	          NodeList listTexts = document.getElementsByTagName("text");		//Lista con los textos
	          
	          //Para realizar las consultas
	          IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
	          IndexSearcher searcher = new IndexSearcher(reader);
	          Analyzer analyzer = new SpanishAnalyzer2(SpanishAnalyzer2.createStopSet3());
	          QueryParser parser = new QueryParser(field, analyzer);
	          
	          String id="", text="";
	          Query[] querys = new Query[5];
	          BooleanQuery[] bQuerys = new BooleanQuery[4];
	          
	          //Bucle en el que se recorre el texto de cada una de las necesidades de información
	          //y se realiza la consulta oportuna tras procesarlo
	          for(int i=0; i<listIds.getLength(); i++) {
	        	  for(int k=0; k<querys.length; k++) {
	        		  querys[k]=null;
	        	  }
	        	  for(int k=0; k<bQuerys.length; k++) {
	        		  bQuerys[k]=null;
	        	  }
	        	  Query finRangeQuery = null;
	        	  Query beginRangeQuery = null;
	        	  
	        	  id = listIds.item(i).getTextContent();	//obtenemos el id de la necesidad
	        	  
	        	  //obtenemos el texto eliminando '.', ',', '?', '¿' y acentos
	        	  text = listTexts.item(i).getTextContent().trim().replace(".", "").replace(",", "").replace(")", "").replace("(", "");
	        	  text = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
	        	  text = text.replace("?", "").replace("¿", "");
	        	  
	        	  //Se utiliza una red neuronal para encontrar nombres de personas en el texto
	        	  TokenNameFinderModel model;
	        	  try (InputStream modelIn = new FileInputStream("es-ner-person.bin")){
	        		 model = new TokenNameFinderModel(modelIn);
	        	  }
	        	  NameFinderME nameFinder = new NameFinderME(model);
	        	  String[] textNombres = text.split(" ");
	        	  Span nameSpans[] = nameFinder.find(textNombres);
	        	  
	        	  /*
	        	   * CONSULTA DE NOMBRES. Se monta la consulta de nombres
	        	   * que ha encontrado la red
	        	   */
                  if(nameSpans.length>0) {
               	   String consulta = "";
	                   for(Span s: nameSpans){
	 	                  // s.getStart() : contains the start index of possible name in the input string array
	 	                  // s.getEnd() : contains the end index of the possible name in the input string array
	 	                  for(int indexx=s.getStart();indexx<s.getEnd();indexx++){
	 	                	  consulta = consulta +"creator:"+textNombres[indexx]+" ";
	 	                	  text = text.replace(textNombres[indexx], "");
	 	                  }
	 	               }
	                   
	                   querys[0] = parser.parse(consulta.trim());
                  }
                  
                  text = text.toLowerCase();
                  //Se buscan intervalos de fechas en el texto
	        	  Map<String, Integer> fechas = encuentraFechas(text);	
	        	  //Se buscan mas nombres propios por si falla la red neuronal
	        	  text = encuentraNombres(text).trim();
	        	  //Procesa el texto en busca de TFG o TFM
	        	  text = encuentraBachelorMaster(text).trim();
	        	  
	        	  /* 
	        	   * SEGUNDA PASADA CONSULTA DE NOMBRES. Por si falla la primera
	        	   */
	        	  String auxNombres[] = text.split(" ");
	        	  String consultaNombre="";
	        	  
	        	  for(int k=0; k<auxNombres.length; k++) {
	        		  if(auxNombres[k].equals("_nombre_")) {
	        			  consultaNombre=consultaNombre + "creator:"+auxNombres[k+1]+" ";
	        			  text = text.replace(auxNombres[k+1], "");
	        		  }
	        	  }
	        	  
	        	  if(!consultaNombre.isEmpty()) {
	        		  text = text.replace("_nombre_", "");
	        		  querys[1] = parser.parse(consultaNombre.trim());
	        	  }
	        	  
	        	  
	        	  /* 
	        	   * CONSULTA DE FECHAS (EN CASO DE QUE LAS HAYA)
	        	   */
	        	  Iterator<Map.Entry<String, Integer>> it = fechas.entrySet().iterator(); 
	              
	              while(it.hasNext()) { 
	            	   Map.Entry<String, Integer> entry = it.next();
	            	   String aux;
	            	   Double inicio;
	            	   
	                   switch(entry.getKey()){
		        		  case("intervalo"):
		        			  aux = entry.getValue().toString();
		        		  	  entry = it.next();
			        	  	  String aux1 = entry.getValue().toString();
			        	  	  
			        	  	  if(aux.length()<8){
				          		  for(int k=aux.length(); k<8; k++) {
				          			  aux+="0";
				          		  }
				          	  }
			        	  	  
			        	  	  if(aux1.length()<8){
				          		  for(int k=aux1.length(); k<8; k++) {
				          			aux1+="0";
				          		  }
				          	  }
			        	  	  
			        	  	  inicio=Double.parseDouble(aux);
			        	  	  Double fin=Double.parseDouble(aux1);
			        	  	  
			        	  	 //date <= fecha fin
				              beginRangeQuery = DoublePoint.newRangeQuery("date" , Double.NEGATIVE_INFINITY, fin);
				              //date >= fecha inicio
				              finRangeQuery = DoublePoint.newRangeQuery("date", inicio , Double.POSITIVE_INFINITY);
				              
				              bQuerys[0] = new BooleanQuery.Builder()
				            		  .add(beginRangeQuery, BooleanClause.Occur.MUST)
				            		  .add(finRangeQuery, BooleanClause.Occur.MUST).build();
		        			  break;
		        			  
		        		  case("ultimos"):
		        			  Integer auxNum = (int)2019-entry.getValue();
		        		  	  aux = auxNum.toString();
			        	  	  
			        	  	  if(aux.length()<8){
				          		  for(int k=aux.length(); k<8; k++) {
				          			  aux+="0";
				          		  }
				          	  }
			        	  	  
			        	  	  inicio=Double.parseDouble(aux);
			        	  	  
				              //date >= fecha inicio
				              finRangeQuery = DoublePoint.newRangeQuery("date", inicio , Double.POSITIVE_INFINITY);
				              
				              bQuerys[1] = new BooleanQuery.Builder()
				            		  .add(finRangeQuery, BooleanClause.Occur.MUST).build();
		        			  break;
		        			  
		        		  case("anterior"):
		        			  aux = entry.getValue().toString();
		        		  
			        	  	  if(aux.length()<8){
				          		  for(int k=aux.length(); k<8; k++) {
				          			  aux+="0";
				          		  }
				          	  }
			        	  	  
			        	  	  fin=Double.parseDouble(aux);
			        	  	  
			        	  	  //date <= fecha fin
				              beginRangeQuery = DoublePoint.newRangeQuery("date" , Double.NEGATIVE_INFINITY, fin);
				              
				              bQuerys[2] = new BooleanQuery.Builder()
				            		  .add(beginRangeQuery, BooleanClause.Occur.MUST).build();
		        			  break;
		        			  
		        		  case("posterior"):
		        			  aux = entry.getValue().toString();
		        		  
			        	  	  if(aux.length()<8){
				          		  for(int k=aux.length(); k<8; k++) {
				          			  aux+="0";
				          		  }
				          	  }
			        	  	  
			        	  	  inicio=Double.parseDouble(aux);
			        	  	  
				              //date >= fecha inicio
				              finRangeQuery = DoublePoint.newRangeQuery("date", inicio , Double.POSITIVE_INFINITY);
				              
				              bQuerys[3] = new BooleanQuery.Builder()
				            		  .add(finRangeQuery, BooleanClause.Occur.MUST).build();
		        			  break;
		        			  
		        		  default:
		        			  break;
		        		 } 
	              }
	              
	              /*  
	               * CONSULTA DE TIPO. Busca TFG's o TFM's
	               */
	              if(text.contains("_grado_")) {
	            	  text = text.replace("_grado_", "");
	            	  querys[2] = parser.parse("type:bachelorthesis");
	              }
	              if(text.contains("_master_")) {
	            	  text = text.replace("_master_", "");
	            	  querys[3] = parser.parse("type:masterthesis");
	              }
	              
	              /* 
	               * CONSULTA DE PALABRAS IMPORTANTES. Para el resto de palabras
	               * (nombres comunes y adjetivos)
	               */
	              
	              //Red neuronal para realizar un analisis en busca de nombres comunes
	              //y adjetivos
	        	  POSModel modelPOST;
	        	  try (InputStream modelIn = new FileInputStream("es-pos-maxent.bin")){
		        		 modelPOST = new POSModel(modelIn);
	        	  }
	        	  POSTaggerME tagger = new POSTaggerME(modelPOST);
	        	  String textSplit[] = text.split(" ");
	        	  String tags[] = tagger.tag(textSplit);
	        	  
	        	  String consultaImp = "";
	        	  
	        	  //Primera pasada para realizar consulta por cada termino
	        	  for(int k=0; k<tags.length; k++) {
	        		  if(tags[k].equals("NC") || tags[k].equals("AO") || tags[k].equals("AQ")) {
	        			  if(!textSplit[k].equals("")) {
	        				  consultaImp = consultaImp + "description:" + textSplit[k] + " "
	        						  					+ "subject:" + textSplit[k] + " "
	        						  					+ "title:" + textSplit[k] + " ";
	        			  }
	        		  }
	        	  }
	        	  
	        	  //Segunda pasada para comprobar si los nombres comunes van acompañados de adjetivos
	        	  String masImportante = "";
	        	  for(int k=0; k<tags.length; k++) {
	        		  //System.out.println(tags[k]+" ---> "+textSplit[k]);
	        		  if(tags[k].equals("NC") && (k+1)<tags.length) {
	        			  if(!textSplit[k].equals("") && tags[k+1].equals("AQ")) {	//Se buscan juntos
	        				  masImportante = masImportante + "description:\"" +textSplit[k] + " " +textSplit[k+1]+"\" "
	        						  					+ "subject:\"" +textSplit[k] + " " +textSplit[k+1]+"\" "
	        						  					+ "title:\"" +textSplit[k] + " " +textSplit[k+1]+"\" ";
	        			  }
	        		  }
	        	  }
	        	  
	        	  masImportante = masImportante + masImportante + masImportante + masImportante;
	        	  masImportante = masImportante + masImportante + masImportante;
	        	  masImportante = masImportante + masImportante;
	        	  consultaImp = consultaImp + masImportante;
	        	  querys[4] = parser.parse(consultaImp);
	        	   
	        	  //Se construye la consulta final
	        	  BooleanQuery.Builder booleanBuilder = new BooleanQuery.Builder();
	        	  for(int k=0; k<querys.length;k++) {
	        		  if(querys[k]!=null) {
	        			  booleanBuilder.add(querys[k], BooleanClause.Occur.MUST);
	        		  }
	        	  }
	        	  for(int k=0; k<bQuerys.length;k++) {
	        		  if(bQuerys[k]!=null) {
	        			  booleanBuilder.add(bQuerys[k], BooleanClause.Occur.MUST);
	        		  }
	        	  }
	        	  
	        	  BooleanQuery finalQuery = booleanBuilder.build();
	        	  
	        	  System.out.println("Searching for: " + finalQuery.toString(field));
	        	  
                  realizarConsulta(searcher, finalQuery, id, writer);
                  writer.flush();
	          }
	          reader.close();
	          writer.close();
	        } finally {
	          fis.close();
	        }
	      }
	      
	    }
  }

  /**
   * Realiza la consulta <query>, sobre la necesidad de información <idNeed>, buscando en <searcher>
   * y escribiendo el resultado de la búsqueda en el buffer de escritura <writer>
   **/
  public static void realizarConsulta(IndexSearcher searcher, Query query, String idNeed, BufferedWriter writer) throws IOException {
	
    TopDocs results = searcher.search(query, 17540);
    ScoreDoc[] hits = results.scoreDocs;
    
    //int numTotalHits = (int)results.totalHits;
    //System.out.println(numTotalHits + " total matching documents"); 
    
	  for (int i = 0; i < hits.length; i++) {
	    Document doc = searcher.doc(hits[i].doc);
	    
	    String path = doc.get("path");
	    if (path != null) {
	    	writer.write(idNeed+"\t"+path.substring(path.indexOf("\\")+1));
	    	writer.newLine();
	      //System.out.println(prueba+" "+idNeed+"\t"+path.substring(path.indexOf("\\")+1));
	    	
	      //prueba++;
	      // explain the scoring function
	      //System.out.println(searcher.explain(query, hits[i].doc));
	    } else {
	      System.out.println((i+1) + ". " + "No path for this document");
	    }
	              
	  }
  }
  
  //Devuelve un map en el que ha encontrado secuencias de fechas en <text>
  public static Map<String, Integer> encuentraFechas(String text){
	  Map<String, Integer> fechas= new HashMap<String, Integer>();
	  Scanner buscar = new Scanner(text);
	  
	  while(buscar.hasNext()) {
		  String s = buscar.next();
		  
		  if(s.equals("de") || s.equals("entre") || s.equals("desde") || s.equals("del")){
  			//de/entre/desde/del X hasta/a/y/al Y
			  if(buscar.hasNext()) s = buscar.next();
			if(isNumeric(s)) {
				int num1=Integer.parseInt(s);
				if(buscar.hasNext()) s = buscar.next();
				if(s.equals("hasta") || s.equals("a") || s.equals("y") || s.equals("al")){
					if(buscar.hasNext()) s = buscar.next();
					if(isNumeric(s)) {
						int num2=Integer.parseInt(s);
						fechas.put("intervalo", num1);
						fechas.put("intervaloNext", num2);
					}
				}
			}
  		  }
		  else if(s.equals("ultimos") || s.equals("anterior") || s.equals("anteriores")) {
			  //ultimos X anos
			  //anterior a X
			  //anteriores a X
			  if(buscar.hasNext()) s = buscar.next();
			  if(isNumeric(s)) {
				  int num1=Integer.parseInt(s);
				  if(buscar.hasNext()) s=buscar.next();
				  if(s.equals("anos")) {
					  fechas.put("ultimos", num1);
				  }
			  }
			  else if(s.equals("a")) {
				  if(buscar.hasNext()) s = buscar.next();
				  if(isNumeric(s)) {
					int num1=Integer.parseInt(s);
					fechas.put("anterior", num1);
				  }
			  }
		  }
		  else if(s.equals("a")) {
			  //a partir de X
			  if(buscar.hasNext()) {
				  s=buscar.next();
				  if(s.equals("partir")) {
					  if(buscar.hasNext()) s=buscar.next();
					  if(s.equals("de")) {
						  if(buscar.hasNext()) s=buscar.next();
						  if(isNumeric(s)) {
							  int num1 = Integer.parseInt(s);
							  fechas.put("posterior", num1);
						  }
					  }
				  }
			  }
		  }
	  }
	  
	  buscar.close();	  
	  return fechas;
  }
  
  //Devuelve una cadena en la que los nombres propios que puede haber en <cadena>
  //van precedidos de la cadena _nombre_
  public static String encuentraNombres(String cadena) throws FileNotFoundException, IOException{
	  Scanner buscar = new Scanner(cadena);
	  
	  String resultado = "";
	  String s = "";
	  boolean entra;
	  while(buscar.hasNext()) {
		  entra= false;
		  s=buscar.next();
		  
		  FileReader f = new FileReader("nombresMinusculas.txt");
	      BufferedReader b = new BufferedReader(f);
	      String nombre;
	      while((nombre = b.readLine())!=null && !entra) {
	    	  if(s.equals(nombre)) {
	    		  resultado = resultado +" _nombre_ "+s;
	    		  entra=true;
	    	  }
	      }
	      
	      if(!entra) {
	    	  resultado = resultado + " "+ s;
	      }
	      b.close();
	  }
	  
	  buscar.close();
	  return resultado;
  }
  
  //Devuelve una cadena en la que si se encuentran peticiones de 
  //TFM o TFG en <cadena>, irán precedidas por la cadena _grado_
  //o _master_
  public static String encuentraBachelorMaster(String cadena){
	  Scanner buscar = new Scanner(cadena);
	  
	  String resultado = "";
	  String s = "";
	  while(buscar.hasNext()) {
		  s=buscar.next();
		  if(s.equals("master") || s.equals("TFM")) {
			  resultado = resultado + " _master_" + s;
		  }
		  else if(s.equals("fin") && buscar.hasNext()) {
			  s=buscar.next();
			  if(s.equals("de") && buscar.hasNext()) {
				  s=buscar.next();
				  if(s.equals("grado")) {
					  resultado = resultado + " _grado_";
				  }
				  else {
					  resultado = resultado + " " + s;
				  }
			  }
			  else {
				  resultado = resultado + " " + s;
			  }
		  }
		  else {
			  resultado = resultado + " " + s;
		  }
		 
	  }
	  
	  buscar.close();
	  return resultado;
  }
  
  //Devuelve true si y solo si <cadena> equivale a un
  //numero entero
  public static boolean isNumeric(String cadena) {
      boolean resultado;

      try {
          Integer.parseInt(cadena);
          resultado = true;
      } catch (NumberFormatException excepcion) {
          resultado = false;
      }

      return resultado;
  }
}