package sylvain.juge.memoryfs;

import org.testng.annotations.Test;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;
import static sylvain.juge.memoryfs.TestEquals.checkHashCodeEqualsConsistency;

public class MemoryPathTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void providerRequired() {
        MemoryPath.create(null,"/anypath");
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

        checkParts(path,
                "relative",
                "relative/path");
    }


    @Test
    public void absolutePathToUri() {
        MemoryPath path = createPath("/absolute/path");

        assertThat(path.isAbsolute()).isTrue();
        assertThat(path.getRoot()).isEqualTo(createPath("/"));

        checkParts(path,
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
    public void getFileName() {
        // like default FS, root path is an "empty absolute path"
        checkFileName(createPath("/"), null);
        checkFileName(createPath("/a"), "a");
        checkFileName(createPath("/a/b"), "b");
        checkFileName(createPath("a"), "a");
        checkFileName(createPath("a/b"), "b");
    }

    @Test
    public void absolutePath() {
        for (String absolute : Arrays.asList("/", "/a", "/a/b")) {
            MemoryPath path = createPath(absolute);
            assertThat(path.isAbsolute()).isTrue();
            assertThat(path.toAbsolutePath()).isEqualTo(path);
        }
        for (String relative : Arrays.asList("a", "a/b")) {
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
        MemoryPath path = createAndCheckPath("/withTrailingSlash/", "/withTrailingSlash");
        assertThat(path.getNameCount()).isEqualTo(1);
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

    @Test
    public void normalizeNormalizedOrNonNormalizablePaths() {
        for (String s : Arrays.asList("/a", "/a/b", "a", "a/b", "..", ".", "../a", "../..", "../../..")) {
            checkNormalize(s, s);
        }
    }

    @Test
    public void normalizeNormalizablePaths() {
        checkNormalize("/a/../b", "/b");
        checkNormalize("/./a", "/a");
        checkNormalize("a/../b/../c", "c");
        checkNormalize("a/./b/.", "a/b");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void outOfRootAbsolutePath() {
        // thou shall not go upper than root !
        createPath("/..");
    }

    @Test
    public void startWith(){
        assertThat(createPath("/"));
    }

    private static void checkNormalize(String p, String expected) {
        assertThat(createPath(p).normalize()).isEqualTo(createPath(expected));
    }

    private static void checkParts(Path p, String... expectedParts) {
        assertThat(p.getNameCount()).isEqualTo(expectedParts.length);

        // check parents through iterable interface
        Path[] expectedPaths = new Path[expectedParts.length];
        int i=0;
        for (String part : expectedParts) {
            expectedPaths[i++] = createPath(part);
        }
        assertThat(p).containsSequence(expectedPaths);

        // test each of them through getName
        i=0;
        for (Path part:expectedPaths) {
            assertThat(part).isEqualTo(p.getName(i));
            if (i == 0) {
                // getName(0) return identity;
                assertThat(p.getParent()).isEqualTo(part);
            }
            i++;

            // toUri converts to absolute path, thus we can't test a lot more than suffix
            assertThat(p.toUri().getPath()).startsWith(part.toUri().getPath());

            // if path is absolute, then its parts path must be absolute
            // also, both paths must have the same root
            if (p.isAbsolute()) {
                assertThat(part.isAbsolute()).isTrue();
                assertThat(part.getRoot()).isEqualTo(p.getRoot());
            } else {
                assertThat(part.isAbsolute()).isFalse();
                assertThat(part.getRoot()).isNull();
            }
        }
    }

    private static void checkFileName(Path path, String expectedFileName) {
        Path fileName = path.getFileName();
        if (null == expectedFileName) {
            assertThat(fileName).isNull();
        } else {
            assertThat(fileName.isAbsolute()).describedAs("relative path").isFalse();
            assertThat(fileName).isEqualTo(createPath(expectedFileName));
        }
    }

    private static MemoryPath createAndCheckPath(String path, String expectedPath) {
        MemoryPath item = createPath(path);
        String expectedItemPath = (item.isAbsolute() ? "" : "/") + expectedPath;
        assertThat(item.toUri().getPath()).isEqualTo(expectedItemPath);
        return item;
    }

    private static MemoryPath createPath(String path) {
        MemoryPath result = MemoryPath.create(createFs(), path);
        URI uri = result.toUri();
        assertThat(uri.getScheme()).isEqualTo("memory");
        assertThat(uri.getHost()).isNull();
        return result;
    }

    private static MemoryFileSystem createFs() {
        MemoryFileSystemProvider provider = new MemoryFileSystemProvider();
        return MemoryFileSystem.builder(provider).build();
    }
}
