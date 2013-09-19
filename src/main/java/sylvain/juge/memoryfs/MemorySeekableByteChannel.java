package sylvain.juge.memoryfs;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;

// TODO : rename to MemoryByteChannel ??
public class MemorySeekableByteChannel implements SeekableByteChannel {

    private long size;
    private boolean open;
    private long position;

    private final WritableByteChannel writeChannel;
    private final ReadableByteChannel readChannel;

    private final byte[] data;

    // truncate-existing : truncate if exists
    // append : append to end of file
    //
    // create : create file if it does not exists
    // create-new : create file if it does not exists, fails if it exists

    // ==> reading has only a single option, thus let's try with it first

    // TODO : define where is data stored
    // using a very large byte array can be a solution
    // -> appending to a file may be quite hard
    // -> a kind of linked list with blocks for storage can also be a good idea
    // - fs metadata have to be stored separately from files

    private MemorySeekableByteChannel(long size, boolean readOnly) {
        if( size <0){
            throw new IllegalArgumentException("size must be >= 0");
        }
        if( size > Integer.MAX_VALUE){
            throw new IllegalArgumentException("can't handle such large sizes yet !");
        }
        data = new byte[(int)size]; // TODO : find a way to allow more than 2^32 bytes of storage
        InputStream input = new ByteArrayInputStream(data);
        OutputStream output = new ByteArrayOutputStream(0);
        readChannel = readOnly ? Channels.newChannel(input) : null;
        writeChannel = readOnly ? null : Channels.newChannel(output);
        this.size = size;
        this.open = true;
    }

    public static MemorySeekableByteChannel newReadChannel(long size){
        return new MemorySeekableByteChannel(size, true);
    }

    // TODO : add parameters to allow more than one mode
    public static MemorySeekableByteChannel newWriteChannel(long size){
        return new MemorySeekableByteChannel(size, false);
    }

    // TODO : defining a buildder to handle all options may be a good idea after all
    // -> builder should ensure that we don't try to use an invalid combination of parameters

    @Override
    public int read(ByteBuffer dst) throws IOException {
        checkOpen();
        checkCanRead();
        int read =  readChannel.read(dst);
        position += read;
        return read;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        checkOpen();
        checkCanWrite();
        int written =  writeChannel.write(src);
        position += written;
        return written;
    }

    private void checkCanRead() {
        if (null == readChannel) {
            throw new NonReadableChannelException();
        }
    }

    private void checkCanWrite(){
        if(null == writeChannel){
            throw new NonWritableChannelException();
        }
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
        checkCanWrite();
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
