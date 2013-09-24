package sylvain.juge.memoryfs;

import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

public class MemoryFileSystemTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullProviderNotAllowed() {
        MemoryFileSystem.builder(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullIdNotAllowed() {
        MemoryFileSystem
                .builder(newProvider())
                .id(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void negativeCapacityNotAllowed() {
        MemoryFileSystem
                .builder(newProvider())
                .capacity(-1);
    }

    @Test
    public void defaultId() {
        MemoryFileSystemProvider provider = newProvider();
        MemoryFileSystem fs = MemoryFileSystem
                .builder(provider)
                .build();
        assertThat(fs.getId()).isEqualTo("");
    }

    @Test
    public void buildThroughBuilderWithExplicitId() {
        MemoryFileSystem fs = MemoryFileSystem
                .builder(newProvider())
                .id("id")
                .build();
        assertThat(fs.getId()).isEqualTo("id");
    }

    @Test
    public void buildThroughUriWithExplicitIdInUri() throws IOException {
        URI uri = URI.create("memory:/fsId");
        MemoryFileSystem fs = (MemoryFileSystem) newProvider().newFileSystem(uri, null);
        assertThat(fs.getId()).isEqualTo("fsId");
    }

    @Test
    public void buildThroughUriWithoutExplicitId() throws IOException {
        URI uri = URI.create("memory:/");
        MemoryFileSystem fs = (MemoryFileSystem)newProvider().newFileSystem(uri, null);
        assertThat(fs.getId()).isEqualTo("");
    }

    @Test
    public void fsRegistrationInProvider() throws IOException {
        MemoryFileSystemProvider provider = newProvider();
        assertThat(provider.registeredFileSystems()).isEmpty();

        MemoryFileSystem fs = MemoryFileSystem.builder(provider).build();
        assertThat(provider.getFileSystem(URI.create("memory:/"))).isSameAs(fs);
        assertThat(provider.registeredFileSystems().get("")).isSameAs(fs);

        fs.close();

        assertThat(provider.registeredFileSystems()).isEmpty();
    }

    // TODO : create FS through Path ?
    // TODO : create FS with URI and with explicit parameters
    // TODO : test for concurrent access on open/close state

    @Test
    public void createFsWithCapacity() throws IOException {

        int capacity = 100;

        try (FileSystem fs = MemoryFileSystem
                .builder(newProvider())
                .capacity(capacity)
                .build()) {
            assertThat(fs.getFileStores()).hasSize(1);
            for (FileStore store : fs.getFileStores()) {
                assertThat(store.name()).isEqualTo("");
                assertThat(store.getTotalSpace()).isEqualTo(capacity);
                assertThat(store.getUsableSpace()).isEqualTo(capacity);
                assertThat(store.getUnallocatedSpace()).isEqualTo(capacity);
            }
        }
    }

    // TODO : test that creating a readonly filesystem only contains readonly filestores
    // TODO : create a filesystem with mixex readonly/readwrite stores
    // TODO : allow to create runtime-configurable stores ?
    // -> otherwise creating empty-read-only filestores seems quite useless

    // TODO : multiple filestores in the same fs have different IDs
    // when there is a single store, there is no need to explicitely provide its name

    @Test
    public void buildWithDefaultValues() throws IOException {
        MemoryFileSystem fs = MemoryFileSystem
                .builder(newProvider())
                .build();

        assertThat(fs.getId()).isEqualTo("");

        // singlefile store by default or size zero
        assertThat(fs.getFileStores()).hasSize(1);
        for (FileStore store : fs.getFileStores()) {
            assertThat(store.getTotalSpace()).isEqualTo(0);
            assertThat(store.getUnallocatedSpace()).isEqualTo(0);
            assertThat(store.getUsableSpace()).isEqualTo(0);
        }
    }

    @Test
    public void getPathFromParts() {
        MemoryFileSystem fs = MemoryFileSystem.builder(newProvider()).build();
        assertThat(fs.getPath("/absoluteSingle")).isEqualTo(MemoryPath.create(fs,"/absoluteSingle"));
        assertThat(fs.getPath("relative")).isEqualTo(MemoryPath.create(fs,"relative"));

        assertThat(fs.getPath("/a","path")).isEqualTo(MemoryPath.create(fs, "/a/path"));
        assertThat(fs.getPath("a","relative")).isEqualTo(MemoryPath.create(fs,"a/relative"));

        assertThat(fs.getPath("a/b","c/d","e")).isEqualTo(MemoryPath.create(fs,"a/b/c/d/e"));

    }

    @Test
    public void rootPathIsTheOnlyRoot(){
        // with or without id, the fs root remains the same
        checkRootDirectories(MemoryFileSystem.builder(newProvider()).build(), "/");
        checkRootDirectories(MemoryFileSystem.builder(newProvider()).id("id").build(), "/");
    }

    private static void checkRootDirectories(MemoryFileSystem fs, String root, String... expectedSubPaths){
        Path[] subPaths = new Path[expectedSubPaths.length];
        for (int i=0;i<expectedSubPaths.length;i++) {
            subPaths[i] = MemoryPath.create(fs, expectedSubPaths[i]);
        }
        assertThat(fs.getRootDirectories()).containsOnly(MemoryPath.create(fs, root));

        // TODO
        // if no subfolder expected, ensure that root folder is empty
        // check root subfolders
        // assertThat(fs.getRootDirectories()).containsOnly(expectedPaths.toArray(new Path[expectedPaths.size()]));
    }

    public void getRootDirectories() {
        // root directory is always the root folder
        // however, contents of the root folder changes depending on fs content and structure
        // 1 store : all folders in this store are at the fs root
        // 1 or more store : 1 folder per store at the fs root
    }

    @Test(enabled = false)
    public void getPathMatcher() {

        throw new RuntimeException("TODO : implement getPathMatcher");
    }

    private static MemoryFileSystemProvider newProvider() {
        return new MemoryFileSystemProvider();
    }

}
