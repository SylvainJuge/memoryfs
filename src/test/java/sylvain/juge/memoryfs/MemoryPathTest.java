package sylvain.juge.memoryfs;

import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

import static org.fest.assertions.api.Assertions.assertThat;
import static sylvain.juge.memoryfs.TestEquals.checkHashCodeEqualsConsistency;

public class MemoryPathTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void fileSystemRequired() {
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
            assertThat(toAbsolute.toUri().toString()).isEqualTo("memory:/" + relative);
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

    private final static List<MemoryPath> samplePaths;
    static {
        samplePaths = new ArrayList<>();
        List<String> paths = Arrays.asList("/","/a", "/a/b", "a", "a/b", "..", ".", "../a", "../..", "../../..");
        for (String path : paths) {
            samplePaths.add(createPath(path));
        }
    }

    @Test
    public void startsAndEndsWithItself(){
        for (MemoryPath p :samplePaths) {
            assertThat(p.startsWith(p)).isTrue();
            assertThat(p.startsWith(p.getPath())).isTrue();
            assertThat(p.endsWith(p)).isTrue();
            assertThat(p.endsWith(p.getPath())).isTrue();
        }
    }

    // TODO : make test shorter using a collection of MemoryPath (allow to get path)

    @Test
    public void absolutePathStartsWithRoot(){
        String rootPath = "/";
        Path root = createPath(rootPath);
        for(MemoryPath p: samplePaths){
            if(p.isAbsolute()){
                assertThat(p.startsWith(root));
            }
        }
    }

    @Test
    public void startsWithPrefixPaths(){
        for(MemoryPath p:samplePaths){
            for(int i =0;i<p.getNameCount();i++){
                MemoryPath prefix = (MemoryPath) p.getName(i);
                assertThat(p.startsWith(prefix)).isTrue();
                assertThat(p.startsWith(prefix.getPath())).isTrue();
            }
        }
    }

    @Test
    public void startsWithSameElementsButAbsoluteness(){
        MemoryPath absoluteAbc = createPath("/a/b/c");
        MemoryPath relativeAbc = createPath("a/b/c");
        assertThat(absoluteAbc.startsWith(relativeAbc)).isFalse();
        assertThat(absoluteAbc.startsWith(relativeAbc.getPath())).isFalse();
    }

    @Test
    public void endsWithSameElementsButAbsoluteness(){
        MemoryPath absoluteAbc = createPath("/a/b/c");
        MemoryPath relativeAbc = createPath("a/b/c");
        assertThat(absoluteAbc.endsWith(relativeAbc)).isTrue();
        assertThat(absoluteAbc.endsWith(relativeAbc.getPath())).isTrue();
    }

    @Test
    public void startsWithStringPrefix(){
        // a/bc/c starts with a/b (which is not true for paths)
        MemoryPath abcd = createPath("a/bc/d");
        MemoryPath ab = createPath("a/b");
        assertThat(abcd.startsWith(ab)).isFalse();
        assertThat(abcd.startsWith(ab.getPath())).isTrue();
    }


