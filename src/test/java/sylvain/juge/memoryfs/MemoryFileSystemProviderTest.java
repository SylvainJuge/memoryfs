package sylvain.juge.memoryfs;

import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.ServiceLoader;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Fail.fail;

public class MemoryFileSystemProviderTest {


    // TODO : see how to use fs only by using a path that uses "memory" scheme

    @Test
    public void loadThroughServiceLoader(){
        FileSystemProvider provider = getProvider();
        assertThat(provider).isNotNull();
        if (null == provider) {
            fail("memory fs provider not found");
        }

    }

    @Test
    public void checkProviderScheme(){
        FileSystemProvider provider = getProvider();
        assertThat(provider).isNotNull();
        assertThat(provider.getScheme()).isEqualTo("memory");
    }

    @Test
    public void createFileSystemInstanceAndGetByUri() throws IOException {

        URI fsUri = URI.create("dummyUri");
        FileSystemProvider provider = getProvider();
        FileSystem fs = provider.newFileSystem(fsUri, new HashMap<String, Object>());
        assertThat(fs).isNotNull();

        assertThat(provider.getFileSystem(fsUri)).isSameAs(fs);
    }

    // TODO
    // - see how filesytem is used, especially when newFileSystem() is called when creating files in it
    // -> if only 1 instance is used, we can easily control ro/rw state of such fs


    // tests to write
    // - try to get a fs instance without creating it beforehand : myst throw exception

    private static MemoryFileSystemProvider getProvider(){
        ServiceLoader<FileSystemProvider> loader = ServiceLoader.load(FileSystemProvider.class);
        for(FileSystemProvider provider:loader){
            if(provider instanceof MemoryFileSystemProvider){
                return MemoryFileSystemProvider.class.cast(provider);
            }
        }
        return null;
    }
}
