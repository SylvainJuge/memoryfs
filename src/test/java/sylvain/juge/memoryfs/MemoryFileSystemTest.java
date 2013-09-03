package sylvain.juge.memoryfs;

import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Fail.fail;

public class MemoryFileSystemTest {


    private static final Map<String, ?> EMPTY_OPTIONS = new HashMap<>();

    // TODO : FileStore : difference between unallocated space and usable space ?

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullProviderNotAllowed() {
        new MemoryFileSystem(null, "", 0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullIdNotAllowed() {
        createFs(null, 0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void negativeCapacityNotAllowed() {
        createFs("", -1);
    }

    // TODO : test for concurrent access on open/close state

    @Test(enabled = false) // failing test disabled yet
    public void createFsWithCapacity() throws IOException {

        // TODO : allow to pass capacity when creating filesystem
        // - either through map of options, or through creation uri
        // - providing a URI builder on impl may be convenient
        int capacity = 100;
        try (FileSystem fs = createFs("", capacity)) {
            assertThat(fs.getFileStores()).hasSize(1);
            for (FileStore store : fs.getFileStores()) {
                assertThat(store.name()).isEqualTo("");
                assertThat(store.getTotalSpace()).isEqualTo(capacity);
                assertThat(store.getUsableSpace()).isEqualTo(capacity);
                assertThat(store.getUnallocatedSpace()).isEqualTo(capacity);
            }
        }
    }

    private MemoryFileSystem createFs(String id, long capacity) {
        MemoryFileSystemProvider provider = new MemoryFileSystemProvider();
        return new MemoryFileSystem(provider, id, capacity);
    }

}
