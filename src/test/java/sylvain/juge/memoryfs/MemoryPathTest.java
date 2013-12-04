package sylvain.juge.memoryfs;

import org.fest.assertions.api.Assertions;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static sylvain.juge.memoryfs.AssertPath.assertThat;
import static sylvain.juge.memoryfs.TestEquals.checkHashCodeEqualsConsistency;

public class MemoryPathTest {

    private static final MemoryFileSystem defaultFs = createFs();

    @Test
    public void tryToCreateWithInvalidCharacters() {
        for (String path : Arrays.asList("", "*", "**", "a**a", "a*a", "*a", "*a", "?")) {
            boolean thrown = false;
            try {
                MemoryPath.create(defaultFs, path);
                fail("should not be able to create file with path : " + path);
            } catch (InvalidPathException e) {
                thrown = true;
            }
            assertThat(thrown).isTrue();
        }
    }

    @Test
    public void createRoot() {
        MemoryPath path = MemoryPath.createRoot(defaultFs);
        assertThat(path).isRoot();
        assertThat(path.toUri().toString()).isEqualTo("memory:/");

        assertThat(path.getNameCount()).isEqualTo(0);
        assertThat(path).isEmpty();
        assertThat(path.iterator().hasNext()).isFalse();
    }

    @Test
    public void createRootWithNonDefaultFsId() {
        MemoryFileSystemProvider provider = new MemoryFileSystemProvider();
        MemoryFileSystem fs = MemoryFileSystem.builder(provider).id("id").build();
        MemoryPath root = MemoryPath.createRoot(fs);
        assertThat(root.toUri().toString()).isEqualTo("memory:/id/");
    }


    @Test
    public void asMemoryPath() {
        Path path = MemoryPath.createRoot(defaultFs);
        MemoryPath memoryPath = MemoryPath.asMemoryPath(path);
        assertThat(memoryPath).isSameAs(path);

        assertThat(MemoryPath.asMemoryPath(null)).isNull();
    }

