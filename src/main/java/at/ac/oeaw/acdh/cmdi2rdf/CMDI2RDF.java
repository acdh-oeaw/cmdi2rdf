package at.ac.oeaw.acdh.cmdi2rdf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
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
        
        try {
            
            // loading policy one but X3MLGeneratorPolicy has to by loaded each time since it is probably NOT thread safe
            byte[] policyArr = Files.readAllBytes(Configuration.FILE_POLICY);
            
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            
            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Configuration.THREAD_POOL_SIZE);
            
            // walk recursively through all xml files in a given parent directory 
            Files.walk(Configuration.DIR_CMDI).filter(p -> p.toString().endsWith(".xml")).forEach(xmlPath -> { 
                
                // processing for each file is done in a thread
                executor.submit(() -> {
                
                    try {
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
                                    X3MLGeneratorPolicy.load(new ByteArrayInputStream(policyArr), X3MLGeneratorPolicy.createUUIDSource(-1))
                                );
                            
                            Path rdfPath = Configuration.DIR_RDF.resolve(xmlPath.getName(xmlPath.getNameCount() -2));
                            
                            synchronized(CMDI2RDF.class) {
                                if(!Files.exists(rdfPath))
                                    Files.createDirectory(rdfPath);
                            }
                            
                            rdfPath = rdfPath.resolve(xmlPath.getFileName().toString().replace(".xml", ".rdf"));
                            
                            rdf.write(Files.newOutputStream(rdfPath, StandardOpenOption.CREATE), Configuration.FORMAT_OUTPUT);
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
                    
                });
            });
            
            executor.shutdown();
        }
        catch (IOException ex) {
            
            log.error("", ex);
        
        }
        catch (ParserConfigurationException ex) {
            
            log.error("", ex);
        };
    }
    
    /**
     * @param profileID
     * @return the profile specific mapping as a byte array
     */
    private static synchronized byte[] getMapping(String profileID){
        
        // creates the profile specific mapping and stores it in the map, if it doesn't exist already
        return X3ML_MAPPING.computeIfAbsent(profileID, k -> {
            
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
        });
    }
}
