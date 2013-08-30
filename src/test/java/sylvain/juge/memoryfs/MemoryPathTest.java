package sylvain.juge.memoryfs;

import org.testng.annotations.Test;

import java.net.URI;
import java.nio.file.Path;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Fail.fail;

public class MemoryPathTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void fileSystemRequired() {
        new MemoryFileSystem(null, "/anypath");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void emptyPathNotAllowed() {
        createPath("");
    }

    @Test
    public void getFileSystem() {
        MemoryFileSystem fs = createFs();
        MemoryPath path = new MemoryPath(fs, "/");
        assertThat(path.getFileSystem()).isSameAs(fs);
    }

    @Test
    public void relativePathToUri() {
        MemoryPath path = createPath("relative/path");

        assertThat(path.isAbsolute()).isFalse();
        assertThat(path.getRoot()).isNull(); // relative path does not have root
        // TODO : get relative path parent

        checkUri(path.toUri(), "relative/path");
        checkParents(path,
                "relative/path",
                "relative");
    }


    @Test
    public void absolutePathToUri() {
        MemoryPath path = createPath("/absolute/path");

        assertThat(path.isAbsolute()).isTrue();
        assertThat(path.getRoot()).isEqualTo(createPath("/"));

        checkUri(path.toUri(), "/absolute/path");
        checkParents(path,
                "/absolute",
                "/absolute/path");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void getNameLessThanZero() {
        createPath("/").getName(-1);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void getNameOutOfIndex() {
        MemoryPath path = createPath("/dummy");
        assertThat(path.getNameCount()).isEqualTo(1);
        path.getName(path.getNameCount());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void getNameEmptyParts() {
        MemoryPath path = createPath("/");
        assertThat(path.getNameCount()).isZero();
        path.getName(0);
    }

    @Test
    public void pathWithTrailingSlash() {
        // useless trailing slash should be removed from path
        MemoryPath path = createPath("/withTrailingSlash/");
        assertThat(path.getNameCount()).isEqualTo(1);
        assertThat(path.toUri().toString()).endsWith("/withTrailingSlash");
        assertThat(path).isEqualTo(createPath("/withTrailingSlash"));
    }

    @Test
    public void rootPath() {
        // root path trailing slash should not be removed
        MemoryPath root = createPath("/");
        assertThat(root.getNameCount()).isEqualTo(0);
        assertThat(root.getParent()).isNull(); // TODO : does parent of root is null or is it itself ?
        assertThat(root.getRoot()).isEqualTo(root);
        assertThat(root.toUri().toString()).isEqualTo("memory:///");
    }

    @Test
    public void pathWithUselessSlashes(){
        // useless duplicates slashes should be remvoed from path
        fail("TODO");
    }

    // TODO : generate all possible paths ?
    // ""
    // a / . ..
    // a/ // ./ ../  /a // /. /..   -> 1 item, 1 separator
    // a/b                          -> 2 items, 1 separator
    //                                 3 items, 2 separators

    @Test
    public void equalsHashCodeWithItself(){
        Path p = createPath("anyPath");
        assertThat(p).isEqualTo(p);
        assertThat(p.hashCode()).isEqualTo(p.hashCode());
    }

    @Test
    public void equalsHashCodeWithSamePartsButAbsoluteness(){

    }

    @Test
    public void equalsHashCodeWithEquivalent(){
        fail("TODO");
    }

    @Test
    public void equalsHashCodeWithDifferent(){
        fail("TODO");
    }

    private static void checkRoot(Path p){
        Path root = p.getRoot();
        assertThat(root).isNotNull();
        // root is its own root
        assertThat(root.getRoot()).isEqualTo(root.getRoot());
    }

    private static void checkParents(Path p, String... expectedParents) {
        assertThat(p.getNameCount()).isEqualTo(expectedParents.length);
        for (int i = 0; i < expectedParents.length; i++) {
            Path parent = p.getName(i);
            if( i == 0 ){
                assertThat(p.getParent()).isEqualTo(parent);
            }
            assertThat(parent.isAbsolute()).isEqualTo(p.isAbsolute());
            assertThat(parent.toUri().getPath()).isEqualTo(expectedParents[i]);
            // if child path is absolute, then its parent path must be absolute
            // also, both paths must have the same root
            if(p.isAbsolute()){
                assertThat(parent.isAbsolute()).isTrue();
                assertThat(parent.getRoot()).isEqualTo(p.getRoot());
            } else {
                assertThat(parent.isAbsolute()).isFalse();
                assertThat(parent.getRoot()).isNull();
            }
            // TODO : check that all sub-paths or this path are parent of p
            // using startsWith, endsWith, and other parenting-related methods
        }
    }

    private static void checkUri(URI uri, String path) {
        assertThat(uri.getPath()).isEqualTo(path);
        assertThat(uri.getScheme()).isEqualTo("memory");
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

    private static MemoryPath createPath(String path) {
        return new MemoryPath(createFs(), path);
    }

    private static MemoryFileSystem createFs() {
        MemoryFileSystemProvider provider = new MemoryFileSystemProvider();
        return new MemoryFileSystem(provider, "");
    }
}
