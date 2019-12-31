package IR.Practica5;




import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileManager;

/**
 * Ejemplo de lectura de un modelo RDF de un fichero de texto 
 * y como acceder con SPARQL a los elementos que contiene
 */
public class E_AccesoSPARQL {

	public static void main(String args[]) {
		
		// cargamos el fichero deseado
		Model model = FileManager.get().loadModel("card.rdf");
		
		System.out.println("--------------------CONSULTA TIPO QUERY--------------------");
		//definimos la consulta (tipo query)
		String queryString = "Select ?x ?y ?z WHERE  {?x ?y ?z }" ;
		
		//ejecutamos la consulta y obtenemos los resultados
		  Query query = QueryFactory.create(queryString) ;
		  QueryExecution qexec = QueryExecutionFactory.create(query, model) ;
		  try {
		    ResultSet results = qexec.execSelect() ;
		    for ( ; results.hasNext() ; )
		    {
		      QuerySolution soln = results.nextSolution() ;
		      Resource x = soln.getResource("x");
		      Resource y = soln.getResource("y");
		      RDFNode z = soln.get("z") ;  
		      if (z.isLiteral()) {
					System.out.println(x.getURI() + " - "
							+ y.getURI() + " - "
							+ z.toString());
				} else {
					System.out.println(x.getURI() + " - "
							+ y.getURI() + " - "
							+ z.asResource().getURI());
				}
		    }
		  } finally { qexec.close() ; }
		
		System.out.println("--------------------CONSULTA TIPO DESCRIBE--------------------");

		//definimos la consulta (tipo describe)
		queryString = "Describe <http://www.w3.org/People/Berners-Lee/card#i>" ;
		query = QueryFactory.create(queryString) ;
		qexec = QueryExecutionFactory.create(query, model) ;
		Model resultModel = qexec.execDescribe() ;
		qexec.close() ;
		resultModel.write(System.out);
		
		System.out.println("--------------------CONSULTA TIPO ASK--------------------");
		//definimos la consulta (tipo ask)
		queryString = "ask {<http://www.w3.org/People/Berners-Lee/card#i> ?x ?y}" ;
		query = QueryFactory.create(queryString) ;
		qexec = QueryExecutionFactory.create(query, model) ;
		System.out.println( qexec.execAsk()) ;
		qexec.close() ;
		
		System.out.println("--------------------CONSULTA TIPO CONSTRUCT--------------------");
		//definimos la consulta (tipo construct)
		queryString = "construct {?x <http://miuri/inverseSameAs> ?y} where {?y <http://www.w3.org/2002/07/owl#sameAs> ?x}" ;
		query = QueryFactory.create(queryString) ;
		qexec = QueryExecutionFactory.create(query, model) ;
		resultModel = qexec.execConstruct() ;
		qexec.close() ;
		resultModel.write(System.out);
		
		System.out.println("--------------------EJERCICIO 1--------------------");
		//Crea una consulta que devuelva todos los literales que contengan el texto "Berners-Lee"
		//definimos la consulta (tipo query)
		String queryStringg = "Select ?x ?y ?z WHERE  {?x ?y ?z. FILTER regex(str(?z), \"Berners-Lee\") }" ;
		
		//ejecutamos la consulta y obtenemos los resultados
		  Query queryy = QueryFactory.create(queryStringg) ;
		  QueryExecution qexecc = QueryExecutionFactory.create(queryy, model) ;
		  try {
		    ResultSet results = qexecc.execSelect() ;
		    for ( ; results.hasNext() ; )
		    {
		      QuerySolution soln = results.nextSolution() ;
		      Resource x = soln.getResource("x");
		      Resource y = soln.getResource("y");
		      RDFNode z = soln.get("z") ;  
		      if (z.isLiteral()) {
					System.out.println(x.getURI() + " - "
							+ y.getURI() + " - "
							+ z.toString());
				} else {
					System.out.println(x.getURI() + " - "
							+ y.getURI() + " - "
							+ z.asResource().getURI());
				}
		    }
		  } finally { qexec.close() ; }
		
		  
		System.out.println("--------------------EJERCICIO 2--------------------");
		//Crea una consulta que devuelva el titulo de todos los documentos creados por Tim Berners-Lee
		
		//definimos la consulta (tipo query)
		String queryStringgg = "PREFIX dc: <http://purl.org/dc/elements/1.1/>"
							 + "PREFIX card: <http://www.w3.org/People/Berners-Lee/card#>"
							 + "SELECT ?title WHERE {"
							 		+ "?x dc:creator card:i."
							 		+ "?x dc:title ?title }" ;
		
		//ejecutamos la consulta y obtenemos los resultados
		  Query queryyy = QueryFactory.create(queryStringgg) ;
		  QueryExecution qexeccc = QueryExecutionFactory.create(queryyy, model) ;
		  try {
		    ResultSet results = qexeccc.execSelect() ;
		    for ( ; results.hasNext() ; )
		    {
		      QuerySolution soln = results.nextSolution() ;
		      RDFNode title = soln.get("title") ;
		      if (title.isLiteral()) {
					System.out.println(title.toString());
				} else {
					System.out.println(title.asResource().getURI());
				}
		    }
		  } finally { qexec.close() ; }
		  
		System.out.println("--------------------EJERCICIO 3--------------------");
		//Crea un nuevo modelo que solo contenga la informacion de los documentos creados por Tim Berners-Lee
		  
		//definimos la consulta (tipo describe)
		String queryStringggg = "PREFIX dc: <http://purl.org/dc/elements/1.1/>"
							  + "PREFIX card: <http://www.w3.org/People/Berners-Lee/card#>"
							  + "DESCRIBE ?x WHERE {"
							 		   + "?x dc:creator card:i }" ;
		query = QueryFactory.create(queryStringggg) ;
		qexec = QueryExecutionFactory.create(query, model) ;
		Model resultModell = qexec.execDescribe() ;
		qexec.close() ;
		resultModell.write(System.out);
	}
	
}
