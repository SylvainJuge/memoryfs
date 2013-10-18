package sylvain.juge.memoryfs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.*;

public class MemoryFileChannel  extends FileChannel {

    private final MemoryByteChannel channel;

    MemoryFileChannel(MemoryByteChannel channel){
        this.channel = channel;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return channel.read(dst);
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        return 0;// TODO: implement this
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return channel.write(src);
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        return 0;// TODO: implement this
    }

    @Override
    public long position() throws IOException {
        return channel.position();
    }

    @Override
    public FileChannel position(long newPosition) throws IOException {
        return new MemoryFileChannel((MemoryByteChannel)channel.position(newPosition));
    }

    @Override
    public long size() throws IOException {
        return channel.size();
    }

    @Override
    public FileChannel truncate(long size) throws IOException {
        return new MemoryFileChannel((MemoryByteChannel)channel.truncate(size));
    }

    // --- start of file specific operations ---

    @Override
    public void force(boolean metaData) throws IOException {
        // not relevant, since all writes are in-memory so everything is lost in case of power failure
    }

    // --- methods for fast transfer between channels in the same fs ---

    @Override
    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int read(ByteBuffer dst, long position) throws IOException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
        // TODO : return either exception / or make it as it works
        return null;
    }

    @Override
    public FileLock lock(long position, long size, boolean shared) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void implCloseChannel() throws IOException {
        channel.close();
    }
}
