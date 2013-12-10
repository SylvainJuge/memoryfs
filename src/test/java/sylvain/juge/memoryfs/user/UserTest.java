package sylvain.juge.memoryfs.user;

import org.testng.annotations.Test;
import sylvain.juge.fsutils.ProjectPathFinder;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

import static java.nio.file.StandardOpenOption.*;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * This test class represents library use cases.
 * It has only runtime dependencies on memoryfs.
 */
public class UserTest {

    @Test
    public void createFsAndWriteFileThroughChannel() throws IOException {
        URI uri = URI.create("memory:/");

        // Note : we use indirectly a single instance of memory fs provider.
        // Thus we share a single instance of fs provider (per fs type), and this one
        // allows multiple instances, thus we have to keep the place clean
        //
        // If this constraint is too restrictive, for example when we need to execute tests in parallel
        // we may add an option for a "detached" mode. However, in this case the user won't be able to
        // use static Files.xxx methods, but using direct access to memory fs classes allows it, at the
        // price of a compile-time dependency.
        //
        // Another option is to allow an optional thread-local in the
        // provider, thus ensuring that every thread only sees a single independant provider, tests will still have to
        // keep clean, but parallelism is allowed.

        try (FileSystem fs = FileSystems.newFileSystem(uri, null)) {
            assertThat(fs).isNotNull();

            Path file = fs.getPath("file");

            // SeekableByteChannel does not have any static constructor
            // just like FileChannel.open(...)

            SeekableByteChannel writeChannel = fs.provider().newByteChannel(file, EnumSet.of(WRITE, CREATE));
            writeChannel.write(ByteBuffer.wrap(new byte[]{1, 2, 3}));
            // TODO close channel before read

            SeekableByteChannel readChannel = fs.provider().newByteChannel(file, EnumSet.of(READ));
            byte[] readBuffer = new byte[3];
            readChannel.read(ByteBuffer.wrap(readBuffer));
            assertThat(readBuffer).isEqualTo(new byte[]{1, 2, 3});
            // TODO close channel after read

        }

    }

    //@Test
    public void sandbox() throws IOException {
        // test parameters :
        // - filesystem
        FileSystem fs = FileSystems.getDefault();

        Path path = fs.getPath("toCreate");
        assertThat(Files.exists(path))
                .isEqualTo(Files.exists(path.toAbsolutePath()))
                .isFalse();
        Path created = Files.createFile(path);

        assertThat(created).isEqualTo(path);
        assertThat(Files.isDirectory(path)).isFalse();

        // when creating a relative path, it is resolved against "current working dir"
        // => how to define such state in our memory implementation ?

        assertThat(Files.exists(path))
                .isEqualTo(Files.exists(path.toAbsolutePath()))
                .isTrue();


        // methods to test
        // Files.createFile()
        // Files.exists();

    }

    // TODO : make tests pass on default implementation to understand how Files.xxx methods should work
    // then, once the test suite covers everything, switch implementation to our memory impl
    // and make sure that implementation is compliant

    @Test
    public void createWithRelativePathConvertedToAbsolute() throws IOException {

        // issue #7
        URI uri = URI.create("memory:/");
        try (FileSystem fs = FileSystems.newFileSystem(uri, null)) {

            Path relativePath = fs.getPath("hello");
            Files.createDirectories(relativePath);
            assertThat(Files.exists(relativePath)).isTrue();
        }

    }

    @Test
    public void copyFromDefaultFsToMemoryFsUsingTreeWalking() throws IOException {
        // this test copies all files in src/main/java folder to a memoryfs instance

        ListPathVisitor toCopyList = new ListPathVisitor();

        Path toCopy = ProjectPathFinder.getFolder("memoryfs").resolve("src/main/java");
        Files.walkFileTree(toCopy, toCopyList);

        // store file sha1 in original folder to compare later with their copies
        FileDigestVisitor originalFilesSha1 = new FileDigestVisitor("SHA-1", 4096);
        Files.walkFileTree(toCopy, originalFilesSha1);

        assertThat(toCopyList.getList()).isNotEmpty();

        URI uri = URI.create("memory:/");

        try (FileSystem fs = FileSystems.newFileSystem(uri, null)) {

            Path copyTarget = fs.getPath("/copy");

            // Files.copy(...) only copies one folder (or file), and is not recursive
            // thus we have to write a custom (but rather simple) file visitor.
            Files.walkFileTree(toCopy, new CopyVisitor(toCopy, copyTarget));

            FileDigestVisitor copyFilesSha1 = new FileDigestVisitor("SHA-1", 4096);
            Files.walkFileTree(copyTarget, copyFilesSha1);

            List<FileDigestVisitor.FileHash> copy = copyFilesSha1.getResult();
            List<FileDigestVisitor.FileHash> original = originalFilesSha1.getResult();
            assertThat(copy).hasSameSizeAs(original);
            for (int i = 0; i < copy.size(); i++) {
                FileDigestVisitor.FileHash copyHash = copy.get(i);
                FileDigestVisitor.FileHash originalHash = original.get(i);
                assertThat(copyHash.getHash()).isEqualTo(originalHash.getHash());

                // TODO : we might even check that both paths are "equivalent"
                assertThat(copyHash.getPath().getFileName().toString()).isEqualTo(originalHash.getPath().getFileName().toString());

            }


        }

    }

}
