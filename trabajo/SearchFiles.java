package org.apache.lucene.demo;

/*
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
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;
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
	        	
	          BufferedWriter writer = new BufferedWriter(new FileWriter(output));
	          
	          DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
	          DocumentBuilder builder = builderFactory.newDocumentBuilder();
	          org.w3c.dom.Document document = builder.parse(file.getPath());
	          
	          NodeList listIds = document.getElementsByTagName("identifier");
	          NodeList listTexts = document.getElementsByTagName("text");
	          
	          IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
	          IndexSearcher searcher = new IndexSearcher(reader);
	          Analyzer analyzer = new SpanishAnalyzer2(SpanishAnalyzer2.createStopSet3());
	          
	          QueryParser parser = new QueryParser(field, analyzer);
	          
	          String id="", text="";
	          Query[] querys = new Query[5];
	          BooleanQuery[] bQuerys = new BooleanQuery[4];
	          for(int i=0; i<listIds.getLength(); i++) {
	        	  for(int k=0; k<querys.length; k++) {
	        		  querys[k]=null;
	        	  }
	        	  for(int k=0; k<bQuerys.length; k++) {
	        		  bQuerys[k]=null;
	        	  }
	        	  
	        	  Query finRangeQuery = null;
	        	  Query beginRangeQuery = null;
	        	  
	        	  id = listIds.item(i).getTextContent();
	        	  System.out.println(id);
	        	  text = listTexts.item(i).getTextContent().trim().replace(".", "").replace(",", "");
	        	  text = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("[\\p{InCombiningDiacriticalMarks}]", "").replace("?", "");
	        	  text = text.replace("?", "").replace("¿", "");
	        	  
	        	  TokenNameFinderModel model;
	        	  try (InputStream modelIn = new FileInputStream("es-ner-person.bin")){
	        		 model = new TokenNameFinderModel(modelIn);
	        	  }
	        	  NameFinderME nameFinder = new NameFinderME(model);
	        	  
	        	  String[] textNombres = text.split(" ");
	        	  Span nameSpans[] = nameFinder.find(textNombres);
	        	  
	        	  /* CONSULTA DE NOMBRES */
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
	                   //System.out.println("Searching for: " + query.toString(field));
	                   //doPagingSearch(in, searcher, query, 10, false, true);
                  }
                  
                  text = text.toLowerCase();
	        	  Map<String, Integer> fechas = encuentraFechas(text);	  	        	  
	        	  text = encuentraNombres(text).trim();
	        	  text = encuentraBachelorMaster(text).trim();
	        	  
	        	  /* SEGUNDA PASADA CONSULTA DE NOMBRES */
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
	                  //System.out.println("Searching for: " + query.toString(field));
	                  //doPagingSearch(in, searcher, query, 10, false, true);
	        	  }
	        	  
	        	  
	        	  /*  CONSULTA DE FECHAS (EN CASO DE QUE LAS HAYA)	*/
	        	  Iterator<Map.Entry<String, Integer>> it = fechas.entrySet().iterator(); 
	              
	              while(it.hasNext()) 
	              { 
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
			        	  	  
			        	  	 //begin <= fecha fin
				              beginRangeQuery = DoublePoint.newRangeQuery("date" , Double.NEGATIVE_INFINITY, fin);
				              //end >= fecha inicio
				              finRangeQuery = DoublePoint.newRangeQuery("date", inicio , Double.POSITIVE_INFINITY);
				              
				              bQuerys[0] = new BooleanQuery.Builder()
				            		  .add(beginRangeQuery, BooleanClause.Occur.MUST)
				            		  .add(finRangeQuery, BooleanClause.Occur.MUST).build();
				              
				              //System.out.println("Searching for: " + query.toString(field));
				              
				              //doPagingSearch(in, searcher, booleanQuery, 10, false, true);
			        	  	  
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
			        	  	  
			        	  	  //begin <= fecha fin
				              //Query beginRangeQuery = DoublePoint.newRangeQuery("date" , Double.NEGATIVE_INFINITY, fin);
				              //end >= fecha inicio
				              finRangeQuery = DoublePoint.newRangeQuery("date", inicio , Double.POSITIVE_INFINITY);
				              
				              bQuerys[1] = new BooleanQuery.Builder()
				            		  .add(finRangeQuery, BooleanClause.Occur.MUST).build();
				              
				              //System.out.println("Searching for: " + query.toString(field));
				              
				              //doPagingSearch(in, searcher, booleanQuery, 10, false, true);
				              
		        			  break;
		        		  case("anterior"):
		        			  aux = entry.getValue().toString();
		        		  
			        	  	  if(aux.length()<8){
				          		  for(int k=aux.length(); k<8; k++) {
				          			  aux+="0";
				          		  }
				          	  }
			        	  	  
			        	  	  fin=Double.parseDouble(aux);
			        	  	  
			        	  	  //begin <= fecha fin
				              beginRangeQuery = DoublePoint.newRangeQuery("date" , Double.NEGATIVE_INFINITY, fin);
				              //end >= fecha inicio
				              //finRangeQuery = DoublePoint.newRangeQuery("date", inicio , Double.POSITIVE_INFINITY);
				              
				              bQuerys[2] = new BooleanQuery.Builder()
				            		  .add(beginRangeQuery, BooleanClause.Occur.MUST).build();
				              
				              //System.out.println("Searching for: " + query.toString(field));
				              
				              //doPagingSearch(in, searcher, booleanQuery, 10, false, true);
				              
		        			  break;
		        		  case("posterior"):
		        			  aux = entry.getValue().toString();
		        		  
			        	  	  if(aux.length()<8){
				          		  for(int k=aux.length(); k<8; k++) {
				          			  aux+="0";
				          		  }
				          	  }
			        	  	  
			        	  	  inicio=Double.parseDouble(aux);
			        	  	  
			        	  	  //begin <= fecha fin
				              //Query beginRangeQuery = DoublePoint.newRangeQuery("date" , Double.NEGATIVE_INFINITY, fin);
				              //end >= fecha inicio
				              finRangeQuery = DoublePoint.newRangeQuery("date", inicio , Double.POSITIVE_INFINITY);
				              
				              bQuerys[3] = new BooleanQuery.Builder()
				            		  .add(finRangeQuery, BooleanClause.Occur.MUST).build();
				              
				              //System.out.println("Searching for: " + query.toString(field));
				              
				              //doPagingSearch(in, searcher, booleanQuery, 10, false, true);
			        	  	  
			        	  	  
		        			  break;
		        		  default:
		        			  break;
		        		 } 
	              }
	              
	              /*  CONSULTA DE TIPO	*/
	              if(text.contains("_grado_")) {
	            	  text = text.replace("_grado_", "");
	            	  querys[2] = parser.parse("type:bachelorthesis");
	            	  //System.out.println("Searching for: " + query.toString(field));
	                  //doPagingSearch(in, searcher, query, 10, false, true);
	              }
	              if(text.contains("_master_")) {
	            	  text = text.replace("_master_", "");
	            	  querys[3] = parser.parse("type:masterthesis");
	            	  //System.out.println("Searching for: " + query.toString(field));
	                  //doPagingSearch(in, searcher, query, 10, false, true);
	              }
	              
	              /* CONSULTA DE PALABRAS IMPORTANTES */
	        	  POSModel modelPOST;
	        	  try (InputStream modelIn = new FileInputStream("es-pos-maxent.bin")){
		        		 modelPOST = new POSModel(modelIn);
	        	  }
	        	  
	        	  POSTaggerME tagger = new POSTaggerME(modelPOST);
	        	  
	        	  System.out.println(text);
	        	  String textSplit[] = text.split(" ");
	        	  String tags[] = tagger.tag(textSplit);
	        	  
	        	  String consultaImp = "";
	        	  for(int k=0; k<tags.length; k++) {
	        		  if(tags[k].equals("NC") || tags[k].equals("AO") || tags[k].equals("AQ")) {
	        			  if(!textSplit[k].equals("")) {
	        				  consultaImp = consultaImp + "description:" + textSplit[k] + " "
	        						  					+ "subject:" + textSplit[k] + " "
	        						  					+ "title:" + textSplit[k] + " ";
	        			  }
	        		  }
	        	  }
	        	  
	        	  querys[4] = parser.parse(consultaImp);
	        	  
	        	  /*BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
	        	  booleanQuery.add(queryNombres1, BooleanClause.Occur.SHOULD);
	        	  finalQuery = new BooleanQuery.Builder().build();
	        	  finalQuery.;
	            		  .add(queryNombres1, BooleanClause.Occur.SHOULD)
	            		  .add(queryNombres2, BooleanClause.Occur.SHOULD)
	            		  .add(queryBach, BooleanClause.Occur.SHOULD)
	            		  .add(queryMaster, BooleanClause.Occur.SHOULD)
	            		  .add(queryImp, BooleanClause.Occur.SHOULD)
	            		  .add(bQueryIntervalos, BooleanClause.Occur.SHOULD)
	            		  .add(bQueryUltimos, BooleanClause.Occur.SHOULD)
	            		  .add(bQueryAnterior, BooleanClause.Occur.SHOULD)
	            		  .add(bQueryPosterior, BooleanClause.Occur.SHOULD).build();*/
	        	  
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
                doPagingSearch(searcher, finalQuery, id, writer);
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


  /** Simple command-line based search demo. */
  public static void main(String[] args) throws Exception {
    String usage =
      "Usage:\tjava org.apache.lucene.demo.SearchFiles [-index dir] [-field f] [-repeat n] [-queries file] [-query string] [-raw] [-paging hitsPerPage]\n\nSee http://lucene.apache.org/core/4_1_0/demo/ for details.";
    if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
      System.out.println(usage);
      System.exit(0);
    }

    String index = "index";
    String field = "contents";
    String output = "";
    String queries = null;
    int repeat = 0;
    boolean raw = false;
    String queryString = null;
    int hitsPerPage = 10;
    
    String infoNeeds = null;
    
    for(int i = 0;i < args.length;i++) {
      if ("-index".equals(args[i])) {
        index = args[i+1];
        i++;
      } else if ("-output".equals(args[i])) {
          output = args[i+1];
      }
      else if ("-field".equals(args[i])) {
        field = args[i+1];
        i++;
      } else if ("-queries".equals(args[i])) {
        queries = args[i+1];
        i++;
      } else if ("-query".equals(args[i])) {
        queryString = args[i+1];
        i++;
      } else if ("-repeat".equals(args[i])) {
        repeat = Integer.parseInt(args[i+1]);
        i++;
      } else if ("-raw".equals(args[i])) {
        raw = true;
      } else if ("-paging".equals(args[i])) {
        hitsPerPage = Integer.parseInt(args[i+1]);
        if (hitsPerPage <= 0) {
          System.err.println("There must be at least 1 hit per page.");
          System.exit(1);
        }
        i++;
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
    
    final File needsDir = new File(infoNeeds);
    if (!needsDir.exists() || !needsDir.canRead()) {
      System.out.println("Document directory '" +needsDir.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
      System.exit(1);
    }
    
    processingNeeds(needsDir, index, field, output);
    
    IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
    IndexSearcher searcher = new IndexSearcher(reader);
    Analyzer analyzer = new SpanishAnalyzer2(SpanishAnalyzer2.createStopSet3());

    /*BufferedReader in = null;
    if (queries != null) {
      in = new BufferedReader(new InputStreamReader(new FileInputStream(queries), "UTF-8"));
    } else {
      in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
    }
    QueryParser parser = new QueryParser(field, analyzer);
    while (true) {
      if (queries == null && queryString == null) {                        // prompt the user
        System.out.println("Enter query: ");
      }

      String line = queryString != null ? queryString : in.readLine();

      if (line == null || line.length() == -1) {
        break;
      }

      line = line.trim();
      if (line.length() == 0) {
        break;
      }
    	   	  
	  Query query = parser.parse(line);
      System.out.println("Searching for: " + query.toString(field));
            
      if (repeat > 0) {                           // repeat & time as benchmark
        Date start = new Date();
        for (int i = 0; i < repeat; i++) {
          searcher.search(query, 100);
        }
        Date end = new Date();
        System.out.println("Time: "+(end.getTime()-start.getTime())+"ms");
      }
      
      String primeraConsulta = line.substring(0,line.indexOf(":"));
      
      if(primeraConsulta.equals("spatial") && line.indexOf(" ")==-1) { //Consulta únicamente espacial
    	  String[] coordenadas = (line.substring(line.indexOf(":")+1)).split(",");
    	  
    	  Double east = Double.parseDouble(coordenadas[1]);
          Double west = Double.parseDouble(coordenadas[0]);
          Double north = Double.parseDouble(coordenadas[3]);
          Double south = Double.parseDouble(coordenadas[2]);
          
          //Xmin <= east
          Query westRangeQuery = DoublePoint.newRangeQuery("west" , Double.NEGATIVE_INFINITY, east);
          //Xmax >= west
          Query eastRangeQuery = DoublePoint.newRangeQuery("east", west , Double.POSITIVE_INFINITY);
          //Ymin <= north
          Query southRangeQuery = DoublePoint.newRangeQuery("south", Double.NEGATIVE_INFINITY, north);
          //Ymax >= south
          Query northRangeQuery = DoublePoint.newRangeQuery("north", south, Double.POSITIVE_INFINITY);
          
          BooleanQuery query = new BooleanQuery.Builder()
        		  .add(westRangeQuery, BooleanClause.Occur.MUST)
        		  .add(eastRangeQuery, BooleanClause.Occur.MUST)
        		  .add(southRangeQuery, BooleanClause.Occur.MUST)
        		  .add(northRangeQuery, BooleanClause.Occur.MUST).build();
          
          System.out.println("Searching for: " + query.toString(field));
          
          doPagingSearch(in, searcher, query, hitsPerPage, raw, queries == null && queryString == null);
      }
      else if(primeraConsulta.equals("spatial") && line.indexOf(" ")!=-1) {	//Consulta combinada
    	  String[] coordenadas = (line.substring(line.indexOf(":")+1, line.indexOf(" "))).split(",");
    	  
    	  Double east = Double.parseDouble(coordenadas[1]);
          Double west = Double.parseDouble(coordenadas[0]);
          Double north = Double.parseDouble(coordenadas[3]);
          Double south = Double.parseDouble(coordenadas[2]);
          
          //Xmin <= east
          Query westRangeQuery = DoublePoint.newRangeQuery("west" , Double.NEGATIVE_INFINITY, east);
          //Xmax >= west
          Query eastRangeQuery = DoublePoint.newRangeQuery("east", west , Double.POSITIVE_INFINITY);
          //Ymin <= north
          Query southRangeQuery = DoublePoint.newRangeQuery("south", Double.NEGATIVE_INFINITY, north);
          //Ymax >= south
          Query northRangeQuery = DoublePoint.newRangeQuery("north", south, Double.POSITIVE_INFINITY);
          
          BooleanQuery query1 = new BooleanQuery.Builder()
        		  .add(westRangeQuery, BooleanClause.Occur.MUST)
        		  .add(eastRangeQuery, BooleanClause.Occur.MUST)
        		  .add(southRangeQuery, BooleanClause.Occur.MUST)
        		  .add(northRangeQuery, BooleanClause.Occur.MUST).build();
    	  
    	  String resto = line.substring(line.indexOf(" ")+1);   	  
    	  Query query2 = parser.parse(resto);
    	  
    	  BooleanQuery queryFinal = new BooleanQuery.Builder()
        		  .add(query1, BooleanClause.Occur.SHOULD)
        		  .add(query2, BooleanClause.Occur.SHOULD).build();
    	  
    	  System.out.println("Searching for: " + queryFinal.toString(field));
          
          doPagingSearch(in, searcher, queryFinal, hitsPerPage, raw, queries == null && queryString == null);
    	  
      }
      else if(primeraConsulta.equals("temporal")) {
    	  String fechaInicio = line.substring(line.indexOf("[")+1, line.indexOf(" "));
    	  if(fechaInicio.length()<8){
    		  for(int i=fechaInicio.length(); i<8; i++) {
    			  fechaInicio+="0";
    		  }
    	  }

    	  String aux = line.substring(line.indexOf(":")).toLowerCase(); 
    	  String fechaFinal = aux.substring(aux.indexOf("o")+2, aux.indexOf("]"));
    	  if(fechaFinal.length()<8){
    		  for(int i=fechaFinal.length(); i<8; i++) {
    			  fechaFinal+="0";
    		  }
    	  }
    	  
    	  Double inicio = Double.parseDouble(fechaInicio);
    	  Double fin = Double.parseDouble(fechaFinal);
    	  
    	  //begin <= fecha fin
          Query beginRangeQuery = DoublePoint.newRangeQuery("begin" , Double.NEGATIVE_INFINITY, fin);
          //end >= fecha inicio
          Query finRangeQuery = DoublePoint.newRangeQuery("end", inicio , Double.POSITIVE_INFINITY);
          
          BooleanQuery query = new BooleanQuery.Builder()
        		  .add(beginRangeQuery, BooleanClause.Occur.MUST)
        		  .add(finRangeQuery, BooleanClause.Occur.MUST).build();
          
          System.out.println("Searching for: " + query.toString(field));
          
          doPagingSearch(in, searcher, query, hitsPerPage, raw, queries == null && queryString == null);
      }
      else {	//No espacial
    	  Query query = parser.parse(line);
          System.out.println("Searching for: " + query.toString(field));
                
          if (repeat > 0) {                           // repeat & time as benchmark
            Date start = new Date();
            for (int i = 0; i < repeat; i++) {
              searcher.search(query, 100);
            }
            Date end = new Date();
            System.out.println("Time: "+(end.getTime()-start.getTime())+"ms");
          }
          
          doPagingSearch(in, searcher, query, hitsPerPage, raw, queries == null && queryString == null);
      }

      if (queryString != null) {
        break;
      }
    }*/
    reader.close();
  }
  
  

  /**
   * This demonstrates a typical paging search scenario, where the search engine presents 
   * pages of size n to the user. The user can then go to the next page if interested in
   * the next hits.
   * 
   * When the query is executed for the first time, then only enough results are collected
   * to fill 5 result pages. If the user wants to page beyond this limit, then the query
   * is executed another time and all hits are collected.
   * 
   */
  public static void doPagingSearch(IndexSearcher searcher, Query query, String idNeed, BufferedWriter writer) throws IOException {
	
    
    TopDocs results = searcher.search(query, 17540);
    ScoreDoc[] hits = results.scoreDocs;
    
    int numTotalHits = (int)results.totalHits;
    System.out.println(numTotalHits + " total matching documents"); 
    
	  for (int i = 0; i < hits.length; i++) {
	    
	    Document doc = searcher.doc(hits[i].doc);
	    
	    String path = doc.get("path");
	    if (path != null) {
	    	writer.write(idNeed+"\t"+path.substring(path.indexOf("\\")+1));
	    	writer.newLine();
	      System.out.println(prueba+" "+idNeed+"\t"+path.substring(path.indexOf("\\")+1));
	      prueba++;
	      // explain the scoring function
	      //System.out.println(searcher.explain(query, hits[i].doc));
	    } else {
	      System.out.println((i+1) + ". " + "No path for this document");
	    }
	              
	  }
  }
  
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