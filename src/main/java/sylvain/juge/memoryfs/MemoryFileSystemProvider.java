package sylvain.juge.memoryfs;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MemoryFileSystemProvider extends FileSystemProvider {

    // TODO : thread safety ??
    private final Map<URI,FileSystem> fileSystems;

    public MemoryFileSystemProvider(){
        this.fileSystems = new HashMap<>();
    }

    @Override
    public String getScheme() {
        return "memory";
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        if( fileSystems.containsKey(uri)){
            throw new RuntimeException("uri already exists, can't reuse it : "+uri);
        }
        FileSystem fs = new MemoryFileSystem(this);
        fileSystems.put(uri, fs);
        return fs;
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        FileSystem fs = fileSystems.get(uri);
        if(null == fs){
            throw new RuntimeException("no filesystem exists with this uri : "+uri);
        }
        return fs;
    }

    @Override
    public Path getPath(URI uri) {
        throw new UnsupportedOperationException("TODO : implement this");
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException("TODO : implement this");
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        throw new UnsupportedOperationException("TODO : implement this");
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException("TODO : implement this");
    }

    @Override
    public void delete(Path path) throws IOException {
        throw new UnsupportedOperationException("TODO : implement this");
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        throw new UnsupportedOperationException("TODO : implement this");
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        throw new UnsupportedOperationException("TODO : implement this");
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        throw new UnsupportedOperationException("TODO : implement this");
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        throw new UnsupportedOperationException("TODO : implement this");
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        throw new UnsupportedOperationException("TODO : implement this");
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        throw new UnsupportedOperationException("TODO : implement this");
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        return null;
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
    }
}
