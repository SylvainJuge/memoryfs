package sylvain.juge.memoryfs;

import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.ServiceLoader;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Fail.fail;

public class MemoryFileSystemProviderTest {

    private static final HashMap<String, Object> EMPTY_OPTIONS = new HashMap<>();

    private static final URI DUMMY_MEMORY_URI = URI.create("memory://dummy");

    // TODO : see how to use fs only by using a path that uses "memory" scheme

    @Test
    public void loadThroughServiceLoader() {
        FileSystemProvider provider = getNewProvider();
        assertThat(provider).isInstanceOf(MemoryFileSystemProvider.class);
    }

    @Test
    public void loadThroughFileSystemsAndUri() throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(DUMMY_MEMORY_URI, EMPTY_OPTIONS)) {
            assertThat(fs).isInstanceOf(MemoryFileSystem.class);
            assertThat(FileSystems.getFileSystem(DUMMY_MEMORY_URI)).isSameAs(fs);
        }
    }

    @Test
    public void samePathPrefixPointsToSameFileSystem() {
        Path path1 = Paths.get(URI.create("memory://dummy/1"));
        Path path2 = Paths.get(URI.create("memory://dummy/2"));
        // both paths must point to same FS instance
        assertThat(path1.getFileSystem()).isSameAs(path2.getFileSystem());
    }

    @Test(expectedExceptions = {FileSystemAlreadyExistsException.class})
    public void createDuplicateFileSystem() throws IOException {
        FileSystem fs1 = null;
        FileSystem fs2 = null;
        try {
            fs1 = FileSystems.newFileSystem(DUMMY_MEMORY_URI, EMPTY_OPTIONS);
            fs2 = FileSystems.newFileSystem(DUMMY_MEMORY_URI, EMPTY_OPTIONS);
        } finally {
            closeQuietly(fs1);
            closeQuietly(fs2);
        }
        fail("should not be able to create two FS with same URI");
    }

    private static void closeQuietly(FileSystem fs) {
        if (null == fs) {
            return;
        }
        try {
            fs.close();
        } catch (IOException e) {
            // silently ignored
        }
    }

    //@Test
    public void providerShouldBeLoadedOnce() {
        // multiple calls to service loader should return the same instance

        // TODO : it seems that service loader creates a new instance at each call
        // however, if fs code uses a for Path resolution, it may be perfectly fine
        // -> we must check that getting instances of this filesystem are identical through paths
        assertThat(getNewProvider()).isSameAs(getNewProvider());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void createFileSystemWithoutCorrectScheme() throws IOException {
        createFileSystem(getNewProvider(), URI.create("withoutMemoryScheme"));
        fail("should not be able to create such FS");
    }

    @Test
    public void checkProviderScheme() {
        FileSystemProvider provider = getNewProvider();
        assertThat(provider).isNotNull();
        assertThat(provider.getScheme()).isEqualTo("memory");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void createFsWithInvalidSchemeInPath() throws IOException {
        FileSystemProvider provider = getNewProvider();
        Path invalidPath = Paths.get("invalid://dummy");
        createFileSystem(provider, invalidPath);
    }

    @Test
    public void createFsFromPathAddMemoryScheme() throws IOException {
        Path pathWithoutScheme = Paths.get("anything");
        FileSystemProvider provider = getNewProvider();
        boolean ok;
        try (FileSystem fs = createFileSystem(provider, pathWithoutScheme)) {
            assertThat(fs).isNotNull();
            assertThat(fs).isSameAs(provider.getFileSystem(URI.create("memory://anything")));
            ok = true;
        }
        assertThat(ok).isTrue();
    }

    @Test
    public void openCloseReopen() throws IOException {
        FileSystemProvider provider = getNewProvider();

        Path path = Paths.get("dummy");

        FileSystem fs = createFileSystem(provider, path);
        assertThat(fs.isOpen()).isTrue();

        // we should not be able to create FS with same path
        FileSystemAlreadyExistsException expected = null;
        try {
            createFileSystem(provider, path);
        } catch (FileSystemAlreadyExistsException e) {
            expected = e;
        }
        assertThat(expected).isInstanceOf(FileSystemAlreadyExistsException.class);

        // once closed, we should be able to create again
        fs.close();
        assertThat(fs.isOpen()).isFalse();

        try (FileSystem reOpendedFs = createFileSystem(provider, path)) {
            assertThat(reOpendedFs).isNotNull();
            fs = reOpendedFs;
        }
        assertThat(fs.isOpen()).isFalse();

    }

    @Test
    public void createFileSystemInstanceAndGetByUri() throws IOException {
        FileSystemProvider provider = getNewProvider();
        try (FileSystem fs = createFileSystem(provider, DUMMY_MEMORY_URI)) {
            assertThat(fs).isNotNull();
            assertThat(provider.getFileSystem(DUMMY_MEMORY_URI)).isSameAs(fs);
        }
    }

    @Test
    public void defaultFileSystemProperties() throws IOException {
        try (FileSystem fs = createFileSystem(getNewProvider(), DUMMY_MEMORY_URI)) {
            assertThat(fs.isOpen()).isTrue();
            assertThat(fs.isReadOnly()).isFalse();
            assertThat(fs.getSeparator()).isNotEmpty();
        }
    }

    // TODO
    // - see how filesytem is used, especially when newFileSystem() is called when creating files in it
    // -> if only 1 instance is used, we can easily control ro/rw state of such fs
    //
    // - when storage is implemented, see how we can use ByteBuffer to allocate storage out of heap
    //
    // - misc : create a "/dev/null" fs where we always write
    // - misc : allow to control ro/rw state at runtime (allow to test that app does not write when not required)


    // tests to write
    // - try to get a fs instance without creating it beforehand : myst throw exception

    @Test
    public void newProviderReturnsNewInstanceOnEachCall() {
        assertThat(getNewProvider()).isNotSameAs(getNewProvider());
    }

    private static FileSystemProvider getNewProvider() {
        ServiceLoader<FileSystemProvider> loader = ServiceLoader.load(FileSystemProvider.class);
        for (FileSystemProvider provider : loader) {
            if (provider instanceof MemoryFileSystemProvider) {
                return MemoryFileSystemProvider.class.cast(provider);
            }
        }
        return null;
    }

    private static FileSystem createFileSystem(FileSystemProvider provider, Path path) throws IOException {
        return provider.newFileSystem(path, EMPTY_OPTIONS);
    }

    private static FileSystem createFileSystem(FileSystemProvider provider, URI uri) throws IOException {
        return provider.newFileSystem(uri, EMPTY_OPTIONS);
    }
}
