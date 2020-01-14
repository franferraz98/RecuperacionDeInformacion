import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb2.TDB2Factory;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
	    	modelColeccionRDF.write(coleccionRDF);
	    	
	    	System.out.println("... Terminado");
	    	
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
	
	public static Model sacaXmlModel(Model skos, String xml) throws IOException{
		String trabajoPath = "http://www.trabajos.fake/trabajos#";
		String skosPath = "http://www.w3.org/2004/02/skos/core#";
		
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		org.w3c.dom.Document document = null;
		
		try {
			builder = builderFactory.newDocumentBuilder();
	        document = builder.parse(xml);		
		} catch(SAXException s) {
			System.out.println(s.getMessage());
		} catch(ParserConfigurationException p) {
			System.out.println(p.getMessage());
		}
		
		String[] creador = null;
        String titulo=null, id=null, publisher=null, 
        	   descripcion=null, idioma=null, fecha=null, type=null;
        
        
        try {
        	//Creador
        	NodeList creators = document.getElementsByTagName("dc:creator");
        	creador = new String[creators.getLength()];
        	for(int i=0; i<creators.getLength(); i++) {
				 creador[i] = creators.item(i).getTextContent();
        	}
        	
        	//titulo
        	titulo = document.getElementsByTagName("dc:title").item(0).getTextContent();
        	
        	//id
        	NodeList identifier = document.getElementsByTagName("dc:identifier");
        	id = "oai_zaguan.unizar.es_"+identifier.item(0).getTextContent().substring(31)+".xml";
        	//System.out.println(id);
        	
        	//publisher
        	if(document.getElementsByTagName("dc:publisher").item(0) != null)
        		publisher = document.getElementsByTagName("dc:publisher").item(0).getTextContent();
        	
        	//System.out.println("bien");
        	//descripcion
        	descripcion = document.getElementsByTagName("dc:description").item(0).getTextContent();
        	
        	//idioma
        	if(document.getElementsByTagName("dc:language").item(0) != null)
        		idioma = document.getElementsByTagName("dc:language").item(0).getTextContent();
        	
        	//fecha
        	fecha = document.getElementsByTagName("dc:date").item(0).getTextContent().trim();
        	
			if(fecha.length()>4) {
				fecha = fecha.substring(0, fecha.indexOf("T")).replace("-", "");
			}
			else {
				for(int i=fecha.length(); i<8; i++) {
						fecha+="0";
				}
			}
			
			String anyo = fecha.substring(0, 4);
			String mes = fecha.substring(4, 6);
			String dia = fecha.substring(6, 8);
			if(mes.equals("00")) {
				mes = "01";
			}
			if(dia.equals("00")) {
				dia = "01";
			}
			
			fecha = anyo+"-"+mes+"-"+dia;
        	
        	//TFG o TFM
			NodeList typeList = document.getElementsByTagName("dc:type");
			
			String typeAux = "";
			type = "TFM";
			for(int i=0; i<typeList.getLength(); i++) {
				typeAux = typeList.item(i).getTextContent();
				if(typeAux.contains("bachelorThesis")) {
					type = "TFG";
				}
			}
        	
        } catch (NullPointerException e) {
        	System.out.println(e.getMessage());
        	System.exit(1);
        }
        
        //Generar grafo rdf
        Model model = ModelFactory.createDefaultModel();
        
        String uriDoc = "http://www.trabajos.fake/trabajos#Documento/"+id.substring(21);
        Resource doc = model.createResource(uriDoc)
        		.addProperty(RDF.type, trabajoPath + type)
        		.addProperty(model.createProperty(trabajoPath + "titulo"), titulo)
        		.addProperty(model.createProperty(trabajoPath + "identificador"), id)
        		.addProperty(model.createProperty(trabajoPath + "descripcion"), descripcion)
        		.addProperty(model.createProperty(trabajoPath + "fecha"), fecha);
        		
        
        if(publisher!=null)
        	doc.addProperty(model.createProperty(trabajoPath + "publisher"), publisher);
        
        if(idioma!=null)
        	doc.addProperty(model.createProperty(trabajoPath + "idioma"), idioma);
        
        //Añadir autores
        for(int i=0; i<creador.length; i++) {
        	String uri = trabajoPath + "Persona/"+creador[i].replaceAll("[\\s,]", "");
        	Resource persona = model.createResource(uri)
        			.addProperty(RDF.type, trabajoPath + "Persona")
        			.addProperty(model.createProperty(trabajoPath + "nombrePersona"), creador[i]);
        	
        	doc.addProperty(model.createProperty(trabajoPath + "creador"), persona);
        }
        
        //SKOS
        ResIterator it = skos.listSubjectsWithProperty(model.createProperty(skosPath + "definition"));
        String tituloSkos = limpia(titulo);
        String descripcionSkos = limpia(descripcion);
        while(it.hasNext()) {
        	Resource res = it.next();
        	Statement st = res.getProperty(model.createProperty(skosPath + "prefLabel"));
        	
        	String subject = "";
        	if(st.getObject().isLiteral()) {
        		subject = st.getLiteral().toString();
        	}
        	else {
        		subject = st.getResource().getURI();
        	}
        	subject = subject.substring(0, subject.length() - 3);
        	
        	if(tituloSkos.contains(subject) || descripcionSkos.contains(subject)) {
        		doc.addProperty(model.createProperty(trabajoPath + "subject"), res);
        	}
        	
        	StmtIterator altLabels = res.listProperties(model.createProperty(skosPath + "altLabel"));
    		while(altLabels.hasNext()) {
        		Statement s = altLabels.nextStatement();
        		subject = "";
        		if(s.getObject().isLiteral()) {
        			subject = s.getLiteral().toString();
        		}
        		else {
        			subject = s.getResource().getURI();
        		}
        		
        		if(tituloSkos.contains(subject) || descripcionSkos.contains(subject)) {
            		doc.addProperty(model.createProperty(trabajoPath + "subject"), res);
            	}
        	}
        }
        
        return model;
	}
	
	static public String limpia(String cadena) {
		cadena = cadena.toLowerCase();
		cadena = cadena.replaceAll("á", "a");
		cadena = cadena.replaceAll("é", "e");
		cadena = cadena.replaceAll("í", "i");
		cadena = cadena.replaceAll("ó", "u");
		cadena = cadena.replaceAll("ú", "o");
		
		return cadena;
	}
}
