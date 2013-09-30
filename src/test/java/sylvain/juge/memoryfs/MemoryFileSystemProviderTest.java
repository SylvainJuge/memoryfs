package sylvain.juge.memoryfs;

import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Fail.fail;

public class MemoryFileSystemProviderTest {

    private static final URI DUMMY_MEMORY = URI.create("memory:/");

    // handle the static state of file system manager

    // TODO : check creating FS instances with capacity

    // TODO : see zip fs jdk demo
    // Path jar = Paths.get("jarfile.jar");
    // FileSystem fs = FileSystems.newFileSystem(jar,...);
    // --> fs path is resolved at runtime
    // -> how does the provider responds to such Path ??
    // --> there is no scheme provided, and FS type is not explicit
    // -> see when FsProvider.getPath(URI uri) and FsProvider.uriToPath(..) are called
    // ZipFsProvider seems to contain explanation about how it works for "legacy URI syntax"used in demo

    @AfterTest
    public void checkStaticFileSystemProviderState() {
        // there is a static provider instance that may be used by tests when creating fs instance without explicit
        // references to provider itself.
        //
        // we have to make sure that tests properly close associated filesystems
        // otherwise fs provider instance may contain stale fs references which creates side effects.
        checkNoFileSystemsLeftOpen(getStaticProvider());
    }

    @Test
    public void loadThroughServiceLoader() {
        MemoryFileSystemProvider provider = getNewProvider();
        assertThat(provider).isInstanceOf(MemoryFileSystemProvider.class);
        checkNoFileSystemsLeftOpen(provider);
    }

    @Test
    public void loadThroughFileSystemsAndUri() throws IOException {
        try (FileSystem fs = staticCreateAndGet(DUMMY_MEMORY)) {
            // most of tests are done in create & get method
            assertThat(fs).isNotNull();
        }
    }

    @Test(expectedExceptions = FileSystemNotFoundException.class)
    public void getBeforeCreateThrowsException() {
        FileSystems.getFileSystem(URI.create("memory:/"));
    }

    // non-static properties of provider : we use a dedicated instance for each test

