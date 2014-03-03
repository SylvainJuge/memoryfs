package com.github.sylvainjuge.memoryfs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;

import static com.github.sylvainjuge.memoryfs.ParamAssert.checkNotNull;

public class MemoryByteChannel implements SeekableByteChannel {

    private boolean open;
    private long position;

    private final WritableByteChannel writeChannel;
    private final ReadableByteChannel readChannel;
    private final FileData data;

    private MemoryByteChannel(FileData data, boolean readOnly, boolean append) {
        this.data = checkNotNull(data, "file data");
        this.open = true;
        if (readOnly) {
            writeChannel = null;
            readChannel = Channels.newChannel(data.asInputStream());
        } else {
            readChannel = null;
            data.asOutputStream();
            if (append) {
                position = data.size();
            } else {
                data.truncate(0);
            }
            writeChannel = Channels.newChannel(data.asOutputStream());
        }
    }

    public static MemoryByteChannel newReadChannel(FileData data) {
        return new MemoryByteChannel(data, true, false);
    }

    public static MemoryByteChannel newWriteChannel(FileData data, boolean append) {
        return new MemoryByteChannel(data, false, append);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        checkOpen();
        checkCanRead();
        int read = readChannel.read(dst);
        position += read;
        return read;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        checkOpen();
        checkCanWrite();
        int written = writeChannel.write(src);
        position += written;
        return written;
    }

    private void checkCanRead() {
        if (null == readChannel) {
            throw new NonReadableChannelException();
        }
    }

    private void checkCanWrite() {
        if (null == writeChannel) {
            throw new NonWritableChannelException();
        }
    }

    @Override
    public long position() throws IOException {
        return position;
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        checkOpen();
        if (newPosition < 0 || data.size() <= newPosition) {
            throw new IllegalArgumentException("position out of bounds : " + newPosition);
        }
        this.position = newPosition;
        return this;
    }

    @Override
    public long size() throws IOException {
        return data.size();
    }

    @Override
    public SeekableByteChannel truncate(long newSize) throws IOException {
        checkOpen();
        checkCanWrite();
        if (newSize < 0) {
            throw new IllegalArgumentException("can't truncate to negative size");
        }
        data.truncate((int) newSize);
        if (data.size() < position) {
            position = data.size();
        }
        return this;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void close() throws IOException {
        checkOpen();
        this.open = false;
    }

    private void checkOpen() throws ClosedChannelException {
        if (!open) {
            throw new ClosedChannelException();
        }
    }
}
