package IR.Practica5;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.FileManager;

/**
 * Ejemplo de lectura de un modelo RDF de un fichero de texto 
 * y como acceder por API a los elementos que contiene
 */
public class D_AccesoRDF {

	public static void main(String args[]) {

		// cargamos el fichero deseado
		Model model = FileManager.get().loadModel("card.rdf");

		// obtenemos todos los statements del modelo
		StmtIterator it = model.listStatements();

		// mostramos todas las tripletas cuyo objeto es un literal
		while (it.hasNext()) {
			Statement st = it.next();

			if (st.getObject().isLiteral()) {
				System.out.println(st.getSubject().getURI() + " - "
						+ st.getPredicate().getURI() + " - "
						+ st.getLiteral().toString());
			}
		}

		System.out.println("----------------------------------------");

		// mostramos los valores de todas las propiedades de un recurso
		// determinado
		Resource res = model
				.getResource("http://dig.csail.mit.edu/2008/webdav/timbl/foaf.rdf");
		it = res.listProperties();
		while (it.hasNext()) {
			Statement st = it.next();

			if (st.getObject().isLiteral()) {
				System.out.println(st.getSubject().getURI() + " - "
						+ st.getPredicate().getURI() + " - "
						+ st.getLiteral().toString());
			} else {
				System.out.println(st.getSubject().getURI() + " - "
						+ st.getPredicate().getURI() + " - "
						+ st.getResource().getURI());
			}
		}

		System.out.println("----------------------------------------");

		// mostramos todos los recursos que contienen una propiedad determinada
		Property prop = model
				.getProperty("http://purl.org/dc/elements/1.1/title");
		ResIterator ri = model.listSubjectsWithProperty(prop);
		while (ri.hasNext()) {
			Resource r = ri.next();
			System.out.println(r.getURI());
		}

		System.out.println("----------------------------------------");

		// mostramos todos los recursos que contienen una propiedad determinada
		// forma alternativa que usa un filtro sobre los statements a recuperar
		it = model.listStatements(null, prop, (RDFNode) null);
		while (it.hasNext()) {
			Statement st = it.next();

			if (st.getObject().isLiteral()) {
				System.out.println(st.getSubject().getURI() + " - "
						+ st.getPredicate().getURI() + " - "
						+ st.getLiteral().toString());
			}
		}
		
		System.out.println("----------------------------------------");
		// consulta que devuelve todos los literales que contienen el texto 
		// Berners-Lee
		
		// cargamos el fichero deseado
		model = FileManager.get().loadModel("card.rdf");

		// obtenemos todos los statements del modelo
		it = model.listStatements();

		// mostramos todas las tripletas cuyo objeto es un literal
		while (it.hasNext()) {
			Statement st = it.next();

			if (st.getObject().isLiteral()) {
				if(st.getLiteral().toString().indexOf("Berners-Lee")!=-1) {
					System.out.println(st.getSubject().getURI() + " - "
							+ st.getPredicate().getURI() + " - "
							+ st.getLiteral().toString());
				}
			}
		}
		System.out.println("----------------------------------------");
		// consulta que devuelve todos los documentos cuyo creador es
		// Tim Berners-Lee
		
		// cargamos el fichero deseado
		model = FileManager.get().loadModel("card.rdf");
		
		prop = model
				.getProperty("http://purl.org/dc/elements/1.1/creator");
		ri = model.listSubjectsWithProperty(prop);
		while (ri.hasNext()) {
			Resource r = ri.next();
			Statement a = r.getProperty(prop);
			String s = a.getObject().toString();
			if(s.indexOf("Berners-Lee")!=-1) {
				a = r.getProperty(model.getProperty("http://purl.org/dc/elements/1.1/title"));
				System.out.println(a.getObject().toString());
			}
			
		}

		System.out.println("----------------------------------------");
		// creación de un modelo que solo tiene los documentos creados
		// por Tim Berners-Lee
		
		Model model2 = ModelFactory.createDefaultModel();
		
		prop = model
				.getProperty("http://purl.org/dc/elements/1.1/creator");
		ri = model.listSubjectsWithProperty(prop);
		while (ri.hasNext()) {
			Resource r = ri.next();
			Statement a = r.getProperty(prop);
			String s = a.getObject().toString();
			if(s.indexOf("Berners-Lee")!=-1) {
				model2.add(a);
			}
		}
		ri = model2.listSubjects();
		while (ri.hasNext()) {
			Resource r = ri.next();
			System.out.println(r);
		}
		

		
	}

}