    @Test
    public void fileSystemIdentifiedByFirstElementOfPath() throws IOException {
        MemoryFileSystemProvider provider = getNewProvider();

        FileSystem fs1 = createAndGet(provider, URI.create("memory:/fs1"));
        FileSystem fs2 = createAndGet(provider, URI.create("memory:/fs2"));

        Path path1 = provider.getPath(URI.create("memory:/fs1/dummy/1"));
        Path path2 = provider.getPath(URI.create("memory:/fs1/dummy/2"));
        Path path3 = provider.getPath(URI.create("memory:/fs2/dummy/2"));

        // both paths must point to same FS instance
        assertThat(path1.getFileSystem())
                .isInstanceOf(MemoryFileSystem.class)
                .isSameAs(path2.getFileSystem())
                .isSameAs(fs1);

        // 3rd path does not use the same fs instance
        assertThat(path3.getFileSystem())
                .isInstanceOf(MemoryFileSystem.class)
                .isNotSameAs(fs1)
                .isSameAs(fs2);

    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void createFileSystemWithoutCorrectScheme() throws IOException {
        createAndGet(getNewProvider(), URI.create("withoutMemoryScheme"));
    }

    @Test
    public void checkProviderScheme() {
        FileSystemProvider provider = getNewProvider();
        assertThat(provider).isNotNull();
        assertThat(provider.getScheme()).isEqualTo("memory");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void createFsWithInvalidSchemeInUri() throws IOException {
        createAndGet(getNewProvider(), URI.create("invalid://dummy"));
    }

    @Test(expectedExceptions = FileSystemAlreadyExistsException.class)
    public void shouldNotAllowDuplicates() throws IOException {
        MemoryFileSystemProvider provider = getNewProvider();
        FileSystem fs = createAndGet(provider, DUMMY_MEMORY);
        assertThat(fs.isOpen()).isTrue();
        createAndGet(provider, DUMMY_MEMORY);
    }

    @Test
    public void openCloseReopen() throws IOException {
        MemoryFileSystemProvider provider = getNewProvider();

        URI uri = URI.create("memory:/dummy/");

        FileSystem fs = createAndGet(provider, uri);
        assertThat(fs.isOpen()).isTrue();
        fs.close();
        assertThat(fs.isOpen()).isFalse();
        FileSystem recreatedFs = createAndGet(provider, uri);
        recreatedFs.close();
        checkNoFileSystemsLeftOpen(provider);
    }

    @Test
    public void createFileSystemInstanceAndGetByUri() throws IOException {
        MemoryFileSystemProvider provider = getNewProvider();
        checkNoFileSystemsLeftOpen(provider);
        URI uri = URI.create("memory:/dummy/");
        try (FileSystem fs = createAndGet(provider, uri)) {
            assertThat(fs).isNotNull();
            // nothing much to do, since we have equivalent tests in createAndGet
        }
        checkNoFileSystemsLeftOpen(provider);
    }

    @Test
    public void defaultFileSystemProperties() throws IOException {
        try (FileSystem fs = createAndGet(getNewProvider(), URI.create("memory:/dummy/"))) {
            assertThat(fs.isOpen()).isTrue();
            assertThat(fs.isReadOnly()).isFalse();
            assertThat(fs.getSeparator()).isNotEmpty();
        }
    }

    @Test
    public void newProviderReturnsNewInstanceOnEachCall() {
        assertThat(getNewProvider()).isNotSameAs(getNewProvider());
    }

    @Test
    public void memoryFileSystemProviderStaticallyAvailable() {
        boolean found = false;
        List<FileSystemProvider> providers = FileSystemProvider.installedProviders();
        for (FileSystemProvider provider : providers) {
            if (MemoryFileSystemProvider.class.equals(provider.getClass())) {
                found = true;
            }
        }
        assertThat(found).isTrue();
    }

    @Test(enabled = false) // TODO : disabled yet since file store is not implemented
    // TODO : move this test to FileSystemTest
    public void getFileStoreFromPath() throws IOException {
        URI uri = URI.create("memory://dummy/");
        try (FileSystem fs = staticCreateAndGet(uri)) {
            Path path = Paths.get(uri);
            assertThat(path).isNotNull();
            assertThat(path.getFileSystem())
                    .isSameAs(fs);
            Iterable<FileStore> stores = path.getFileSystem().getFileStores();
            assertThat(stores).hasSize(1);
            for (FileStore store : stores) {
                assertThat(store).isNotNull();
            }
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void getFileSystemIdFromUriWithInvalidUriScheme() {
        createAndGet(getNewProvider(), URI.create("notMemory://dummy/"));
    }

    @Test
    public void suffixInFileSystemUriIsIgnored() {
        // only the 1st non empty element of path is used as ID
        MemoryFileSystem fs = createAndGet(getNewProvider(), URI.create("memory:/fs1/a"));
        assertThat(fs.getId()).isEqualTo("fs1");
    }

    @Test
    public void getFileSystemIdFromValidUri() {
        // fs ID must be the 1st non-empty element in path (if it exists), or an empty string

        MemoryFileSystem fs1 = createAndGet(getNewProvider(), URI.create("memory:///"));
        assertThat(fs1.getId()).isEqualTo("");

        MemoryFileSystem fs2 = createAndGet(getNewProvider(), URI.create("memory:/id/"));
        assertThat(fs2.getId()).isEqualTo("id");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void hostNameInUriShouldNotBeAllowed() {
        createAndGet(getNewProvider(), URI.create("memory://id/"));
    }

    @Test
    public void emptyFolderDirectoryStream() throws IOException {
        MemoryFileSystemProvider provider = getNewProvider();
        MemoryFileSystem fs = MemoryFileSystem.builder(provider).build();
        Path root = MemoryPath.create(fs, "/");

        DirectoryStream<Path> stream = provider.newDirectoryStream(root, new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                return true;
            }
        });
        assertThat(stream).isEmpty();
    }

    @Test
    public void rootDirectoryAttributes() throws IOException {
        MemoryFileSystemProvider provider = getNewProvider();
        MemoryFileSystem fs = MemoryFileSystem.builder(provider).build();
        Path root = MemoryPath.create(fs, "/");

        BasicFileAttributes a = provider.readAttributes(root, BasicFileAttributes.class);
        checkDirectoryAttributes(a);

    }

    private static void checkDirectoryAttributes(BasicFileAttributes a){
        assertThat(a).isNotNull();
        assertThat(a.isDirectory()).describedAs("must be a directory").isTrue();
        assertThat(a.isRegularFile()).describedAs("directory is not a regular file").isFalse();
        assertThat(a.isSymbolicLink()).describedAs("directory is not a symbolic link").isFalse();
        assertThat(a.isOther()).describedAs("directory is not other").isFalse();
        assertThat(a.size()).describedAs("directory does not have size").isEqualTo(0);
    }

    private static String findFirstCommonId(Set<String> before, Set<String> after) {
        for (String id : after) {
            if (!before.contains(id)) {
                return id;
            }
        }
        throw new IllegalArgumentException("no distinct element in after set");
    }

    private static FileSystem staticCreateAndGet(URI uri) {
        FileSystem fs = null;
        try {
            fs = FileSystems.newFileSystem(uri, null);
        } catch (IOException e) {
            fail("unexpected io exception", e);
        }
        assertThat(FileSystems.getFileSystem(uri)).isSameAs(fs);
        assertThat(fs).isInstanceOf(MemoryFileSystem.class);
        return fs;
    }

    // create fs by using a specific provider instance
    private static MemoryFileSystem createAndGet(MemoryFileSystemProvider provider, URI uri) {
        // try to create file with specific uri, and if it succeeds,
        // we check that this filesystem is properly registered with expected id

        Set<String> beforeIds = new HashSet<>(provider.registeredFileSystems().keySet());
        String id;
        FileSystem fs = null;
        try {
            fs = provider.newFileSystem(uri, null);
        } catch (IOException e) {
            fail("unexpected io exception", e);
        }
        assertThat(fs).isInstanceOf(MemoryFileSystem.class);

        Set<String> afterIds = provider.registeredFileSystems().keySet();
        assertThat(afterIds).hasSize(beforeIds.size() + 1);
        id = findFirstCommonId(beforeIds, afterIds);
        assertThat(provider.registeredFileSystems().get(id)).isSameAs(fs);
        assertThat(provider.getFileSystem(uri)).isSameAs(fs);

        return (MemoryFileSystem) fs;
    }


    static MemoryFileSystemProvider getNewProvider() {
        ServiceLoader<FileSystemProvider> loader = ServiceLoader.load(FileSystemProvider.class);
        for (FileSystemProvider provider : loader) {
            if (provider instanceof MemoryFileSystemProvider) {
                return MemoryFileSystemProvider.class.cast(provider);
            }
        }
        return null;
    }

    private static MemoryFileSystemProvider getStaticProvider() {
        List<FileSystemProvider> providers = FileSystemProvider.installedProviders();
        for (FileSystemProvider provider : providers) {
            if (provider instanceof MemoryFileSystemProvider) {
                return (MemoryFileSystemProvider) provider;
            }
        }
        return null;
    }

    private static void checkNoFileSystemsLeftOpen(MemoryFileSystemProvider provider) {
        assertThat(provider.registeredFileSystems().isEmpty());
    }

}
