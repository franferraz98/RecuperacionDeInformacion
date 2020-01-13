import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.FileManager;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;

public class SemanticSearcher {

	public static void main(String[] args) {
		//Manejar argumentos
		String usage = "java SemanticSearcher"
                + " -rdf <rdfPath> -infoNeeds <infoNeedsFile> -output <resultsFile>\n\n";
		
		String rdfPath = null;
		String infoNeedsFile = null;
	    String resultsFile = null;
	    
	    /* Valores para pruebas */
	    
	    rdfPath = "../../modelo_equipo16.rdf"; // Fichero RDF donde se almacena el grafo de la coleccion
		infoNeedsFile = "../../necesidadesSPARQL.txt"; // Fichero de las necesidades de información
		resultsFile = "../../results.txt"; // Fichero donde se almacenan los resultados
	    
	    int outputLimit = 50;
	    
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
	    
	    /* PrintWriter para escribir en el fichero de resultados */
		PrintWriter out = null;
		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter(resultsFile)));
		} catch (IOException e) {
			e.printStackTrace();
		}

		/* Cargamos el fichero RDF de la coleccion */
		Model model = FileManager.get().loadModel(rdfPath);
		
		/* Creamos un modelo de inferencia */
		InfModel inf = ModelFactory.createRDFSModel(model);
		
		/* Necesidades de informacion almacenadas en un hashmap y su iterador */
		HashMap<String, String> needs = getInfoNeeds(infoNeedsFile);
		Iterator<Map.Entry<String, String>> it = needs.entrySet().iterator();
		
		while (it.hasNext()) {
			Map.Entry<String, String> pair = it.next();
			System.out.println("Buscando: " + pair.getKey() + " -" + pair.getValue()); 

			/* Consulta SPARQL */
			Query query = QueryFactory.create(pair.getValue());
		    System.out.println(query);

			QueryExecution qexec = QueryExecutionFactory.create(query, inf);
		    ResultSet results = qexec.execSelect();
		    /* Comentar la siguiente linea para escribir en el fichero de salida */
		    
		    //ResultSetFormatter.out(System.out, results, query);
            for (int i=0; i<outputLimit && results.hasNext(); i++) {
                QuerySolution qsol = results.nextSolution();
                Resource x = qsol.getResource("doc");
                String value = x.getURI();
                value = value.substring(value.lastIndexOf("/") + 1);
                value = "oai_zaguan.unizar.es_" + value + ".xml"; 
    			
    			out.println(pair.getKey() + "\t" + value);
            }
		    qexec.close();
		}
		out.close();
	}
	
	/**
	 * Devuelve un HashMap que contiene las necesidades de informacion 
	 * donde el identificador de esta es la clave y el texto es el valor
	 * @param file El fichero de necesidades
	 * @return Devuelve un hashmap con las necesidades de informacion
	 */
	private static HashMap<String, String> getInfoNeeds(String file) {
		// HasMap utilizando la implementacion enlazada para mantener el orden
		// de las necesidades como en el fichero file
		HashMap<String, String> infoNeeds = new LinkedHashMap<String, String>();
		Scanner s = null;
		try {
			s = new Scanner(new File(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		while (s.hasNextLine()) {
			infoNeeds.put(s.next(), s.nextLine());
		}
		s.close();

		return infoNeeds;
	}
		
}
