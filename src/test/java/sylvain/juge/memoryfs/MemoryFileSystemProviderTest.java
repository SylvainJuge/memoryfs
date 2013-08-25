package sylvain.juge.memoryfs;

import org.testng.annotations.Test;

import java.nio.file.spi.FileSystemProvider;
import java.util.ServiceLoader;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Fail.fail;

public class MemoryFileSystemProviderTest {

    @Test
    public void loadThroughServiceLoader(){
        MemoryFileSystemProvider provider = getProvider();
        assertThat(provider).isNotNull();
        if (null == provider) {
            fail("memory fs provider not found");
        }

    }

    @Test
    public void checkProviderScheme(){
        MemoryFileSystemProvider provider = getProvider();
        assertThat(provider).isNotNull();
        assertThat(provider.getScheme()).isEqualTo("memory");
    }

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
