package sylvain.juge.memoryfs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;

public class MemorySeekableByteChannel implements SeekableByteChannel {

    private final long size;
    private boolean open;

    public MemorySeekableByteChannel(long size) {
        if( size <0){
            throw new IllegalArgumentException("size must be >= 0");
        }
        this.size = size;
        this.open = true;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return 0;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return 0;
    }

    @Override
    public long position() throws IOException {
        return 0;
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        return null;
    }

    @Override
    public long size() throws IOException {
        return size;
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        return null;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void close() throws IOException {
        if(!open){
            throw new ClosedChannelException();
        }
        this.open = false;
    }
}
