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

    //@Test
    public void providerShouldBeLoadedOnce(){
        // multiple calls to service loader should return the same instance

        // TODO : it seems that service loader creates a new instance at each call
        // however, if fs code uses a for Path resolution, it may be perfectly fine
        // -> we must check that getting instances of this filesystem are identical through paths
        assertThat(getProvider()).isSameAs(getProvider());
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
        FileSystem fs = newMemoryFileSystem(provider, fsUri);
        assertThat(fs).isNotNull();

        assertThat(provider.getFileSystem(fsUri)).isSameAs(fs);
    }

    @Test
    public void defaultFileSystemProperties(){
        FileSystem fs = newMemoryFileSystem(getProvider(), URI.create("dummy"));
        assertThat(fs.isOpen()).isTrue();
        assertThat(fs.isReadOnly()).isFalse();
        assertThat(fs.getSeparator()).isNotEmpty();

    }

    private static FileSystem newMemoryFileSystem(FileSystemProvider provider, URI uri){
        FileSystem fs = null;
        try {
            fs = provider.newFileSystem(uri, new HashMap<String, Object>());
        } catch (IOException e) {
            fail(e.getMessage());
        }
        return fs;
}

    // TODO
    // - see how filesytem is used, especially when newFileSystem() is called when creating files in it
    // -> if only 1 instance is used, we can easily control ro/rw state of such fs
    //
    // - when storage is implemented, see how we can use ByteBuffer to allocate storage out of heap
    //
    // - misc : create a "/dev/null" fs where we always write
    // - misc : allow to control ro/rw state at runtime (allow to test that app does not write when not required)


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
