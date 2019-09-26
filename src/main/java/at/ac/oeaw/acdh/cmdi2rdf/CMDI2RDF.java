package at.ac.oeaw.acdh.cmdi2rdf;


import java.io.IOException;
import java.nio.file.Files;


import lombok.extern.log4j.Log4j;

/*
* @author Wolfgang Walter SAUER (wowasa) &lt;wolfgang.sauer@oeaw.ac.at&gt;
*/
@Log4j
public class CMDI2RDF {
    

    

    public static void main(String[] args){
        
        try(CMDIFileVisitor visitor = new CMDIFileVisitor()) {
            
            Files.walkFileTree(Configuration.DIR_CMDI, visitor);    

        }
        catch (IOException ex) {
            
            log.error("", ex);
        
        }

    }
    

}
