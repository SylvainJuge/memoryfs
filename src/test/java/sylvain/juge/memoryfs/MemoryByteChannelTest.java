package sylvain.juge.memoryfs;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.*;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.security.SecureRandom;

import static org.fest.assertions.api.Assertions.assertThat;
import static sylvain.juge.memoryfs.MemoryByteChannel.newReadChannel;
import static sylvain.juge.memoryfs.MemoryByteChannel.newWriteChannel;

public class MemoryByteChannelTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void buildNullFileData() {
        newReadChannel(null);
    }

    @Test
    public void openByDefault() {
        MemoryByteChannel c = newReadChannel(FileData.newEmpty());
        assertThat(c.isOpen()).isTrue();
    }

    @Test
    public void sizeAsExpected() throws IOException {
        MemoryByteChannel c = newReadChannel(zeroFileData(42));
        assertThat(c.size()).isEqualTo(42);
    }

    @Test
    public void openCloseChannel() throws IOException {
        MemoryByteChannel c = newReadChannel(FileData.newEmpty());
        assertThat(c.isOpen()).isTrue();
        c.close();
        assertThat(c.isOpen()).isFalse();
    }

    @Test(expectedExceptions = ClosedChannelException.class)
    public void closeTwice() throws IOException {
        MemoryByteChannel c = newReadChannel(FileData.newEmpty());
        c.close();
        c.close();
    }

    @Test(expectedExceptions = ClosedChannelException.class)
    public void readClosed() throws IOException {
        MemoryByteChannel c = newReadChannel(FileData.newEmpty());
        c.close();
        c.read(null);
    }

    @Test(expectedExceptions = ClosedChannelException.class)
    public void writeClosed() throws IOException {
        MemoryByteChannel c = newWriteChannel(FileData.newEmpty());
        c.close();
        c.write(null);
    }

    @Test(expectedExceptions = NonWritableChannelException.class)
    public void writeInReadChannel() throws IOException {
        newReadChannel(FileData.newEmpty()).write(null);
    }

    @Test(expectedExceptions = NonWritableChannelException.class)
    public void truncateReadChannel() throws IOException {
        newReadChannel(FileData.newEmpty()).truncate(0);
    }

    @Test(expectedExceptions = NonReadableChannelException.class)
    public void readInWriteChannel() throws IOException {
        newWriteChannel(FileData.newEmpty()).read(null);
    }

    @Test
    public void writeShouldAdvancePosition() throws IOException {
        MemoryByteChannel c = newWriteChannel(zeroFileData(10));
        ByteBuffer buffer = ByteBuffer.wrap(new byte[5]);
        assertThat(c.position()).isEqualTo(0);

        assertThat(c.write(buffer)).isEqualTo(5);

        assertThat(c.position()).isEqualTo(5);
    }

    @Test
    public void readShouldAdvancePosition() throws IOException {
        MemoryByteChannel c = newReadChannel(randomFileData(10));
        ByteBuffer buffer = ByteBuffer.wrap(new byte[10]);

        // remaining space limits how much data is read
        buffer.position(1).limit(9);
        assertThat(buffer.remaining()).isEqualTo(8);

        assertThat(c.read(buffer)).isEqualTo(8);

        assertThat(c.position()).isEqualTo(8);
    }

    @Test
    public void readTillEOL() throws IOException {
        MemoryByteChannel c = newReadChannel(randomFileData(10));
        ByteBuffer buffer = ByteBuffer.wrap(new byte[10]);
        buffer.mark();
        assertThat(c.read(buffer)).isEqualTo(10);
        buffer.reset();

        // there is space in read buffer, but channel should reach EOL
        assertThat(c.read(buffer)).isLessThan(0);
    }

    @Test(enabled =  false)
    public void readWrite() {
        // test from 1 to slighly less than 10Mb
        int limit = 1024 * 1024 * 10; // 10mb
        for (int size = 1; size <= limit; size *= 2) {
            testReadWriteData(size);
        }
    }

    public void testReadWriteData(int size) {
        byte[] bytes = randomBytes(size);
        FileData data = FileData.fromData(bytes);

        // TODO : check that read data is the same as expected


    }

    private static SecureRandom rand = new SecureRandom();
    private static byte[] randomBytes(int size){
        byte[] result = new byte[size];
        rand.nextBytes(result);
        return result;
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
        MemoryByteChannel c = newReadChannel(zeroFileData(10));
        assertThat(c.position()).isEqualTo(0);
        assertThat(c.position(5)).isSameAs(c);
        assertThat(c.position()).isEqualTo(5);
        assertThat(c.position(0)).isSameAs(c);
        assertThat(c.position(9)).isSameAs(c);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void netagivePositionNotAllowed() throws IOException {
        MemoryByteChannel c = newReadChannel(FileData.newEmpty());
        c.position(-1);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void outOfBoundPositionNotAllowed() throws IOException {
        MemoryByteChannel c = newReadChannel(randomFileData(1));
        c.position(1);
    }

    // TODO : set position on a closed channel : shoudl throw an exception

    // set & get position
    // x set position then get it
    // x set position <0 -> exception
    // x set position >=size -> exception
    //
    // get size
    // x identical as when created
    //
    // truncate
    // x effect on size ??
    // x truncate to <0 -> exception
    // x truncate to >=size -> do nothing
    // x impact on position w : depending on position
    //
    // thread safety
    // - open/close state
    // - multiple readers allowed
    // - single writer allowed
    // - or any other concurrency policy

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void truncateNegative() throws IOException {
        newWriteChannel(randomFileData(1)).truncate(-1);
    }

    @Test
    public void truncateOutOfBounds() throws IOException {
        int initialSize = 2;
        MemoryByteChannel c = newWriteChannel(zeroFileData(initialSize));
        c.position(1);
        c.truncate(c.size() + 1);
        // size & posiiton not altered, doe not allow to grow size
        assertThat(c.size()).isEqualTo(initialSize);
        assertThat(c.position()).isEqualTo(1);
    }

    @Test
    public void truncateToGivenSize() throws IOException {
        MemoryByteChannel c = newWriteChannel(zeroFileData(10));
        assertThat(c.size()).isEqualTo(10);
        assertThat(c.position(4).position()).isEqualTo(4);

        // truncate to same size does not alter size nor position
        assertThat(c.truncate(c.size())).isSameAs(c);
        assertThat(c.size()).isEqualTo(10);
        assertThat(c.position()).isEqualTo(4);

        // truncate after position : position not altered
        c.truncate(6);
        assertThat(c.size()).isEqualTo(6);
        assertThat(c.position()).isEqualTo(4);

        // truncate before position : position set to size
        c.truncate(3);
        assertThat(c.size()).isEqualTo(3);
        assertThat(c.position()).isEqualTo(3);

        // truncate to zero : position reset to 0
        assertThat(c.position(1).position()).isEqualTo(1);
        c.truncate(0);
        assertThat(c.size()).isEqualTo(0);
        assertThat(c.position()).isEqualTo(0);
    }

    @Test(expectedExceptions = ClosedChannelException.class)
    public void truncateClosed() throws IOException {
        MemoryByteChannel c = newWriteChannel(FileData.newEmpty());
        c.close();
        c.truncate(0);
    }

    @Test(expectedExceptions = NonWritableChannelException.class)
    public void truncateReadOnly() throws IOException {
        newReadChannel(zeroFileData(10)).truncate(4);
    }

    @Test(expectedExceptions = NonWritableChannelException.class)
    public void writeReadOnly() throws IOException {
        newReadChannel(zeroFileData(10)).write(null);
    }

    private FileData randomFileData(int size){
        return FileData.fromData(randomBytes(size));
    }

    private FileData zeroFileData(int size){
        return FileData.fromData(new byte[size]);
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
