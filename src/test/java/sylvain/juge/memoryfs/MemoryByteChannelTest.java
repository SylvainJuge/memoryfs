package sylvain.juge.memoryfs;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.security.SecureRandom;

import static org.fest.assertions.api.Assertions.assertThat;
import static sylvain.juge.memoryfs.MemoryByteChannel.newReadChannel;
import static sylvain.juge.memoryfs.MemoryByteChannel.newWriteChannel;

public class MemoryByteChannelTest {

    private static final SecureRandom rand = new SecureRandom();

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
        MemoryByteChannel c = newWriteChannel(FileData.newEmpty(), false);
        c.close();
        assertThat(c.isOpen()).isFalse();
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
        newWriteChannel(FileData.newEmpty(), false).read(null);
    }

    @Test
    public void writeShouldAdvancePosition() throws IOException {
        MemoryByteChannel c = newWriteChannel(zeroFileData(10), false);
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

    @Test
    public void writeAppendInitialPositionAtEnd() throws IOException {
        MemoryByteChannel c = newWriteChannel(zeroFileData(2), true);
        assertThat(c.position()).isEqualTo(2);
    }

    @Test
    public void writingWithoutAppendTruncates() throws IOException {
        MemoryByteChannel c = newWriteChannel(zeroFileData(5), false);
        assertThat(c.size()).isEqualTo(0);
        assertThat(c.position()).isEqualTo(0);
    }

    @Test(enabled = false)
    public void writeInExistingData() throws IOException {
        FileData data = zeroFileData(3);
        MemoryByteChannel write = newWriteChannel(data, true);
        assertThat(write.position()).isEqualTo(3);
        assertThat(write.size()).isEqualTo(3);

        // rewrite the middle byte
        write.position(1);
        write.write(ByteBuffer.wrap(new byte[]{1}));
        assertThat(write.position()).isEqualTo(2);

        assertThat(write.size()).isEqualTo(3);
        MemoryByteChannel read = newReadChannel(data);
        readsExpected(read, new byte[]{0, 1, 0});

        // writing more data is allowed and grows entity
        write.position(2);
        write.write(ByteBuffer.wrap(new byte[]{2, 3}));

        read.position(0);
        readsExpected(read, new byte[]{0, 1, 2, 3});

    }

    @Test
    public void readWrite() throws IOException {
        testReadWrite(1024); // 1kb
        testReadWrite(1024 * 1024); // 1Mb
    }

    @Test
    public void writeAppend() throws IOException {
        byte[] beforeAppend = randomBytes(5);
        byte[] toAppend = randomBytes(5);

        FileData data = FileData.fromData(beforeAppend);

        MemoryByteChannel channel = newWriteChannel(data, true);
        assertThat(channel.write(ByteBuffer.wrap(toAppend))).isEqualTo(toAppend.length);

        byte[] expected = append(beforeAppend, toAppend);
        readsExpected(newReadChannel(data), expected);
    }

    private byte[] append(byte[] first, byte[] second) {
        byte[] result = new byte[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, second.length, second.length);
        return result;
    }

    public void testReadWrite(int size) throws IOException {
        byte[] expected = randomBytes(size);
        FileData data = FileData.fromData(expected);

        // reading existing data
        MemoryByteChannel read = newReadChannel(data);
        readsExpected(read, expected);
        read.close();

        byte[] expectedWrite = randomBytes(size);
        MemoryByteChannel write = newWriteChannel(data, false);
        ByteBuffer toWrite = ByteBuffer.wrap(expectedWrite);
        assertThat(write.write(toWrite)).isEqualTo(size);
        write.close();

        read = newReadChannel(data);
        readsExpected(read, expectedWrite);
        read.close();

    }

    private static void readsExpected(MemoryByteChannel channel, byte[] expected) throws IOException {
        byte[] readBytes = new byte[expected.length];
        ByteBuffer readData = ByteBuffer.wrap(readBytes);
        assertThat(channel.read(readData)).isEqualTo(expected.length);
        assertThat(readBytes).isEqualTo(expected);
    }

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

    @Test(expectedExceptions = ClosedChannelException.class)
    public void setPositionOnClosedChannel() throws IOException {
        MemoryByteChannel c = newReadChannel(zeroFileData(1));
        c.close();
        assertThat(c.isOpen()).isFalse();
        c.position(0);
    }

    // TODO : things to test
    // thread safety
    // - open/close state
    // - multiple readers allowed
    // - single writer allowed
    // - or any other concurrency policy

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void truncateNegative() throws IOException {
        newWriteChannel(randomFileData(1), false).truncate(-1);
    }

    @Test
    public void truncateOutOfBounds() throws IOException {
        int initialSize = 2;
        MemoryByteChannel c = newWriteChannel(zeroFileData(initialSize), true);
        c.position(1);
        c.truncate(c.size() + 1);
        // size & posiiton not altered, does not allow to grow size
        assertThat(c.size()).isEqualTo(initialSize);
        assertThat(c.position()).isEqualTo(1);
    }

    @Test
    public void truncateToGivenSize() throws IOException {
        MemoryByteChannel c = newWriteChannel(zeroFileData(10), true);
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
        MemoryByteChannel c = newWriteChannel(FileData.newEmpty(), false);
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

    private FileData randomFileData(int size) {
        return FileData.fromData(randomBytes(size));
    }

    private FileData zeroFileData(int size) {
        return FileData.fromData(new byte[size]);
    }

    private static byte[] randomBytes(int size) {
        byte[] result = new byte[size];
        rand.nextBytes(result);
        return result;
    }
}
