package sylvain.juge.memoryfs;

import org.fest.assertions.api.StringAssert;
import org.testng.annotations.Test;

import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;

import static org.fest.assertions.api.Assertions.assertThat;

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
        MemoryPath path = MemoryPath.create(fs, "/");
        assertThat(path.getFileSystem()).isSameAs(fs);
    }

    @Test
    public void relativePathToUri() {
        MemoryPath path = createPath("relative/path");

        assertThat(path.isAbsolute()).isFalse();
        assertThat(path.getRoot()).isNull(); // relative path does not have root
        // TODO : get relative path parent

        checkParents(path,
                "relative",
                "relative/path");
    }

    @Test
    public void absolutePathToUri() {
        MemoryPath path = createPath("/absolute/path");

        assertThat(path.isAbsolute()).isTrue();
        assertThat(path.getRoot()).isEqualTo(createPath("/"));

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
    public void getFileName(){
        checkFileName(createPath("/"), null);
        checkFileName(createPath("/a"), "a");
        checkFileName(createPath("/a/b"), "b");
        checkFileName(createPath("a"), "a");
        checkFileName(createPath("a/b"), "b");
    }

    @Test
    public void absolutePath(){
        for(String absolute: Arrays.asList("/", "/a", "/a/b")){
            MemoryPath path = createPath(absolute);
            assertThat(path.isAbsolute()).isTrue();
            assertThat(path.toAbsolutePath()).isEqualTo(path);
        }
        for(String relative: Arrays.asList("a","a/b")){
            MemoryPath path = createPath(relative);
            assertThat(path.isAbsolute()).isFalse();
            Path toAbsolute = path.toAbsolutePath();
            assertThat(toAbsolute.isAbsolute()).isTrue();
            // TODO : check that resuling path endsWith 'relative' as suffix
            assertThat(toAbsolute.toUri().toString()).isEqualTo("memory:///" + relative);
        }
        // TODO : relative path without normalization
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
        MemoryPath root = createPath("/");
        assertThat(root.getNameCount()).isEqualTo(0);
        assertThat(root.getParent()).isNull(); // TODO : does parent of root is null or is it itself ?
        assertThat(root.getRoot()).isEqualTo(root);
    }

    @Test
    public void equalsHashCodeWithItself() {
        Path p;
        p = createPath("/absolute");
        checkHashCodeEqualsConsistency(true, p, p);
        p = createPath("relative");
        checkHashCodeEqualsConsistency(true, p, p);
    }

    @Test
    public void equalsHashCodeWithSamePartsButAbsoluteness() {
        Path p1 = createPath("same/path");
        assertThat(p1.isAbsolute()).isFalse();
        Path p2 = createPath("/same/path");
        assertThat(p2.isAbsolute()).isTrue();
        checkHashCodeEqualsConsistency(false, p1, p2);
    }

    @Test
    public void equalsHashCodeWithEquivalent() {
        checkHashCodeEqualsConsistency(true,
                createPath("/"),
                createPath("///"),
                createPath("//"));
        checkHashCodeEqualsConsistency(true,
                createPath("/a"),
                createPath("/a/"),
                createPath("/a//"),
                createPath("//a"));
        checkHashCodeEqualsConsistency(true,
                createPath("a/b"),
                createPath("a/b/"),
                createPath("a//b"));
    }

    @Test
    public void equalsHashCodeWithDifferent() {
        checkHashCodeEqualsConsistency(false,
                createPath("/a"),
                createPath("/b"));
        checkHashCodeEqualsConsistency(false,
                createPath("a"),
                createPath("b"));
    }

    @SafeVarargs
    private static <T> void checkHashCodeEqualsConsistency(boolean shouldEqual, T... o) {
        assertThat(o.length).isGreaterThanOrEqualTo(1);
        for (int i = 0; i < o.length; i++) {
            for (int j = 0; j < o.length; j++) {
                if (j <= i) {
                    if (shouldEqual) {
                        assertThat(o[i]).isEqualTo(o[j]);
                        assertThat(o[j]).isEqualTo(o[i]);
                        assertThat(o[i].hashCode()).isEqualTo(o[j].hashCode());
                    } else if (j < i) {
                        assertThat(o[i]).isNotEqualTo(o[j]);
                        assertThat(o[j]).isNotEqualTo(o[i]);
                        assertThat(o[i].hashCode()).isNotEqualTo(o[j].hashCode());
                    }
                }
            }
        }
    }

    private static void checkParents(Path p, String... expectedParents) {
        assertThat(p.getNameCount()).isEqualTo(expectedParents.length);
        for (int i = 0; i < expectedParents.length; i++) {
            Path parent = p.getName(i);
            if (i == 0) {
                // getName(0) return identity;
                assertThat(p.getParent()).isEqualTo(parent);
            }

            // toUri converts to absolute path, thus we can't test a lot more than suffix
            assertThat(p.toUri().getPath()).startsWith(parent.toUri().getPath());

            // if child path is absolute, then its parent path must be absolute
            // also, both paths must have the same root
            if (p.isAbsolute()) {
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

    private static void checkFileName(Path path, String expectedFileName){
        Path fileName = path.getFileName();
        if( null == expectedFileName){
            assertThat(fileName).isNull();
        } else {
            assertThat(fileName.isAbsolute()).describedAs("relative path").isFalse();
            assertThat(fileName).isEqualTo(createPath(expectedFileName));
        }
    }

    private static MemoryPath createPath(String path) {
        MemoryPath result = MemoryPath.create(createFs(), path);
        URI uri = result.toUri();
        // TODO : check normalized and/or absolute paths, because removing slashes does not allow direct comparison
        // assertThat(uri.getPath()).isEqualTo(path);
        assertThat(uri.getScheme()).isEqualTo("memory");
        assertThat(uri.getHost()).isNull();
        return result;
    }

    private static MemoryFileSystem createFs() {
        MemoryFileSystemProvider provider = new MemoryFileSystemProvider();
        return new MemoryFileSystem(provider, "");
    }
}