    @Test(enabled = false)
    public void startEndWith(){

        String pathRoot = "/";
        Path root = createPath(pathRoot);

        String pathAbsoluteA = "/a";
        Path absoluteA = createPath(pathAbsoluteA);

        String pathAbsoluteB = "/b";
        Path absoluteB = createPath(pathAbsoluteB);

        String pathAbsoluteAb = "/a/b";
        Path absoluteAb = createPath(pathAbsoluteAb);

        String pathRelativeA = "a";
        Path relativeA = createPath(pathRelativeA);

        String pathRelativeB = "b";
        Path relativeB = createPath(pathRelativeB);

        String pathRelativeAb = "a/b";
        Path relativeAb = createPath(pathRelativeAb);

        String pathRelativeAbcd = "a/bc/d";
        Path relativeAbcd = createPath(pathRelativeAbcd);

        String pathRelativeCd = "c/d";
        Path relativeCd = createPath(pathRelativeCd);

        // check starts with path
        Set<Path> allPaths = new HashSet<>(Arrays.asList(root, absoluteA, absoluteB, absoluteAb, relativeA, relativeB, relativeAb, relativeCd));

        checkPathPart(allPaths, pathStartsWithPath, root);
        checkPathPart(allPaths, pathStartsWithPath, absoluteA, root);
        checkPathPart(allPaths, pathStartsWithPath, absoluteB, root);


        checkPathPart(allPaths, pathStartsWithPath, absoluteAb, absoluteA, root);
        checkPathPart(allPaths, pathStartsWithPath, relativeA);
        checkPathPart(allPaths, pathStartsWithPath, relativeB);
        checkPathPart(allPaths, pathStartsWithPath, relativeAb, relativeA);

        /// check starts with string

        Set<String> allPathsStrings = new HashSet<>(Arrays.asList(pathRoot, pathAbsoluteA, pathAbsoluteAb, pathRelativeA, pathRelativeB, pathRelativeAb, pathRelativeCd, pathRelativeAbcd));

        checkPathPart(allPathsStrings, pathStartsWithString, absoluteA, pathRoot, pathAbsoluteA);
        checkPathPart(allPathsStrings, pathStartsWithString, absoluteB, pathRoot, pathAbsoluteB);
        checkPathPart(allPathsStrings, pathStartsWithString, absoluteAb, pathRoot, pathAbsoluteA, pathAbsoluteAb);
        checkPathPart(allPathsStrings, pathStartsWithString, relativeA, pathRelativeA);
        checkPathPart(allPathsStrings, pathStartsWithString, relativeB, pathRelativeB);

        checkPathPart(allPathsStrings, pathStartsWithString, relativeAbcd, pathRelativeAbcd, pathRelativeA, pathRelativeAb);

        // TODO : test assert that relativeAbcd.endsWith("c/d");

        // ends with path
        checkPathPart(allPaths, pathEndsWithPath, root);
        checkPathPart(allPaths, pathEndsWithPath, absoluteA, root);
        checkPathPart(allPaths, pathEndsWithPath, absoluteB, root);
        checkPathPart(allPaths, pathEndsWithPath, absoluteAb, root, absoluteAb, relativeAb, relativeB);
        checkPathPart(allPaths, pathEndsWithPath, relativeA);
        checkPathPart(allPaths, pathEndsWithPath, relativeB);
        checkPathPart(allPaths, pathEndsWithPath, relativeAb, relativeB);
        checkPathPart(allPaths, pathEndsWithPath, relativeCd);

        // ends with string

        checkPathPart(allPathsStrings, pathEndsWithString, root, pathRoot);
        checkPathPart(allPathsStrings, pathEndsWithString, absoluteA, pathRelativeA);

    }


    @SafeVarargs
    private static  <T> void checkPathPart(Set<T> all, PathPartStrategy<T> strategy, Path p, T... matching){
        Set<T> matchingItems = new HashSet<>(Arrays.asList(matching));
        if( matching.length > 0){
            assertThat(all).contains(matching);
        }
        for (T item : all) {
            boolean shouldMatch = matchingItems.contains(item) || item.equals(p);
            String msg = startEndWithMsg(shouldMatch, true, p, item);
            assertThat(strategy.matches(p,item)).describedAs(msg).isEqualTo(shouldMatch);
        }
    }
    @SafeVarargs
    private static  <T> void checkPathPart(PathPartStrategy<T> strategy, Path p, T... matching){
        Set<T> matchingItems = new HashSet<>(Arrays.asList(matching));
        for (T item : matching) {
            boolean shouldMatch = matchingItems.contains(item) || item.equals(p);
            String msg = startEndWithMsg(shouldMatch, true, p, item);
            assertThat(strategy.matches(p,item)).describedAs(msg).isEqualTo(shouldMatch);
        }
    }

    private static abstract class PathPartStrategy<X> {
        private final boolean shouldMatch;
        private PathPartStrategy(boolean shouldMatch){
            this.shouldMatch = shouldMatch;
        }
        abstract boolean matches(Path path, X other);


    }
    private final static PathPartStrategy<String> pathStartsWithString = new PathPartStrategy<String>(true) {
        @Override
        public boolean matches(Path path, String other) {
            return path.startsWith(other);
        }
    };
    private final static PathPartStrategy<Path> pathStartsWithPath = new PathPartStrategy<Path>(true) {
        @Override
        public boolean matches(Path path, Path other) {
            // starts with path means property is the same as string (reverse is not true)
            return path.startsWith(other) && ((MemoryPath)path).getPath().equals(((MemoryPath)other).getPath());
        }
    };
    private final static PathPartStrategy<Path> pathEndsWithPath = new PathPartStrategy<Path>(true){
        @Override
        public boolean matches(Path path, Path other) {
            // ends with path means property is the same as string (reverse is not true)
            return path.endsWith(other) && ((MemoryPath)path).getPath().endsWith(((MemoryPath)other).getPath());
        }
    };
    private final static PathPartStrategy<String> pathEndsWithString = new PathPartStrategy<String>(true) {
        @Override
        public boolean matches(Path path, String other) {
            return path.endsWith(other);
        }
    };




