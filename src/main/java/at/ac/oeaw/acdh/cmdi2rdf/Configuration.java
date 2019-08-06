package at.ac.oeaw.acdh.cmdi2rdf;
/*
* @author Wolfgang Walter SAUER (wowasa) &lt;wolfgang.sauer@oeaw.ac.at&gt;
*/

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import lombok.extern.log4j.Log4j;

@Log4j
public class Configuration {
    public static final Map CONDITIONS;
    public static final Path DIR_CMDI;
    public static final Path DIR_RDF;
    public static final String FILE_MAPPING;
    public static final Path FILE_POLICY;
    public static final int SIZE_THREAD_POOL;
    
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
        
        CONDITIONS = new HashMap();
        DIR_CMDI = Paths.get(properties.getProperty("DIR_CMDI"));
        DIR_RDF = Paths.get(properties.getProperty("DIR_RDF"));
        FILE_MAPPING = properties.getProperty("FILE_MAPPING");
        FILE_POLICY = Paths.get(properties.getProperty("FILE_POLICY"));
        SIZE_THREAD_POOL = Integer.valueOf(properties.getProperty("SIZE_THREAD_POOL"), 10);
        
    }

}
