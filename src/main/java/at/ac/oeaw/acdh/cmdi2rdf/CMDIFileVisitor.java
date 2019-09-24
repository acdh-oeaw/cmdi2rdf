package at.ac.oeaw.acdh.cmdi2rdf;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/*
* @author Wolfgang Walter SAUER (wowasa) &lt;wolfgang.sauer@oeaw.ac.at&gt;
*/
public class CMDIFileVisitor implements FileVisitor<Path>, Closeable {
    
    private final ThreadPoolExecutor executor; 
    
    public CMDIFileVisitor() {
        this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Configuration.THREAD_POOL_SIZE);
    }

    @Override
    public FileVisitResult postVisitDirectory(Path arg0, IOException arg1) throws IOException {
        // TODO Auto-generated method stub
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path inDirectory, BasicFileAttributes arg1) throws IOException {
        Path rdfPath = Configuration.DIR_RDF.resolve(inDirectory.getFileName());
        

        if(!Files.exists(rdfPath))
            Files.createDirectory(rdfPath);

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes arg1) throws IOException {
        
        this.executor.execute(new ProcessorThread(path));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path path, IOException arg1) throws IOException {
        // TODO Auto-generated method stub
        return FileVisitResult.CONTINUE;
    }

    @Override
    public void close() throws IOException {
        
        this.executor.shutdown();
        
    }

}
