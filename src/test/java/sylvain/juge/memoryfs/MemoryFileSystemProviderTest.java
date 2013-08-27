package sylvain.juge.memoryfs;

import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.List;
import java.util.ServiceLoader;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Fail.fail;

public class MemoryFileSystemProviderTest {

    private static final HashMap<String, Object> EMPTY_OPTIONS = new HashMap<>();

    private static final URI DUMMY_MEMORY_URI = URI.create("memory://dummy");

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
    public void samePathPrefixResolvesToSameFileSystem() {
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

    @Test
    public void newProviderReturnsNewInstanceOnEachCall() {
        assertThat(getNewProvider()).isNotSameAs(getNewProvider());
    }

    @Test
    public void fileSystemProviderStaticallyAvailable(){
        boolean found = false;
        List<FileSystemProvider> providers = FileSystemProvider.installedProviders();
        for (FileSystemProvider provider : providers) {
            if( MemoryFileSystemProvider.class.equals( provider.getClass())){
                found = true;
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    public void getFileStoreFromPath(){
        Path path = Paths.get(DUMMY_MEMORY_URI);
        assertThat(path).isNotNull();
        assertThat(path.getFileSystem()).isInstanceOf(MemoryFileSystem.class);
        Iterable<FileStore> stores = path.getFileSystem().getFileStores();
        int count = 0;
        for(FileStore store : stores){
            assertThat(store).isNotNull();
            count++;
        }
        assertThat(count).isEqualTo(1);
    }

    static FileSystemProvider getNewProvider() {
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
}
