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
        new MemoryFileSystem(null, "");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullIdNotAllowed() {
        MemoryFileSystemProvider provider = new MemoryFileSystemProvider(); // TODO : use mock
        new MemoryFileSystem(provider, null);
    }


    @Test
    public void getDefaultFsCapacityNumbers() throws IOException {
        for (FileStore store : FileSystems.getDefault().getFileStores()) {
            System.out.println("name " + store.name());
            System.out.println("total space " + store.getTotalSpace()); // file store total space (exact)
            System.out.println("usable space " + store.getUsableSpace()); // accessible to jvm (hint, not reliable due to pending I/O)
            System.out.println("unnalocated space " + store.getUnallocatedSpace()); // hint, not really reliable due to pending I/0
            // actual size should be provided through attributes
        }
        //fail("end of dummy test");
    }

    @Test
    public void defaultFsUriToFile() {
        Path path = FileSystems.getDefault().getPath("item");
        System.out.println(path.getClass());
        System.out.println(path.toAbsolutePath());
        System.out.println(path.toUri());

        // TODO : I need to implement MemoryFilePath (and see how much we can reuse from existing implementations)
        // => nothing is reusable without depending on sun proprietary packages
        fail("test");
    }

    @Test
    public void fileStoreCapacity() throws IOException {
        FileSystem fs = FileSystems.newFileSystem(URI.create("memory:///"), EMPTY_OPTIONS);
        assertThat(fs.getFileStores()).hasSize(1);
        for (FileStore store : fs.getFileStores()) {
            assertThat(store.getTotalSpace()).isEqualTo(10);
            assertThat(store.getUnallocatedSpace()).isEqualTo(42);
            assertThat(store.getUsableSpace()).isEqualTo(42);
        }

    }
}
