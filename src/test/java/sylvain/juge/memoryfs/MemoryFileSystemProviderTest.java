package sylvain.juge.memoryfs;

import org.testng.annotations.Test;

import java.nio.file.spi.FileSystemProvider;
import java.util.ServiceLoader;

import static org.fest.assertions.api.Fail.fail;

public class MemoryFileSystemProviderTest {

    @Test
    public void loadThroughServiceLoader(){
        ServiceLoader<FileSystemProvider> loader = ServiceLoader.load(FileSystemProvider.class);
        boolean found = false;
        for(FileSystemProvider provider:loader){
            if(MemoryFileSystemProvider.class.equals(provider.getClass())){
                found = true;
                break;
            }
        }
        if (!found) {
            fail("memory fs provider not found");
        }

    }
}
