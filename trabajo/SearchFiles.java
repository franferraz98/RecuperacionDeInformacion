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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.Date;
import java.util.HashMap;
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
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Simple command-line based search demo. */
public class SearchFiles {

  private SearchFiles() {}
  
  public static void processingNeeds(File file)
	    throws IOException, ParserConfigurationException, SAXException {
	    // do not try to index files that cannot be read
	    if (file.canRead()) {
	      //Si es un directorio
	      if (file.isDirectory()) {
	        String[] files = file.list();	//Recuperamos lista de los ficheros
	        // an IO error could occur
	        if (files != null) {
	          for (int i = 0; i < files.length; i++) {
	        	//Recursividad fichero a fichero
	        	  processingNeeds(new File(file, files[i]));
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
	          DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
	          DocumentBuilder builder = builderFactory.newDocumentBuilder();
	          org.w3c.dom.Document document = builder.parse(file.getPath());
	          
	          NodeList listIds = document.getElementsByTagName("identifier");
	          NodeList listTexts = document.getElementsByTagName("text");
	          
	          String id="", text="";
	          for(int i=0; i<listIds.getLength(); i++) {
	        	  id = listIds.item(i).getTextContent();
	        	  System.out.println(id);
	        	  text = listTexts.item(i).getTextContent().trim().replace(".", "").replace(",", "");
	        	  text = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
	        	  System.out.println(text);
	        	  
	        	  Map<String, Integer> fechas = encuentraFechas(text);
	        	  
	        	  for (Map.Entry<String, Integer> entry : fechas.entrySet()) {
	        		  System.out.println("clave=" + entry.getKey() + ", valor=" + entry.getValue());
	        	  }
	        	  
	        	  /*String[] textSinPuntos = text.split("\\.");
	        	  for(int k=0; k<textSinPuntos.length; k++) {
	        		  textSinPuntos[k] = textSinPuntos[k].trim();
	        	  }
	        	  
	        	  
	        	  String[] aux;
	        	 
	        	  int longitud=0;
	        	  for(int k=0; k<textSinPuntos.length; k++) {
	        		  aux = textSinPuntos[k].split(",");
	        		  for(int z=0; z<aux.length; z++) {
	        			  aux[z]=aux[z];
	        		  }
	        		  longitud=longitud+aux.length;
	        	  }
	        	  
	        	  String[] textSinComa = new String[longitud];
	        	  int j=0;  
	        	  for(int k=0; k<textSinPuntos.length; k++) {
	        		  aux = textSinPuntos[k].split(",");
	        		  
	        		  for(int z=0; z<aux.length; z++) {
	        			  textSinComa[j] = aux[z].trim();
	        			  j++;
	        		  }
	        	  }
	        	  
	        	  for(int k=0; k<textSinComa.length; k++) {
	        		  textSinComa[k]=Normalizer.normalize(textSinComa[k], Normalizer.Form.NFD).replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
	        		  System.out.println(textSinComa[k]);
	        	  }*/ 	  

	          }

	        } finally {
	          fis.close();
	        }
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
      } else if ("-field".equals(args[i])) {
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
    
    processingNeeds(needsDir);
    
    IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
    IndexSearcher searcher = new IndexSearcher(reader);
    Analyzer analyzer = new SpanishAnalyzer2();

    BufferedReader in = null;
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
    	   	  
	  /*Query query = parser.parse(line);
      System.out.println("Searching for: " + query.toString(field));
            
      if (repeat > 0) {                           // repeat & time as benchmark
        Date start = new Date();
        for (int i = 0; i < repeat; i++) {
          searcher.search(query, 100);
        }
        Date end = new Date();
        System.out.println("Time: "+(end.getTime()-start.getTime())+"ms");
      }*/
      
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
    }
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
  public static void doPagingSearch(BufferedReader in, IndexSearcher searcher, Query query, 
                                     int hitsPerPage, boolean raw, boolean interactive) throws IOException {
	
    
	// Collect enough docs to show 5 pages
    TopDocs results = searcher.search(query, 5 * hitsPerPage);
    ScoreDoc[] hits = results.scoreDocs;
    
    int numTotalHits = (int)results.totalHits;
    System.out.println(numTotalHits + " total matching documents");

    int start = 0;
    int end = Math.min(numTotalHits, hitsPerPage);
        
    while (true) {
      if (end > hits.length) {
        System.out.println("Only results 1 - " + hits.length +" of " + numTotalHits + " total matching documents collected.");
        System.out.println("Collect more (y/n) ?");
        String line = in.readLine();
        if (line.length() == 0 || line.charAt(0) == 'n') {
          break;
        }

        hits = searcher.search(query, numTotalHits).scoreDocs;
      }
      
      end = Math.min(hits.length, start + hitsPerPage);
      
      for (int i = start; i < end; i++) {
        if (raw) {                              // output raw format
          System.out.println("doc="+hits[i].doc+" score="+hits[i].score);
          continue;
        }
        
        Document doc = searcher.doc(hits[i].doc);
        
        String path = doc.get("path");
        String modified = doc.get("modified");
        if (path != null) {
          System.out.println((i+1) + ". " + path);
          System.out.println("  "+modified);
          // explain the scoring function
          //System.out.println(searcher.explain(query, hits[i].doc));
        } else {
          System.out.println((i+1) + ". " + "No path for this document");
        }
                  
      }

      if (!interactive || end == 0) {
        break;
      }

      if (numTotalHits >= end) {
        boolean quit = false;
        while (true) {
          System.out.print("Press ");
          if (start - hitsPerPage >= 0) {
            System.out.print("(p)revious page, ");  
          }
          if (start + hitsPerPage < numTotalHits) {
            System.out.print("(n)ext page, ");
          }
          System.out.println("(q)uit or enter number to jump to a page.");
          
          String line = in.readLine();
          if (line.length() == 0 || line.charAt(0)=='q') {
            quit = true;
            break;
          }
          if (line.charAt(0) == 'p') {
            start = Math.max(0, start - hitsPerPage);
            break;
          } else if (line.charAt(0) == 'n') {
            if (start + hitsPerPage < numTotalHits) {
              start+=hitsPerPage;
            }
            break;
          } else {
            int page = Integer.parseInt(line);
            if ((page - 1) * hitsPerPage < numTotalHits) {
              start = (page - 1) * hitsPerPage;
              break;
            } else {
              System.out.println("No such page");
            }
          }
        }
        if (quit) break;
        end = Math.min(numTotalHits, start + hitsPerPage);
      }
    }
  }
}