    private static void checkStartsWith(boolean shouldMatch, Path p, Path... others){
        for(Path other:others){
            String msg = startEndWithMsg(shouldMatch, true, p, other);
            assertThat(p.startsWith(other)).describedAs(msg).isEqualTo(shouldMatch);
        }
    }

    private static void checkStartsWith(boolean shouldMatch, Path p, String... prefixes) {
        for (String prefix : prefixes) {
            String msg = startEndWithMsg(shouldMatch, true, p, prefix);
            assertThat(p.startsWith(prefix)).describedAs(msg).isEqualTo(shouldMatch);
        }
    }

    private static void checkEndsWith(boolean shouldMatch, Path p, Path... others){
        for(Path other:others){
            String msg = startEndWithMsg(shouldMatch, false, p, other);
            assertThat(p.endsWith(other)).isEqualTo(shouldMatch);
        }
    }
    private static void checkEndsWith(boolean shouldMatch, Path p, String... suffixes){
        for(String suffix:suffixes){
            String msg = startEndWithMsg(shouldMatch, false, p, suffix);
            assertThat(p.endsWith(suffix)).isEqualTo(shouldMatch);
        }
    }

    private static String startEndWithMsg(boolean shouldMatch, boolean isStart, Path p, Object other){
        return String.format("%s should %s %s with %s", p, shouldMatch ? "" : "not", isStart ? "start":"end", other);
    }

    @Test( expectedExceptions = ProviderMismatchException.class)
    public void startsWithWrongPathType(){
        createPath("/").startsWith(anotherPathType());
    }

    @Test( expectedExceptions = ProviderMismatchException.class)
    public void endsWithWrongPathType(){
        createPath("/").endsWith(anotherPathType());
    }

    @Test( expectedExceptions = ProviderMismatchException.class)
    public void resolveWithWrongPathType(){
        createPath("/").resolve(anotherPathType());
    }

    @Test( expectedExceptions = ProviderMismatchException.class)
    public void resolveSiblingWithWrongPathType(){
        createPath("/").resolve(anotherPathType());
    }

    @Test( expectedExceptions = ProviderMismatchException.class)
    public void relativizeWithWrongPathType(){
        createPath("/").relativize(anotherPathType());
    }

    @Test( expectedExceptions = ProviderMismatchException.class)
    public void compareToWithWrongPathType(){
        createPath("/").compareTo(anotherPathType());
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
        return result;
    }

    private static MemoryFileSystem createFs() {
        MemoryFileSystemProvider provider = new MemoryFileSystemProvider();
        return MemoryFileSystem.builder(provider).build();
    }

    private static Path anotherPathType(){
        return new Path() {
            @Override
            public FileSystem getFileSystem() {
                return null;
            }

            @Override
            public boolean isAbsolute() {
                return false;
            }

            @Override
            public Path getRoot() {
                return null;
            }

            @Override
            public Path getFileName() {
                return null;
            }

            @Override
            public Path getParent() {
                return null;
            }

            @Override
            public int getNameCount() {
                return 0;
            }

            @Override
            public Path getName(int index) {
                return null;
            }

            @Override
            public Path subpath(int beginIndex, int endIndex) {
                return null;
            }

            @Override
            public boolean startsWith(Path other) {
                return false;
            }

            @Override
            public boolean startsWith(String other) {
                return false;
            }

            @Override
            public boolean endsWith(Path other) {
                return false;
            }

            @Override
            public boolean endsWith(String other) {
                return false;
            }

            @Override
            public Path normalize() {
                return null;
            }

            @Override
            public Path resolve(Path other) {
                return null;
            }

            @Override
            public Path resolve(String other) {
                return null;
            }

            @Override
            public Path resolveSibling(Path other) {
                return null;
            }

            @Override
            public Path resolveSibling(String other) {
                return null;
            }

            @Override
            public Path relativize(Path other) {
                return null;
            }

            @Override
            public URI toUri() {
                return null;
            }

            @Override
            public Path toAbsolutePath() {
                return null;
            }

            @Override
            public Path toRealPath(LinkOption... options) throws IOException {
                return null;
            }

            @Override
            public File toFile() {
                return null;
            }

            @Override
            public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
                return null;
            }

            @Override
            public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
                return null;
            }

            @Override
            public Iterator<Path> iterator() {
                return null;
            }

            @Override
            public int compareTo(Path other) {
                return 0;
            }
        };
    }
}

