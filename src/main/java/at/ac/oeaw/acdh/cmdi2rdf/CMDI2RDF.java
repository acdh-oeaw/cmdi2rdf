package at.ac.oeaw.acdh.cmdi2rdf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.ximpleware.VTDException;

import at.ac.acdh.transformer.ProfileTransformer;
import at.ac.acdh.transformer.utils.XMLIOService;
import gr.forth.ics.isl.x3ml.X3MLEngine;
import gr.forth.ics.isl.x3ml.X3MLGeneratorPolicy;
import gr.forth.x3ml.X3ML;
import lombok.extern.log4j.Log4j;

/*
* @author Wolfgang Walter SAUER (wowasa) &lt;wolfgang.sauer@oeaw.ac.at&gt;
*/
@Log4j
public class CMDI2RDF {
    
    private static Pattern PROFILE_ID = Pattern.compile("p_\\d+");
    
    private static final HashMap<String,byte[]> X3ML_MAPPING = new HashMap<String, byte[]>();
    

    public static void main(String[] args){
        if(args.length == 0) {
            log.fatal("missing input file");
            System.exit(1);
        }
        
        Path xmlPath = Paths.get(args[0]);
        
        if(!Files.exists(xmlPath)) {
            log.fatal("input file " + xmlPath + " doesn't exist");
            System.exit(1);
        }

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            
            X3MLGeneratorPolicy generatorPolicy = X3MLGeneratorPolicy.load(Files.newInputStream(Configuration.FILE_POLICY, StandardOpenOption.READ), X3MLGeneratorPolicy.createUUIDSource(-1));

            // reading whole file content to a string since we have to perform a string replacement later
            String content = new String(Files.readAllBytes(xmlPath));
            
            // skipping files bigger than FILE_SIZE_LIMIT
            if(content.length() > Configuration.FILE_SIZE_LIMIT) {
                log.info("file " + xmlPath + " skipped since its size eceeded the limit of " + Configuration.FILE_SIZE_LIMIT + " bytes");
                return;
            }
            
            Matcher matcher = PROFILE_ID.matcher(content.substring(200));
            
            String profileID;
            
            // processed only if a profile ID can be determined from the first 200 bytes
            if(matcher.find()) {  

            
                profileID = matcher.group(0);
                
                content = content.replace("hdl:", "http://hdl.handle.net/");
               
                // creating engine with the profile specific mapping
                X3MLEngine engine = X3MLEngine.load(new ByteArrayInputStream(getMapping(profileID)));
                
                // back to a stream to parse by document builder
                InputStream in = new ByteArrayInputStream(content.getBytes());
                
                Document document = builder.parse(in);
                
                
                X3MLEngine.Output rdf = engine.execute(
                        document.getDocumentElement(), 
                        generatorPolicy
                    );
                
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                
                //rdf.write(Files.newOutputStream(rdfPath, StandardOpenOption.CREATE), Configuration.FORMAT_OUTPUT);  
                rdf.write(outputStream, Configuration.FORMAT_OUTPUT);
                
                Path collectionDir = xmlPath.getName(xmlPath.getNameCount() -2);
                
                String outputString = new String(outputStream.toByteArray()).replaceFirst("<http://default>","<https://parthenos.acdh-dev.oeaw.ac.at/source/" + collectionDir.toString() + ">");
                
                Path rdfPath = Configuration.DIR_RDF.resolve(collectionDir);
                
                if(!Files.exists(rdfPath))
                    Files.createDirectory(rdfPath);
                
                
                rdfPath = rdfPath.resolve(xmlPath.getFileName().toString().replace(".xml", ".rdf"));

                
                Files.write(rdfPath, outputString.getBytes(), StandardOpenOption.CREATE);
            }
            else {
                log.info("no profile ID for file " + xmlPath);
            }
        }
        catch (IOException ex) {
            
            log.error("", ex);
        
        }
        catch (SAXException ex) {
            
            log.error("can't parse file " + xmlPath);
        
        }
        catch (ParserConfigurationException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
    }


    
    /**
     * @param profileID
     * @return the profile specific mapping as a byte array
     */
    private static byte[] getMapping(String profileID){      
            
        Path mapping = Configuration.DIR_MAPPING.resolve(profileID + ".xml");
        
        try {
        
            if(Files.exists(mapping)) {
                return Files.readAllBytes(mapping);
            }
            else {
        
                X3ML x3ml;
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                    x3ml = new ProfileTransformer().transform(Configuration.FILE_MAPPING, 
                            "https://catalog.clarin.eu/ds/ComponentRegistry/rest/registry/1.x/profiles/clarin.eu:cr1:" + profileID + "/xsd", 
                            Configuration.CONDITIONS.get(profileID)==null?Configuration.CONDITION_DEFAULT:Configuration.CONDITIONS.get(profileID)
                        );
                    
                    new XMLIOService<X3ML>().marshal(x3ml, out); 

                
                Files.write(mapping, out.toByteArray(), StandardOpenOption.CREATE);
                
                return out.toByteArray();
            }
        }

        catch (VTDException|JAXBException | IOException ex) {
            
            log.error("can't create mapping file for profile " + profileID, ex); 

        }
        
        return new byte[0];

    }
}
