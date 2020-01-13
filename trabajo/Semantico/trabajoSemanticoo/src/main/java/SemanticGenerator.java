import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb2.TDB2Factory;

public class SemanticGenerator {

	public static void main(String[] args) {
		//Manejar argumentos
		String usage = "java SemanticGenerator"
                + " -rdf <rdfPath> -skos <skosPath> -owl <owlPath> -docs <docsPath>\n\n";
		
		String rdfPath = null;
		String skosPath = null;
	    String owlPath = null;
	    String docsPath = null;
	    
	    //Obtener argumentos
	    for(int i=0;i<args.length;i++) {
	        if ("-rdf".equals(args[i])) {
	        	rdfPath = args[i+1];
	        	i++;
	        } else if ("-skos".equals(args[i])) {
	        	skosPath = args[i+1];
		        i++;
	        }
	        else if ("-owl".equals(args[i])) {
	        	owlPath = args[i+1];
		        i++;
	        }
	        else if ("-docs".equals(args[i])) {
	        	docsPath = args[i+1];
		        i++;
	        }
	    }
	    
	    //Comprobar argumentos
	    if (rdfPath == null || skosPath == null || owlPath == null || docsPath == null) {
	        System.err.println("Usage: " + usage);
	        System.exit(1);
	    }    
	    
	    //Comprobar que existe el directorio y que se puede leer
	    final File docDir = new File(docsPath);
	    if (!docDir.exists() || !docDir.canRead()) {
	      System.out.println("Document directory '" +docDir.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
	      System.exit(1);
	    }
	    
	    try {
	    	final FileOutputStream coleccionRDF = new FileOutputStream(new File(rdfPath));
	    	
	    	System.out.println("Creando colección RDF ...");
	    	
	    	Model skos = RDFDataMgr.loadModel(skosPath);
	    	
	    	Model modelColeccionRDF = obtenerGrafoColeccion(skos, docDir);
	    	modelColeccionRDF.write(System.out); 
	    	
	    } catch (IOException e) {
	    	System.out.println("Encontrado error "+ e.getClass() + "con mensaje:\n"+ e.getMessage());
	    }		
	}
	
	public static Model obtenerGrafoColeccion(Model skos, File file) throws IOException{
		Model model = ModelFactory.createDefaultModel();
		model.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		model.setNsPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
		model.setNsPrefix("dc", "http://purl.org/dc/elements/1.1/");
		model.setNsPrefix("base", "http://www.trabajos.fake/trabajos#");
		//model.setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/");
		
		if(file.canRead()) {
			if(file.isDirectory()) {
				String[] xmls = file.list();
				
				if(xmls!=null) {
					for(int i=0; i<xmls.length; i++) {
						Model xmlModel = sacaXmlModel(skos, file+"/"+xmls[i]);
						model.add(xmlModel);
					}
				}
			}
		}
		
		return model;
	}
	
	public static Model sacaXmlModel(Model skos, String xml) {
		
	}
}
