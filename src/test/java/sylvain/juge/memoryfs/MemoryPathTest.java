package sylvain.juge.memoryfs;

import org.testng.annotations.Test;

import java.net.URI;
import java.nio.file.Path;

import static org.fest.assertions.api.Assertions.assertThat;

public class MemoryPathTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void fileSystemRequired(){
        new MemoryFileSystem(null, "/anypath");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void emptyPathNotAllowed(){
        new MemoryPath(createFs(), "");
    }

    @Test
    public void getFileSystem(){
        MemoryFileSystem fs = new MemoryFileSystem(null, null);
        MemoryPath path = new MemoryPath(fs, "");
        assertThat(path.getFileSystem()).isSameAs(fs);
    }

    @Test
    public void relativePathToUri(){
        MemoryPath path = new MemoryPath(null, "relative/path");

        assertThat(path.isAbsolute()).isFalse();
        checkUri(path.toUri(), "relative/path");
        checkPathParts(path,
                "relative/path",
                "relative");
    }

    @Test
    public void absolutePathToUri(){
        MemoryPath path = new MemoryPath(null, "/absolute/path/");

        assertThat(path.isAbsolute()).isTrue();
        checkUri(path.toUri(), "/absolute/path");
        checkPathParts(path,
                "/absolute/path",
                "/absolute");
    }

    private static void checkPathParts(Path p, String... parts){
        assertThat(p.getNameCount()).isEqualTo(parts.length);
        for(int i=0;i<parts.length;i++){
            Path part = p.getName(i);
            assertThat(part.isAbsolute()).isEqualTo(p.isAbsolute());
            assertThat(part.toUri().getPath()).isEqualTo(parts[i]);
        }
    }

    private static void checkUri(URI uri, String path){
        String scheme = "memory";
        assertThat(uri.getPath()).isEqualTo(path);
        assertThat(uri.getScheme()).isEqualTo(scheme);
        assertThat(uri.getSchemeSpecificPart()).isEqualTo(scheme);
        assertThat(uri.getRawSchemeSpecificPart()).isEqualTo(scheme);
        assertThat(uri.getHost()).isEmpty();
        assertThat(uri.getPort()).isLessThan(0);
        assertThat(uri.getQuery()).isEmpty();
        assertThat(uri.getAuthority()).isEmpty();
        assertThat(uri.getRawAuthority()).isEmpty();
        assertThat(uri.getFragment()).isEmpty();
        assertThat(uri.getRawFragment()).isEmpty();
        assertThat(uri.getQuery()).isEmpty();
        assertThat(uri.getRawQuery()).isEmpty();
        assertThat(uri.getUserInfo()).isEmpty();
        assertThat(uri.getRawUserInfo()).isEmpty();
    }

    private static MemoryFileSystem createFs(){
        MemoryFileSystemProvider provider = new MemoryFileSystemProvider();
        MemoryFileSystem fs = new MemoryFileSystem(provider, "");
        return fs;
    }
}
