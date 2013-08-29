package sylvain.juge.memoryfs;

import org.testng.annotations.Test;

import java.net.URI;
import java.nio.file.Path;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Fail.fail;

public class MemoryPathTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void fileSystemRequired(){
        new MemoryFileSystem(null, "/anypath");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void emptyPathNotAllowed(){
        createPath("");
    }

    @Test
    public void getFileSystem(){
        MemoryFileSystem fs = createFs();
        MemoryPath path = new MemoryPath(fs, "/");
        assertThat(path.getFileSystem()).isSameAs(fs);
    }

    @Test
    public void relativePathToUri(){
        MemoryPath path = createPath("relative/path");

        assertThat(path.isAbsolute()).isFalse();
        checkUri(path.toUri(), "relative/path");
        checkPathParts(path,
                "relative/path",
                "relative");
    }

    @Test
    public void absolutePathToUri(){
        MemoryPath path = createPath("/absolute/path");

        assertThat(path.isAbsolute()).isTrue();
        checkUri(path.toUri(), "/absolute/path");
        checkPathParts(path,
                "/absolute",
                "/absolute/path");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void getNameLessThanZero(){
        createPath("/").getName(-1);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void getNameOutOfIndex(){
        MemoryPath path = createPath("/dummy");
        assertThat(path.getNameCount()).isEqualTo(1);
        path.getName(path.getNameCount());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void getNameEmptyParts(){
        MemoryPath path = createPath("/");
        assertThat(path.getNameCount()).isZero();
        path.getName(0);
    }

    @Test
    public void pathWithTrailingSlash(){
        // trailing slash should be itnored for folders
        fail("TODO");
    }

    private static MemoryPath createPath(String path){
        return new MemoryPath(createFs(), path);
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
        assertThat(uri.getHost()).isNull();
        assertThat(uri.getPort()).isLessThan(0);
        assertThat(uri.getQuery()).isNull();
        assertThat(uri.getRawQuery()).isNull();
        assertThat(uri.getAuthority()).isNull();
        assertThat(uri.getRawAuthority()).isNull();
        assertThat(uri.getFragment()).isNull();
        assertThat(uri.getRawFragment()).isNull();
        assertThat(uri.getUserInfo()).isNull();
        assertThat(uri.getRawUserInfo()).isNull();
    }

    private static MemoryFileSystem createFs(){
        MemoryFileSystemProvider provider = new MemoryFileSystemProvider();
        return new MemoryFileSystem(provider, "");
    }
}
