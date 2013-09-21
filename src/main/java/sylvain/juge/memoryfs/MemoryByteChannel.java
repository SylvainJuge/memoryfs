package sylvain.juge.memoryfs;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class MemoryByteChannel implements SeekableByteChannel {

    private boolean open;
    private long position;

    private final WritableByteChannel writeChannel;
    private final ReadableByteChannel readChannel;
    private final FileData data;

    // channel open modes :

    // truncate-existing : truncate if exists
    // append : append to end of file
    //
    // create : create file if it does not exists
    // create-new : create file if it does not exists, fails if it exists

    // FS Storage :
    //
    // - folders structure
    // - file content storage
    // - file and folder attributes
    //
    // ==> here, we only deal with file content

    // input : Path + file open mode (read/write + options)
    // output : channel on this file

    // draft implementation :
    // -> use a byte array for each file, and copy to a larger one when we need to.
    // -> channel must use a shared data structure with fs to hold data
    //
    // final implementation :
    // -> use a structure that allows to append efficiently, like a linked-list of "blocks"


    // TODO : define where is data stored
    // using a very large byte array can be a solution
    // -> appending to a file may be quite hard
    // -> a kind of linked list with blocks for storage can also be a good idea
    // - fs metadata have to be stored separately from files

    private MemoryByteChannel(FileData data, boolean readOnly, boolean append){
        if( null == data){
            throw new IllegalArgumentException("file data storage is required");
        }
        this.data = data;
        this.open = true;
        if( readOnly ){
            writeChannel = null;
            readChannel = Channels.newChannel(data.asInputStream());
        } else {
            readChannel = null;
            data.asOutputStream();
            if( append ){
                position = data.size();
            }
            writeChannel = Channels.newChannel(data.asOutputStream());
        }
    }

    public static MemoryByteChannel newReadChannel(FileData data){
        return new MemoryByteChannel(data, true, false);
    }

    public static MemoryByteChannel newWriteChannel(FileData data, boolean append){
        return new MemoryByteChannel(data, false, append);
    }

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
        checkOpen();
        if( newPosition < 0 || data.size() <= newPosition){
            throw new IllegalArgumentException("position out of bounds : "+newPosition);
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
        data.truncate((int)newSize);
        if( data.size() < position ){
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
        if(!open){
            throw new ClosedChannelException();
        }
    }
}
