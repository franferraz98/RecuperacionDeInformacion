package org.apache.lucene.demo;

/*
 * Alumnos: Francisco Ferraz (737312) y Guillermo Cruz (682433)
 * Nombre fichero: IndexFiles.java
 * Fecha: 08 de noviembre de 2019
 * Descripci�n: Archivo que crea un �ndice de documentos del repositorio digital Zagu�n
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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/** Index all text files under a directory.
 * <p>
 */
public class IndexFiles {

  private IndexFiles() {}

  /** Index all text files under a directory. 
 * @throws SAXException 
 * @throws ParserConfigurationException */
  public static void main(String[] args) throws ParserConfigurationException, SAXException {
    String usage = "java org.apache.lucene.demo.IndexFiles"
                 + " -index <indexPath> -docs <docsPath>\n\n"
                 + "This indexes the documents in DOCS_PATH, creating a Lucene index"
                 + "in INDEX_PATH that can be searched with SearchFiles";
    String indexPath = "index";
    String docsPath = null;
    for(int i=0;i<args.length;i++) {
      if ("-index".equals(args[i])) {
        indexPath = args[i+1];
        i++;
      } else if ("-docs".equals(args[i])) {
        docsPath = args[i+1];
        i++;
      }
    }

    if (docsPath == null) {
      System.err.println("Usage: " + usage);
      System.exit(1);
    }

    final File docDir = new File(docsPath);
    if (!docDir.exists() || !docDir.canRead()) {
      System.out.println("Document directory '" +docDir.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
      System.exit(1);
    }

    Date start = new Date();
    try {
      System.out.println("Indexing to directory '" + indexPath + "'...");

      Directory dir = FSDirectory.open(Paths.get(indexPath));
      Analyzer analyzer = new SpanishAnalyzer2(SpanishAnalyzer2.createStopSet3());
      IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

      // Create a new index in the directory, removing any
      // previously indexed documents:
      iwc.setOpenMode(OpenMode.CREATE);

      IndexWriter writer = new IndexWriter(dir, iwc);
      indexDocs(writer, docDir);

      writer.close();

      Date end = new Date();
      System.out.println(end.getTime() - start.getTime() + " total milliseconds");

    } catch (IOException e) {
      System.out.println(" caught a " + e.getClass() +
       "\n with message: " + e.getMessage());
    }
  }
  
  /** Introduce en el �ndice los elementos del xml contenidos en la etiqueta <campo>
   *  con el tipo <fieldType>
   */
  static void introducirCampo(org.w3c.dom.Document document, String campo, String fieldType, Document doc) {
	  NodeList list = document.getElementsByTagName("dc:"+campo);
	 
	 if(list != null && list.getLength() > 0) {
		 switch(fieldType){
		  case("TextField"):
			  TextField infoT = null;
			  for(int i=0; i<list.getLength(); i++) {
					 infoT = new TextField(campo, list.item(i).getTextContent(), Field.Store.YES);
					 doc.add(infoT);
			  }
			  break;
			  
		  case("StringField"):
			  if(campo.equals("identifier")) {
				  String id = list.item(0).getTextContent().substring(31);
				  StringField infoId = new StringField(campo, id, Field.Store.YES);
				  doc.add(infoId);
			  }
			  else if(campo.equals("type")) {
				  StringField infoType = null;
				  String[] s;
				  for(int i=0; i<list.getLength(); i++) {
					  	 s = list.item(i).getTextContent().split("/");
						 infoType = new StringField(campo, s[s.length-1].toLowerCase(), Field.Store.YES);
						 doc.add(infoType);
				  }
			  }
			  else {
				  StringField infoS = null;
				  for(int i=0; i<list.getLength(); i++) {
						 infoS = new StringField(campo, list.item(i).getTextContent(), Field.Store.YES);
						 doc.add(infoS);
				  }
			  }
			  
			  break;
			  
		  case("DoublePoint"):
			  String date = list.item(0).getTextContent().trim();
			  if(date.length()>4) {
				  date = date.substring(0, date.indexOf("T")).replace("-", "");
			  }
			  else {
				  for(int i=date.length(); i<8; i++) {
					  date+="0";
				  }
			  }
			  
			  //Se guarda la fecha como un DoublePoint para poder hacer comparaciones
			  DoublePoint infoDate = new DoublePoint(campo, Double.parseDouble(date));
			  doc.add(infoDate);
			  break;
		  default:
			  break;
		 }
	 }
  }
  
  /**
   * Indexes the given file using the given writer, or if a directory is given,
   * recurses over files and directories found under the given directory.
   *
   * NOTE: This method indexes one document per input file.  This is slow.  For good
   * throughput, put multiple documents into your input file(s).  An example of this is
   * in the benchmark module, which can create "line doc" files, one document per line,
   * using the
   * <a href="../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
   * >WriteLineDocTask</a>.
   *
   * @param writer Writer to the index where the given file/dir info will be stored
   * @param file The file to index, or the directory to recurse into to find files to index
   * @throws IOException If there is a low-level I/O error
 * @throws ParserConfigurationException 
 * @throws SAXException 
   */
  static void indexDocs(IndexWriter writer, File file)
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
            indexDocs(writer, new File(file, files[i]));
          }
        }
      //Si es archivo, hay que indexarlo
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

          // make a new, empty document
          Document doc = new Document();

          //Guardamos la ruta del fichero
          // Add the path of the file as a field named "path".  Use a
          // field that is indexed (i.e. searchable), but don't tokenize
          // the field into separate words and don't index term frequency
          // or positional information:
          Field pathField = new StringField("path", file.getPath(), Field.Store.YES);
          doc.add(pathField);

          // Add the last modified date of the file a field named "modified".
          // Use a LongField that is indexed (i.e. efficiently filterable with
          // NumericRangeFilter).  This indexes to milli-second resolution, which
          // is often too fine.  You could instead create a number based on
          // year/month/day/hour/minutes/seconds, down the resolution you require.
          // For example the long value 2011021714 would mean
          // February 17, 2011, 2-3 PM.
          doc.add(new LongPoint("modified", file.lastModified()));

          // Add the contents of the file to a field named "contents".  Specify a Reader,
          // so that the text of the file is tokenized and indexed, but not stored.
          // Note that FileReader expects the file to be in UTF-8 encoding.
          // If that's not the case searching for special characters will fail.
          doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(fis, "UTF-8"))));
          
          //INDICES SEPARADOS PARA CADA CAMPO DE XML DIFERENTE (creator, title, identifier ...),
          DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
          DocumentBuilder builder = builderFactory.newDocumentBuilder();
          org.w3c.dom.Document document = builder.parse(file.getPath());
           
          introducirCampo(document, "creator", "TextField", doc);
          introducirCampo(document, "title", "TextField", doc);
          introducirCampo(document, "identifier", "StringField", doc);
          introducirCampo(document, "subject", "TextField", doc);
          introducirCampo(document, "publisher", "TextField", doc);
          introducirCampo(document, "description", "TextField", doc);
          introducirCampo(document, "language", "StringField", doc);
          introducirCampo(document, "date", "DoublePoint", doc);
          introducirCampo(document, "type", "StringField", doc);     

          // New index, so we just add the document (no old document can be there):
          System.out.println("adding " + file);
          writer.addDocument(doc);

        } finally {
          fis.close();
        }
      }
    }
  }
}
