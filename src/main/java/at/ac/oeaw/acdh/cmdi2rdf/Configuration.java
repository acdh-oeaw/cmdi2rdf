package at.ac.oeaw.acdh.cmdi2rdf;
/*
* @author Wolfgang Walter SAUER (wowasa) &lt;wolfgang.sauer@oeaw.ac.at&gt;
*/

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import lombok.extern.log4j.Log4j;

@Log4j
public class Configuration {
    public static final String FORMAT_OUTPUT;
    public static final List<String> CONDITION_DEFAULT;
    public static final Map<String,List<String>> CONDITIONS;
    public static final Path DIR_CMDI;
    public static final Path DIR_RDF;
    public static final String FILE_MAPPING;
    public static final Path FILE_POLICY;
    public static final int FILE_SIZE_LIMIT;
    public static final int THREAD_POOL_SIZE;
    
    static {
        Properties properties = new Properties();
        try {
            if(System.getProperty("cmdi2rdf.properties") != null) {
                Path propertiesPath = Paths.get(System.getProperty("cmdi2rdf.properties"));
                properties.load(Files.newInputStream(propertiesPath));
            }
            else {
                properties.load(ClassLoader.getSystemResourceAsStream("cmdi2rdf.properties"));
            }
        }
        catch (IOException ex) {
            // TODO Auto-generated catch block
            log.error("can't load cmdi2rdf.properties by classloader!", ex);
        }
        FORMAT_OUTPUT = properties.getProperty("FORMAT_OUTPUT", "text/turtle");
        
        CONDITION_DEFAULT = Arrays.asList(properties.getProperty("CONDITION_DEFAULT", "creator-actor,dataset").replace(" ", "").split(","));
        
        CONDITIONS = new HashMap<String,List<String>>();
        
        if(properties.getProperty("CONDITIONS") != null) {
            for(String conditionset : properties.getProperty("CONDITIONS").split(";")) {
                if(conditionset.split(":").length == 2) {
                    List<String> condition = Arrays.asList(conditionset.split(":")[0].split(","));
                    
                    for(String profileID : conditionset.split(":")[1].split(",")) {
                        CONDITIONS.put(profileID, condition);
                    }
                    
                }
            }
            
        }
        DIR_CMDI = Paths.get(properties.getProperty("DIR_CMDI"));
        DIR_RDF = Paths.get(properties.getProperty("DIR_RDF"));
        FILE_MAPPING = properties.getProperty("FILE_MAPPING");
        FILE_POLICY = Paths.get(properties.getProperty("FILE_POLICY"));
        FILE_SIZE_LIMIT = Integer.valueOf(properties.getProperty("FILE_SIZE_LIMIT", "10000000"));
        THREAD_POOL_SIZE = Integer.valueOf(properties.getProperty("THREAD_POOL_SIZE", "20"));
    }

}
