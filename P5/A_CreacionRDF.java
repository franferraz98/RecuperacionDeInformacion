package IR.Practica5;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.VCARD;
import org.apache.jena.vocabulary.RDF;

/**
 * Ejemplo de como construir un modelo de Jena y añadir nuevos recursos 
 * mediante la clase Model
 */
public class A_CreacionRDF {
	
	/**
	 * muestra un modelo de jena de ejemplo por pantalla
	 */
	public static void main (String args[]) {
        Model model = A_CreacionRDF.generarEjemplo();
        // write the model in the standar output
        model.write(System.out); 
    }
	
	/**
	 * Genera un modelo de jena de ejemplo
	 */
	public static Model generarEjemplo(){
		// definiciones
        String personURI    = "http://somewhere/JohnSmith";
        String givenName    = "John";
        String familyName   = "Smith";
        String fullName     = givenName + " " + familyName;

        // crea un modelo vacio
        Model model = ModelFactory.createDefaultModel();

        // le a�ade las propiedades
        Resource johnSmith  = model.createResource(personURI)
             .addProperty(VCARD.FN, fullName)
             .addProperty(VCARD.N, 
                      model.createResource()
                           .addProperty(VCARD.Given, givenName)
                           .addProperty(VCARD.Family, familyName))
             .addProperty(RDF.type, FOAF.Person)
             .addProperty(FOAF.birthday, "07-31");
        
        String person1URI    = "http://somewhere/MichaelJordan";
        String person2URI    = "http://somewhere/KobeBryant";
        String fullName1 = "Michael Jordan";
        String fullName2 = "Kobe Bryant";
        
        Resource michaelJordan = model.createResource(person1URI)
        		.addProperty(VCARD.FN, fullName1)
        		.addProperty(RDF.type, FOAF.Person);
        Resource kobeBryant = model.createResource(person2URI)
        		.addProperty(VCARD.FN, fullName2)
        		.addProperty(RDF.type, FOAF.Person)
        		.addProperty(FOAF.knows, michaelJordan);
        return model;
	}
	
	
}
