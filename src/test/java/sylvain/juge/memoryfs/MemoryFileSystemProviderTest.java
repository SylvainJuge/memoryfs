package sylvain.juge.memoryfs;

import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.ServiceLoader;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Fail.fail;

public class MemoryFileSystemProviderTest {

    private static final HashMap<String,Object> EMPTY_OPTIONS = new HashMap<>();

    private static final URI DUMMY_MEMORY_URI = java.net.URI.create("memory://dummy");

    // TODO : see how to use fs only by using a path that uses "memory" scheme

    @Test
    public void loadThroughServiceLoader(){
        FileSystemProvider provider = getProvider();
        assertThat(provider).isInstanceOf(MemoryFileSystemProvider.class);
    }

    @Test
    public void loadThroughFileSystemsAndUri() throws IOException {
        try(FileSystem fs = FileSystems.newFileSystem(DUMMY_MEMORY_URI, EMPTY_OPTIONS)){
            assertThat(fs).isInstanceOf(MemoryFileSystem.class);
            assertThat(FileSystems.getFileSystem(DUMMY_MEMORY_URI)).isSameAs(fs);
        }
    }

    @Test(expectedExceptions = {FileSystemAlreadyExistsException.class})
    public void createDuplicateFileSystem() throws IOException {
        FileSystem fs1 = null;
        FileSystem fs2 = null;
        try {
            fs1 = FileSystems.newFileSystem(DUMMY_MEMORY_URI, EMPTY_OPTIONS);
            fs2 = FileSystems.newFileSystem(DUMMY_MEMORY_URI, EMPTY_OPTIONS);
        } finally {
            fs1.close();
            if (null != fs2) {
                fs2.close();
            }
        }
        fail("should not be able to create two FS with same URI");
    }

    //@Test
    public void providerShouldBeLoadedOnce(){
        // multiple calls to service loader should return the same instance

        // TODO : it seems that service loader creates a new instance at each call
        // however, if fs code uses a for Path resolution, it may be perfectly fine
        // -> we must check that getting instances of this filesystem are identical through paths
        assertThat(getProvider()).isSameAs(getProvider());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void createFileSystemWithoutCorrectScheme() throws IOException {
        FileSystem fs = null;
        try {
            fs = getProvider().newFileSystem(URI.create("withoutMemoryScheme"), EMPTY_OPTIONS);
        } finally {
            assertThat(fs).isNull();
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
        FileSystemProvider provider = getProvider();
        try(FileSystem fs =  provider.newFileSystem(DUMMY_MEMORY_URI, EMPTY_OPTIONS)){
            assertThat(fs).isNotNull();
            assertThat(provider.getFileSystem(DUMMY_MEMORY_URI)).isSameAs(fs);
        }
    }

        @Test
    public void defaultFileSystemProperties() throws IOException {
        try (FileSystem fs = getProvider().newFileSystem(DUMMY_MEMORY_URI, EMPTY_OPTIONS)) {
            assertThat(fs.isOpen()).isTrue();
            assertThat(fs.isReadOnly()).isFalse();
            assertThat(fs.getSeparator()).isNotEmpty();
        }
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

    private static FileSystemProvider getProvider(){
        ServiceLoader<FileSystemProvider> loader = ServiceLoader.load(FileSystemProvider.class);
        for(FileSystemProvider provider:loader){
            if(provider instanceof MemoryFileSystemProvider){
                return MemoryFileSystemProvider.class.cast(provider);
            }
        }
        return null;
    }
}
