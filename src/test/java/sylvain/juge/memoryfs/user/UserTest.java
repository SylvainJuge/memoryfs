package sylvain.juge.memoryfs.user;

import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.StandardOpenOption.READ;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * This test class represents library use cases.
 * It has only runtime dependencies on memoryfs.
 */
public class UserTest {

    @Test
    public void createFsAndWriteFile() throws IOException {
        URI uri = URI.create("memory:/");

        FileSystem fs = FileSystems.newFileSystem(uri, null);
        assertThat(fs).isNotNull();

        Path file = fs.getPath("file");

        // SeekableByteChannel does not have any static constructor
        // just like FileChannel.open(...)

        SeekableByteChannel writeChannel = fs.provider().newByteChannel(file, openOptions(WRITE, CREATE));
        writeChannel.write(ByteBuffer.wrap(new byte[]{1, 2, 3}));
        // TODO close channel before read

        SeekableByteChannel readChannel = fs.provider().newByteChannel(file, openOptions(READ));
        byte[] readBuffer = new byte[3];
        readChannel.read(ByteBuffer.wrap(readBuffer));
        assertThat(readBuffer).isEqualTo(new byte[]{1, 2, 3});
        // TODO close channel after read

        // TODO close fs once we don't need it anymore

    }

    @Test(enabled = false)
    public void fileTree(){
        // create non-trivial directory/file structure
        // then use directory stream to print it's layout
        // check result on ascii art
    }

    private static Set<StandardOpenOption> openOptions(StandardOpenOption... options){
        return new HashSet<>(Arrays.asList(options));
    }
}
