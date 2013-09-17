package sylvain.juge.memoryfs;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

public class MemorySeekableByteChannelTest {

    @Test
    public void buildEmptyChannel() throws IOException {
        create(0);
    }

    @Test
    public void buildNonEmptyChannel() {
        create(42);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void buildInvalidSizeChannel() {
        create(-1);
    }

    @Test
    public void openCloseChannel() throws IOException {
        MemorySeekableByteChannel c = create(0);
        assertThat(c.isOpen()).isTrue();
        c.close();
        assertThat(c.isOpen()).isFalse();
    }

    @Test(expectedExceptions = ClosedChannelException.class)
    public void closeTwice() throws IOException {
        MemorySeekableByteChannel c = create(0);
        c.close();
        c.close();
    }

    @Test(expectedExceptions = ClosedChannelException.class)
    public void readClosed() throws IOException {
        MemorySeekableByteChannel c = create(0);
        c.close();
        c.read(null);
    }

    @Test(expectedExceptions = ClosedChannelException.class)
    public void writeClosed() throws IOException {
        MemorySeekableByteChannel c = create(0);
        c.close();
        c.write(null);
    }

    private static MemorySeekableByteChannel create(int size) {
        MemorySeekableByteChannel c = new MemorySeekableByteChannel(size);
        assertThat(c.isOpen()).isTrue();
        try {
            assertThat(c.position()).isEqualTo(0);
            assertThat(c.size()).isEqualTo(size);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        return c;
    }

    // TODO : test close thread safety

    // operations to test
    //
    // create
    // x with valid parameters
    // x negative size -> exception
    //
    // open & close
    // x should be open by default
    // x should be closed once closed() have been called
    // x should throw exception when invoking any method once closed
    //
    // read
    // - read known data
    // - read while empty -> exception ?
    // - read at a given position
    // - reading should increase position
    //
    // write
    // - write known data, then read-it back
    // - write when position is at the end of buffer -> exception ?
    // - write should increase position
    //

    @Test
    public void setPosition() throws IOException {
        MemorySeekableByteChannel c = create(10);
        assertThat(c.position()).isEqualTo(0);
        assertThat(c.position(5)).isSameAs(c);
        assertThat(c.position()).isEqualTo(5);
        assertThat(c.position(0)).isSameAs(c);
        assertThat(c.position(9)).isSameAs(c);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void netagivePositionNotAllowed() throws IOException {
        MemorySeekableByteChannel c = create(0);
        c.position(-1);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void outOfBoundPositionNotAllowed() throws IOException {
        MemorySeekableByteChannel c = create(1);
        c.position(1);
    }

    // set & get position
    // - set position then get it
    // - set position <0 -> exception
    // - set position >=size -> exception
    //
    // get size
    // - identical as when created
    //
    // truncate
    // - effect on size ??
    // truncate to <0 -> exception
    // truncate to >=size -> exception

    @Test
    public void truncateNegative() {
        // TODO
    }

    @Test
    public void truncateOutOfBounds() {
        //TODO
    }

    @Test
    public void truncateToGivenSize() {
        // TODO
        // truncate to max size
        // truncate to non-zero value
        // truncate to 0
    }

    @Test
    public void truncateReadOnly() {
        // TODO
    }

    @Test
    public void writeReadOnly() {
        // TODO
    }

    // TODO : how to handle file open read/write modes
    // StandardOpenOption

    // Concurrency ??
    // -> FileChannel seems to partially deal with it
    @Test
    public void sandbox() {
        final int CAPACITY = 100;

        byte[] data = new byte[CAPACITY];
        ByteBuffer buffer = ByteBuffer.wrap(data);
        assertThat(buffer.position()).isEqualTo(0);
        assertThat(buffer.capacity()).isEqualTo(CAPACITY);
        assertThat(buffer.limit()).isEqualTo(CAPACITY);


        buffer.put((byte) 42);

        assertThat(buffer.limit()).isEqualTo(CAPACITY);
        assertThat(buffer.capacity()).isEqualTo(CAPACITY);
        assertThat(buffer.remaining()).isEqualTo(CAPACITY - 1);
        assertThat(buffer.position()).isEqualTo(1);
        assertThat(buffer.limit()).isEqualTo(CAPACITY);


        // TODO : difference between capacity and limit ?
        // capacity : initial capacity
        // limit : index du premier elément à ne pas lire ou écrire.
        // position : index du premier élément à lire ou écrire
        // -> position et limit définissent une fenêtre sur le buffer
        // -> mark : backup de la position.

        // mark() : saves current position
        // reset() : restove previously saved position
        // rewind() : set position to 0
        // clear() : rewind to 0 + removes mark + limit = capacity
        // remaining() : limit - position

        // TODO : generate random sequence of bytes for testing

    }

    // TODO
    //
    // open/close status, ensure that we can't use it while closed
    //
    // --> how to read and write data from a ByteBuffer ?
    // do we need to create a dedicated test class ?
    // -> there must exist a suitable implementation somewhere
    // Solution : use ByteBuffer.allocate() or ByteBuffer.wrap();
    // when reading form such buffer, we have to use a readonly to make sure input is not modified
    //
    // ByteBuffer methods & attributes
    // difference between position and mark
    // what does compact does ?
    //
    // --> sounds like ByteBuffer may be a good candidate class to hold our data blocks
    // wrapping it in a small linked-list structure may do the job
    // - switching between direct and non-direct allocation may allow to allocate outside heap
    // but in-depth testing is required to see if it is relevant
    //
    // read and write data, check that data is properly stored
    // -> ensure that data is properly stored
    //
    // read and write more than initial capacity (use blocks + chaining to minimize copy?)
    // -> ensure that all data is properly stored intact
    //
    // write few bytes, check expected size, then truncate, size must be zero
    // try to truncate with a value <0 or >size must throw an error
    // set position & read at this position

}
