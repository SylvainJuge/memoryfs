package sylvain.juge.memoryfs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;

public class MemorySeekableByteChannel implements SeekableByteChannel {

    private long size;
    private boolean open;
    private long position;


    public MemorySeekableByteChannel(long size) {
        if( size <0){
            throw new IllegalArgumentException("size must be >= 0");
        }
        this.size = size;
        this.open = true;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        checkOpen();
        return 0;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        checkOpen();
        return 0;
    }

    @Override
    public long position() throws IOException {
        return position;
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        if( newPosition < 0 || size <= newPosition){
            throw new IllegalArgumentException("position out of bounds : "+newPosition);
        }
        this.position = newPosition;
        return this;
    }

    @Override
    public long size() throws IOException {
        return size;
    }

    @Override
    public SeekableByteChannel truncate(long newSize) throws IOException {
        checkOpen();
        if (newSize < 0) {
            throw new IllegalArgumentException("can't truncate to negative size");
        }
        if (newSize < size) {
            size = newSize;
            if (size < position) {
                position = size;
            }
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
        if(!open){
            throw new ClosedChannelException();
        }
    }
}
