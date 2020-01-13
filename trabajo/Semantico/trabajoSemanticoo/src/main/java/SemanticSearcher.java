import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class SemanticSearcher {

	public static void main(String[] args) {
		//Manejar argumentos
		String usage = "java SemanticSearcher"
                + " -rdf <rdfPath> -infoNeeds <infoNeedsFile> -output <resultsFile>\n\n";
		
		String rdfPath = null;
		String infoNeedsFile = null;
	    String resultsFile = null;
	    
	    for(int i=0;i<args.length;i++) {
	        if ("-rdf".equals(args[i])) {
	        	rdfPath = args[i+1];
	        	i++;
	        } else if ("-infoNeeds".equals(args[i])) {
	        	infoNeedsFile = args[i+1];
		        i++;
	        }
	        else if ("-output".equals(args[i])) {
	        	resultsFile = args[i+1];
		        i++;
	        }
	    }
	    
	    if (rdfPath == null || infoNeedsFile == null || resultsFile == null) {
	        System.err.println("Usage: " + usage);
	        System.exit(1);
	    }
	    
	    /*if (docDir.canRead()) {
			//Si es un directorio
			if (docDir.isDirectory()) {
			  String[] files = file.list();	//Recuperamos lista de los ficheros
			  // an IO error could occur
			  if (files != null) {
			    for (int i = 0; i < files.length; i++) {
			  	//Recursividad fichero a fichero
			      indexDocs(writer, new File(file, files[i]));
			    }
			  }
			//Si es archivo, hay que indexarlo
			}else {
			
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
	    }*/
		
	}

}
