package sylvain.juge.memoryfs;

import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;

import static org.fest.assertions.api.Assertions.assertThat;

public class MemoryFileSystemTest {

    // TODO : FileStore : difference between unallocated space and usable space ?

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
    public void defaultIdInUri() {
        MemoryFileSystemProvider provider = newProvider();
        MemoryFileSystem fs = MemoryFileSystem
                .builder(provider)
                .build();
        assertThat(fs.getUri()).isEqualTo(URI.create("memory:/"));
    }

    @Test
    public void buildThroughBuilderWithExplicitIdInUri() {
        MemoryFileSystemProvider provider = newProvider();
        MemoryFileSystem fs = MemoryFileSystem
                .builder(provider)
                .id("id")
                .build();
        assertThat(fs.getUri()).isEqualTo(URI.create("memory:/id"));
    }

    @Test
    public void buildThroughUriWithExplicitIdInUri() throws IOException {
        MemoryFileSystemProvider provider = newProvider();
        URI uri = URI.create("memory:/fsId");
        MemoryFileSystem fs = (MemoryFileSystem) provider.newFileSystem(uri, null);
        assertThat(fs.getUri()).isEqualTo(uri);
    }

    // TODO : create FS through Path ?
    // TODO : create FS with URI and with explicit parameters
    // TODO : test for concurrent access on open/close state

    @Test(enabled = false) // failing test disabled yet
    public void createFsWithCapacity() throws IOException {

        // TODO : allow to pass capacity when creating filesystem
        // - either through map of options, or through creation uri
        // - providing a URI builder on impl may be convenient
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

        // default URI
        assertThat(fs.getUri()).isEqualTo(URI.create("memory:///"));

        // singlefile store by default or size zero
        assertThat(fs.getFileStores()).hasSize(1);
        for (FileStore store : fs.getFileStores()) {
            assertThat(store.getTotalSpace()).isEqualTo(0);
            assertThat(store.getUnallocatedSpace()).isEqualTo(0);
            assertThat(store.getUsableSpace()).isEqualTo(0);
        }
    }

    private static MemoryFileSystemProvider newProvider() {
        return new MemoryFileSystemProvider();
    }

}
