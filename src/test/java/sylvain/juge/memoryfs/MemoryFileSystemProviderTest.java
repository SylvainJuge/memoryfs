package sylvain.juge.memoryfs;

import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Fail.fail;

public class MemoryFileSystemProviderTest {

    // handle the static state of file system manager

    // TODO : check creating FS instances with capacity

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
        URI uri = URI.create("memory://fs/");
        try (FileSystem fs = staticCreateAndGet(uri)) {
            // most of tests are done in create & get method
            assertThat(fs).isNotNull();
        }
    }

    @Test(expectedExceptions = FileSystemNotFoundException.class)
    public void getBeforeCreateThrowsException(){
        FileSystems.getFileSystem(URI.create("memory:/"));
    }

    // non-static properties of provider : we use a dedicated instance for each test

    @Test
    public void sameHostPrefixResolvesToSameFileSystem() throws IOException {
        MemoryFileSystemProvider provider = getNewProvider();
        try (FileSystem fs = createAndGet(provider, URI.create("memory://host1/"))) {
            Path path1 = provider.getPath(URI.create("memory://host1/dummy/1"));
            Path path2 = provider.getPath(URI.create("memory://host1/dummy/2"));
            // both paths must point to same FS instance
            assertThat(path1.getFileSystem())
                    .isInstanceOf(MemoryFileSystem.class)
                    .isSameAs(path2.getFileSystem())
                    .isSameAs(fs);
        }
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
        URI uri = URI.create("memory:///");
        MemoryFileSystemProvider provider = getNewProvider();
        FileSystem fs = createAndGet(provider, uri);
        assertThat(fs.isOpen()).isTrue();
        createAndGet(provider, uri);
    }

    @Test
    public void openCloseReopen() throws IOException {
        MemoryFileSystemProvider provider = getNewProvider();

        URI uri = URI.create("memory://dummy/");

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
        URI uri = URI.create("memory://dummy/");
        try (FileSystem fs = createAndGet(provider, uri)) {
            assertThat(fs).isNotNull();
            // nothing much to do, since we have equivalent tests in createAndGet
        }
        checkNoFileSystemsLeftOpen(provider);
    }

    @Test
    public void defaultFileSystemProperties() throws IOException {
        try (FileSystem fs = createAndGet(getNewProvider(), URI.create("memory://dummy/"))) {
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

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void getFileSystemIdFromUriWithAnySuffix() {
        // only straight URIs are allowed as FS
        createAndGet(getNewProvider(), URI.create("memory://fs1/a"));
    }

    @Test
    public void getFileSystemIdFromValidUri() {
        // note : we don't care about cleaning up since we use dedicated provider instances
        createAndGet(getNewProvider(), URI.create("memory:///"));
        createAndGet(getNewProvider(), URI.create("memory://id/"));
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
    private static FileSystem createAndGet(MemoryFileSystemProvider provider, URI uri) {
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
        Set<String> afterIds = provider.registeredFileSystems().keySet();
        assertThat(afterIds).hasSize(beforeIds.size() + 1);
        id = findFirstCommonId(beforeIds, afterIds);
        assertThat(provider.registeredFileSystems().get(id)).isSameAs(fs);
        assertThat(provider.getFileSystem(uri)).isSameAs(fs);
        return fs;
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
