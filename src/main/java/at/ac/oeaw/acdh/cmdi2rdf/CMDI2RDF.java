package at.ac.oeaw.acdh.cmdi2rdf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
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
        
        try {
            X3MLGeneratorPolicy policy = X3MLGeneratorPolicy.load(Files.newInputStream(Configuration.FILE_POLICY, StandardOpenOption.CREATE_NEW), X3MLGeneratorPolicy.createUUIDSource(-1));

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            
            Files.walk(Configuration.DIR_CMDI).filter(p -> p.toString().endsWith(".xml")).forEach(xmlPath -> {

                
                try {
                    String content = new String(Files.readAllBytes(xmlPath));
                    
                    if(content.length() > Configuration.FILE_SIZE_LIMIT) {
                        log.info("file " + xmlPath + " skipped since its size eceeded the limit of " + Configuration.FILE_SIZE_LIMIT + " bytes");
                        return;
                    }
                    
                    Matcher matcher = PROFILE_ID.matcher(content.substring(200));
                    
                    String profileID;
                    
                    if(matcher.find()) {

                    
                        profileID = matcher.group(0);
                        
                        content = content.replace("hdl:", "http://hdl.handle.net");
                        
                        InputStream in = new ByteArrayInputStream(content.getBytes());
                        
                        X3MLEngine engine = X3MLEngine.load(new ByteArrayInputStream(getMapping(profileID)));
                        
    
                        Document document = builder.parse(in);
                        
                        X3MLEngine.Output rdf = engine.execute(document.getDocumentElement(), policy);
                        
                        Path rdfPath = Configuration.DIR_RDF.resolve(xmlPath.getName(xmlPath.getNameCount() -2));
                        
                        if(!Files.exists(rdfPath))
                            Files.createDirectory(rdfPath);
                        
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
        }
        catch (IOException ex) {
            
            log.error("", ex);
        
        }
        catch (ParserConfigurationException ex) {
            
            log.error("", ex);
        };

    }
    
    private static byte[] getMapping(String profileID){
        
        return X3ML_MAPPING.computeIfAbsent(profileID, k -> {
            
            X3ML x3ml;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                x3ml = new ProfileTransformer().transform(Configuration.FILE_MAPPING, 
                        "https://catalog.clarin.eu/ds/ComponentRegistry/rest/registry/1.x/profiles/clarin.eu:cr1:" + profileID + "/xsd", 
                        Configuration.CONDITIONS.get(profileID)==null?Configuration.CONDITION_DEFAULT:Configuration.CONDITIONS.get(profileID)
                    );
                
                new XMLIOService<X3ML>().marshal(x3ml, out); 
            }
            catch (VTDException|JAXBException ex) {
                
                log.error("can't create mapping file for profile " + profileID, ex);
            
            }
            
            return out.toByteArray();
        });
    }

}