    @Test(expectedExceptions = ProviderMismatchException.class)
    public void asMemoryPathBadPathType() {
        MemoryPath.asMemoryPath(Paths.get("in/default/fs"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void fileSystemRequired() {
        MemoryPath.create(null, "/anypath");
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
    public void relativePathParts() {
        MemoryPath path = createPath("relative/path");

        assertThat(path).isRelative();

        checkParts(path,
                "relative",
                "path");

    }

    @Test
    public void realPathIsAbsoluteAndNormalized() throws IOException {
        Path real = createPath("a/b").toRealPath();
        assertThat(real).isAbsolute();
        assertThat(real.normalize()).isEqualTo(real);
    }

    @Test
    public void fileKeyIsTheSameForEquivalentPaths() throws IOException {
        Path path = createPath("file");
        Files.createFile(path);

        assertThat(path).exists().isRelative();

        Path absolute = path.toAbsolutePath();
        assertThat(absolute).exists().isAbsolute();

        assertThat(Files.isSameFile(path, absolute)).isTrue();

        Assertions.assertThat(Files.readAttributes(path, BasicFileAttributes.class).fileKey())
                .isEqualTo(Files.readAttributes(absolute, BasicFileAttributes.class).fileKey());
    }

    @Test
    public void checkPathParent() {
        assertThat(createPath("/").getParent()).isNull();
        assertThat(createPath("a").getParent()).isNull();
        checkParent("/a", "/");
        checkParent("/a/b", "/a");
        checkParent("a/b", "a");
    }

    private static void checkParent(String path, String expectedParent) {
        assertThat(createPath(path).getParent()).isEqualTo(createPath(expectedParent));
    }

    @Test
    public void absolutePathParts() {
        MemoryPath path = createPath("/absolute/path");

        assertThat(path).isAbsolute();

        checkParts(path,
                "absolute",
                "path");
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
            assertThat(createPath(absolute))
                    .isAbsolute();
        }
    }

    @Test
    public void relativePath() {
        for (String relative : Arrays.asList("a", "a/b")) {
            assertThat(createPath(relative))
                    .isRelative();
        }
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
        assertThat(root.getParent()).isNull();
        assertThat(root.getRoot()).isEqualTo(root);
    }

    @Test
    public void pathEqualityWorksOnlyWithIdenticalFileSystemInstance() {
        // similar paths but not in the same file system are not equal
        String path = "/a/b/c";
        MemoryFileSystem fs1 = createFs();
        MemoryFileSystem fs2 = createFs();
        MemoryPath path1 = MemoryPath.create(fs1, path);
        MemoryPath path2 = MemoryPath.create(fs2, path);
        assertThat(path1.getPath()).isEqualTo(path2.getPath());
        assertThat(path1.getFileSystem()).isNotSameAs(path2.getFileSystem());
        assertThat(path1).isNotEqualTo(path2);
        assertThat(path1.hashCode()).isNotEqualTo(path2.hashCode());
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
        assertThat(p1).isRelative();
        Path p2 = createPath("/same/path");
        assertThat(p2).isAbsolute();
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
            checkAlreadyNormalized(s, s);
        }
    }

    @Test
    public void normalizeNormalizablePaths() {
        checkAlreadyNormalized("/a/../b", "/b");
        checkAlreadyNormalized("/./a", "/a");
        checkAlreadyNormalized("a/../b/../c", "c");
        checkAlreadyNormalized("a/./b/.", "a/b");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void outOfRootAbsolutePath() {
        // thou shall not go upper than root !
        createPath("/..");
    }

    private final static List<MemoryPath> samplePaths;

    static {
        samplePaths = new ArrayList<>();
        List<String> paths = Arrays.asList("/", "/a", "/a/b", "a", "a/b", "..", ".", "../a", "../..", "../../..");
        for (String path : paths) {
            samplePaths.add(createPath(path));
        }
    }

    @Test
    public void startsAndEndsWithItself() {
        for (MemoryPath p : samplePaths) {
            String msg = String.format("%s should start with %s", p, p);
            assertThat(p.startsWith(p)).describedAs(msg).isTrue();
            assertThat(p.startsWith(p.getPath())).describedAs(msg).isTrue();
            msg = String.format("%s should end with %s", p, p);
            assertThat(p.endsWith(p)).describedAs(msg).isTrue();
            assertThat(p.endsWith(p.getPath())).describedAs(msg).isTrue();
        }
    }

    @Test
    public void absolutePathStartsWithRoot() {
        String rootPath = "/";
        Path root = createPath(rootPath);
        for (MemoryPath p : samplePaths) {
            if (p.isAbsolute()) {
                assertThat(p.startsWith(root)).isTrue();
                assertThat(p.startsWith(rootPath)).isTrue();
            }
        }
    }

    @Test
    public void startsWithPrefixPaths() {
        for (MemoryPath p : samplePaths) {
            MemoryPath prefix = p;
            while (null != prefix) {
                assertThat(p.startsWith(prefix)).isTrue();
                assertThat(p.startsWith(prefix.getPath())).isTrue();
                prefix = MemoryPath.asMemoryPath(prefix.getParent());
            }
        }
    }

    @Test
    public void startsWithSameElementsButAbsoluteness() {
        MemoryPath absoluteAbc = createPath("/a/b/c");
        MemoryPath relativeAbc = createPath("a/b/c");
        assertThat(absoluteAbc.startsWith(relativeAbc)).isFalse();
        assertThat(absoluteAbc.startsWith(relativeAbc.getPath())).isFalse();
    }

    @Test
    public void endsWithSameElementsButAbsoluteness() {
        MemoryPath absoluteAbc = createPath("/a/b/c");
        MemoryPath relativeAbc = createPath("a/b/c");
        assertThat(absoluteAbc.endsWith(relativeAbc)).isTrue();
        assertThat(absoluteAbc.endsWith(relativeAbc.getPath())).isTrue();
    }

    @Test
    public void endsWithLongerReturnFalse() {
        assertThat(createPath("/a/b").endsWith(createPath("/a/b/c"))).isFalse();
    }

    @Test
    public void startsWithStringPrefix() {
        // a/bc/d starts with a/b (which is not true for paths)
        MemoryPath abcd = createPath("a/bc/d");
        MemoryPath ab = createPath("a/b");
        assertThat(abcd.startsWith(ab)).isFalse();
        assertThat(abcd.startsWith(ab.getPath())).isTrue();
    }

    @Test
    public void endsWithStringPrefix() {
        // a/bc/d ends with c/d (which is not true for paths)
        MemoryPath abcd = createPath("a/bc/d");
        MemoryPath cd = createPath("c/d");
        assertThat(abcd.endsWith(cd)).isFalse();
        assertThat(abcd.endsWith(cd.getPath())).isTrue();
    }

    @Test
    public void startsAndEndsWithDoesNotNormalize() {
        // Note : we take absolute paths otherwise second paths ends with a
        MemoryPath a = createPath("/a");
        MemoryPath bDotDotA = createPath("/b/../a");
        assertThat(bDotDotA.normalize()).isEqualTo(a);
        assertThat(bDotDotA.startsWith(a)).isFalse();
        assertThat(bDotDotA.endsWith(a)).isFalse();
    }

    @Test(expectedExceptions = ProviderMismatchException.class)
    public void startsWithWrongPathType() {
        createPath("/").startsWith(anotherPathType());
    }

    @Test(expectedExceptions = ProviderMismatchException.class)
    public void endsWithWrongPathType() {
        createPath("/").endsWith(anotherPathType());
    }

    @Test(expectedExceptions = ProviderMismatchException.class)
    public void resolveWithWrongPathType() {
        createPath("/").resolve(anotherPathType());
    }

    @Test(expectedExceptions = ProviderMismatchException.class)
    public void resolveSiblingWithWrongPathType() {
        createPath("/").resolve(anotherPathType());
    }

    @Test(expectedExceptions = ProviderMismatchException.class)
    public void relativizeWithWrongPathType() {
        createPath("/").relativize(anotherPathType());
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void compareToWithWrongPathType() {
        createPath("/").compareTo(anotherPathType());
    }

    @Test
    public void compareWithItselfAndEqualInstance() {
        MemoryPath path = createPath("/a/b/c");
        assertThat(path.compareTo(path)).isEqualTo(0);

        MemoryPath other = createPath(path.getPath());
        assertThat(path.compareTo(other)).isEqualTo(0);
    }

    @Test
    public void compareToUsesNaturalOrder() {
        checkCompareToStrictOrder("/a", "/b", "/c");
    }

    @Test
    public void compareToAbsolutesFirst() {
        checkCompareToStrictOrder("/a", "/b", "/c", "a", "b", "c");
    }

    @Test
    public void compareToDepthFirstOrdering() {
        // sub-paths of X are before paths that are after X
        checkCompareToStrictOrder("a", "a/b", "ab");
    }

    @Test
    public void compareToShortestFirst() {
        checkCompareToStrictOrder("a/b", "a/b/c");
    }

    @Test
    public void subPathReturnsRelativePath() {
        // get all items of a path
        // check that resulting path is the same as original, but in relative
        // we also check that start is inclusive, and end is exclusive
        MemoryPath path = createPath("/a/b/c");
        assertThat(path.isAbsolute()).isTrue();
        assertThat(path.subpath(0, path.getNameCount())).isEqualTo(createPath("a/b/c"));
        assertThat(path.subpath(0, 1)).isEqualTo(createPath("a"));
        assertThat(path.subpath(1, 2)).isEqualTo(createPath("b"));
        assertThat(path.subpath(2, 3)).isEqualTo(createPath("c"));
        assertThat(path.subpath(0, 2)).isEqualTo(createPath("a/b"));
        assertThat(path.subpath(1, 3)).isEqualTo(createPath("b/c"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void subPathWithoutElementsAskMoreThanAvailable() {
        MemoryPath path = createPath("/");
        assertThat(path.getNameCount()).isEqualTo(0);
        path.subpath(0, 1);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void subPathNegativeStart() {
        createPath("/a").subpath(-1, 0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void subPathStartAfterEnd() {
        createPath("/a/b").subpath(2, 1);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void watchRegisterNotSupported1() throws IOException {
        createPath("/").register(null, new WatchEvent.Kind[0]);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void watchRegisterNotSupported2() throws IOException {
        createPath("/").register(null, null, new WatchEvent.Modifier[0]);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void toFileNotSupported() {
        createPath("/").toFile();
    }

    @Test
    public void resolvePath() {
        checkResolvePath("a/b", "/a", "/a");
        checkResolvePath("a/b", "/", "/");
        checkResolvePath("/a", "/b", "/b");

        checkResolvePath("a", "b", "a/b");
        checkResolvePath("/a", "b/c", "/a/b/c");
        checkResolvePath("/", "a/b", "/a/b");
    }

    @Test
    public void resolveString() {
        checkResolvePathString("a/b", "c/d", "a/b/c/d");
        checkResolvePathString("a/b", "/c", "/c");
        checkResolvePathString("/", "c", "/c");
    }

    private void checkResolvePathString(String path, String toResolve, String expected) {
        MemoryPath initialPath = createPath(path);
        Path resolved = initialPath.resolve(toResolve);
        assertThat(resolved).isEqualTo(createPath(expected));

        assertThat(initialPath.getPath()).describedAs("initial path not modified").isEqualTo(path);
    }

    @Test
    public void relativize() {

        checkRelativize("a/b", "a/b/c/d", "c/d");
        checkRelativize("/a/b", "/a/b/c/d", "c/d");
        checkRelativize("/a/b", "/c/d", "../../c/d");

        checkRelativize("/", "/a/b", "a/b");

        // equal paths : return the path itself
        checkRelativize("a/b", "a/b", ".");

        // not possible to compute relative path, return direct path
        // not really intuitive, returning null may be more meaningful since we
        // can't return a "relative" path, between those paths.
        // as user, we have to check result of relativize to check if
        // result equals second argument.
        checkRelativize("a", "/", "/");
        checkRelativize("/a", "b", "b");
        checkRelativize("a/b", "c/d", "c/d");

        // TODO : add some tests with relative paths : ..
    }

    private static void checkRelativize(String first, String toRelativize, String expected) {
        assertThat(createPath(first).relativize(createPath(toRelativize))).isEqualTo(createPath(expected));
    }

    @Test
    public void resolveSiblingPath() {
        // base case
        checkResolveSiblingPath("a/b", "c", "a/c");

        // absolute siblings
        checkResolveSiblingPath("a/b", "/", "/");
        checkResolveSiblingPath("a/b", "/b", "/b");

        // a has no parent
        checkResolveSiblingPath("a", "b", "b");
        checkResolveSiblingPath("a", "/b", "/b");
        checkResolveSiblingPath("/", "c", "c");

        // Note : we don't allow empty paths (even if javadoc seems to allow it)
    }

    @Test
    public void resolveSiblingString() {
        assertThat(createPath("a/b").resolveSibling("c/d")).isEqualTo(createPath("a/c/d"));
        assertThat(createPath("a").resolveSibling("/b")).isEqualTo(createPath("/b"));

        // resolve with empty string should return same path
        // default fs implementation has the same behavior, and it's just convenient
        Path path = createPath("a/b");
        assertThat(path.resolve("")).isEqualTo(path);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void pathIteratorDoesNotAllowRemove() {
        createPath("/").iterator().remove();
    }

    private static void checkResolvePath(String path, String toResolve, String expected) {
        MemoryPath initialPath = createPath(path);
        assertThat(initialPath.resolve(createPath(toResolve))).isEqualTo(createPath(expected));
        assertThat(initialPath.getPath()).describedAs("initial path not modified").isEqualTo(path);
    }

    private static void checkResolveSiblingPath(String base, String sibling, String expected) {
        MemoryPath basePath = createPath(base);
        assertThat(basePath.resolveSibling(createPath(sibling))).isEqualTo(createPath(expected));
        assertThat(basePath.getPath()).describedAs("initial path not modified").isEqualTo(base);
    }

    private static void checkCompareToStrictOrder(String... paths) {
        if (paths.length < 2) {
            throw new IllegalArgumentException("at least 2 paths expected");
        }
        for (int i = 1; i < paths.length; i++) {
            checkCompareIsBefore(createPath(paths[i - 1]), createPath(paths[i]));
        }
    }

    private static void checkCompareIsBefore(MemoryPath first, MemoryPath second) {
        int firstToSecond = first.compareTo(second);
        int secondToFirst = second.compareTo(first);
        if (first.equals(second)) {
            assertThat(firstToSecond).isEqualTo(secondToFirst).isEqualTo(0);
        } else {
            assertThat(firstToSecond).describedAs(first + " must be before " + second).isLessThan(0);
            assertThat(secondToFirst).describedAs(second + " must be after " + first).isGreaterThan(0);
        }
    }

    private static void checkAlreadyNormalized(String p, String expected) {
        assertThat(createPath(p).normalize()).isEqualTo(createPath(expected));
    }

    private static void checkParts(Path p, String... expectedParts) {
        assertThat(p.getNameCount()).isEqualTo(expectedParts.length);

        // check parents through iterable interface
        Path[] expectedPaths = new Path[expectedParts.length];
        int i = 0;
        for (String part : expectedParts) {
            expectedPaths[i++] = createPath(part);
        }
        assertThat(p).containsSequence(expectedPaths);

        // test each of them through getName and through iterator
        Iterator<Path> it = p.iterator();
        i = 0;
        for (Path part : expectedPaths) {

            assertThat(it.hasNext()).isTrue();
            assertThat(part).isEqualTo(it.next());

            assertThat(part).isEqualTo(p.getName(i));
            assertThat(part.isAbsolute()).isFalse();
            i++;
        }

        // ensure that iterator has properly reached end
        assertThat(it.hasNext()).isFalse();
        boolean thrown = false;
        try {
            assertThat(it.next()).isNull();
        } catch (NoSuchElementException e) {
            thrown = true;
        }
        assertThat(thrown).isTrue();
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
        MemoryPath result = MemoryPath.create(defaultFs, path);
        URI uri = result.toUri();
        assertThat(uri.getScheme()).isEqualTo("memory");
        return result;
    }

    private static MemoryFileSystem createFs() {
        MemoryFileSystemProvider provider = new MemoryFileSystemProvider();
        return MemoryFileSystem.builder(provider).build();
    }

    private static Path anotherPathType() {
        Path path = Paths.get("/dummy/path/in/default/fs");
        assertThat(path).isNotInstanceOf(MemoryPath.class);
        return path;
    }
}

