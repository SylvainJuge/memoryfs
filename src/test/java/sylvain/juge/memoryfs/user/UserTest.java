package sylvain.juge.memoryfs.user;

import org.testng.annotations.Test;
import sylvain.juge.memoryfs.util.ProjectPathFinder;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

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
        Sha1FileVisitor originalFilesSha1 = new Sha1FileVisitor();
        Files.walkFileTree(toCopy, originalFilesSha1);

        assertThat(toCopyList.getList()).isNotEmpty();

        URI uri = URI.create("memory:/");

        try (FileSystem fs = FileSystems.newFileSystem(uri, null)) {

            Path copyTarget = fs.getPath("/copy");

            // Files.copy(...) only copies one folder (or file), and is not recursive
            // thus we have to write a custom (but rather simple) file visitor.
            Files.walkFileTree(toCopy, new CopyVisitor(toCopy, copyTarget));

            ListPathVisitor copyList = new ListPathVisitor();
            Files.walkFileTree(copyTarget, copyList);

            assertThat(copyList.getList()).hasSameSizeAs(toCopyList.getList());

            Sha1FileVisitor copyFilesSha1 = new Sha1FileVisitor();
            Files.walkFileTree(copyTarget, copyFilesSha1);
            assertThat(copyFilesSha1.getHashes()).isEqualTo(originalFilesSha1.getHashes());
        }

    }

    private static class CopyVisitor extends SimpleFileVisitor<Path> {

        private final Path src;
        private final Path dst;

        public CopyVisitor(Path src, Path dst) {
            this.src = src;
            this.dst = dst;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            return copy(dir);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            return copy(file);
        }

        private FileVisitResult copy(Path item) throws IOException {
            Path srcItem = src.relativize(item);
            Path targetPath = dst;
            for (int i = 0; i < srcItem.getNameCount(); i++) {
                targetPath = targetPath.resolve(srcItem.getName(i).toString());
            }
            Files.copy(item, targetPath);
            return FileVisitResult.CONTINUE;
        }
    }

    private static class ListPathVisitor extends SimpleFileVisitor<Path> {

        private final List<Path> paths;

        private ListPathVisitor() {
            this.paths = new ArrayList<>();
        }

        private List<Path> getList() {
            return paths;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            paths.add(dir);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            paths.add(file);
            return FileVisitResult.CONTINUE;
        }

    }

    private static class Sha1FileVisitor extends SimpleFileVisitor<Path> {
        private final List<String> hashes;
        private final List<Path> paths;
        private Sha1FileVisitor(){
            hashes = new ArrayList<>();
            paths = new ArrayList<>();
        }

        public List<String> getHashes(){
            return hashes;
        }

        public List<Path> getPaths(){
            return paths;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

            SeekableByteChannel channel = Files.newByteChannel(file, StandardOpenOption.READ);
            ByteBuffer readBuffer = ByteBuffer.wrap(new byte[8048]);

            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            int n;
            do {
                readBuffer.clear();
                n = channel.read(readBuffer);
                if( 0 < n ){
                    digest.update(readBuffer);
                }
            } while( 0 < n );

            StringBuilder sb = new StringBuilder();
            for(byte b:digest.digest()){
                sb.append(String.format("%02x",b));
            }
            hashes.add(sb.toString());
            paths.add(file);

            return FileVisitResult.CONTINUE;
        }
    }
}
