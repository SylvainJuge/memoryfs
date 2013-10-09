package sylvain.juge.memoryfs;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;

import static sylvain.juge.memoryfs.MemoryFileSystem.SCHEME;
import static sylvain.juge.memoryfs.MemoryPath.asMemoryPath;

public class MemoryFileSystemProvider extends FileSystemProvider {


    // thread safety : synchronized on instance for r/w
    private final Map<String, MemoryFileSystem> fileSystems;

    public MemoryFileSystemProvider() {
        this.fileSystems = new HashMap<>();
    }

    @Override
    public String getScheme() {
        return SCHEME;
    }

    private static void checkMemoryScheme(URI uri) {
        if (!SCHEME.equals(uri.getScheme())) {
            throw new IllegalArgumentException("invalid scheme : " + uri);
        }
    }

    private static String checkAndGetFileSystemId(URI uri) {
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("invalid URI : must be absolute : " + uri);
        }
        String path = uri.getPath();
        if (null == path || path.length() < 1 || null != uri.getHost()) {
            throw new IllegalArgumentException("invalid URI, fs root path must be in the form : 'memory:/[ID]' where [ID] is the filesystem ID");
        }
        for (String part : path.split("/")) {
            if (!part.isEmpty()) {
                return part;
            }
        }
        // default ID is an empty string
        return "";
    }

    // TODO : retrieve fs store structure & capacity from options or through uri parameters
    // TODO : allow to create FS with a random ID (distinct from other instances handled by this provider)

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        checkMemoryScheme(uri);
        String id = checkAndGetFileSystemId(uri);
        return MemoryFileSystem.builder(this).id(id).capacity(0).build();
    }

    public MemoryFileSystem registerFileSystem(MemoryFileSystem fs){
        String id = fs.getId();
        synchronized (fileSystems) {
            if (fileSystems.containsKey(id)) {
                throw new FileSystemAlreadyExistsException("file system already exists : " + id);
            }
            fileSystems.put(id, fs);
        }
        return fs;
    }

    private static Long getCapacity(Map<String, ?> env) {
        Object o = env.get("capacity");
        if (o == null) {
            return null;
        } else if (!(o instanceof Long)) {
            throw new IllegalStateException("capacity parameter must be of type long");
        }
        return (Long) o;
    }

    @Override
    public FileSystem newFileSystem(Path path, Map<String, ?> env) throws IOException {
        URI uri = URI.create(String.format("%s://%s", SCHEME, path));
        return newFileSystem(uri, env);
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        checkMemoryScheme(uri);
        String id = checkAndGetFileSystemId(uri);
        synchronized (fileSystems) {
            FileSystem fs = fileSystems.get(id);
            if (null == fs) {
                throw new FileSystemNotFoundException("no filesystem exists with this ID : " + id);
            }
            return fs;
        }
    }

    Map<String, ? extends FileSystem> registeredFileSystems() {
        synchronized (fileSystems) {
            return Collections.unmodifiableMap(fileSystems);
        }
    }

    void removeFileSystem(String id) {
        synchronized (fileSystems) {
            if (!fileSystems.containsKey(id)) {
                throw new IllegalStateException("file system does not exist in provider : " + id);
            }
            fileSystems.remove(id);
        }
    }

    @Override
    public Path getPath(URI uri) {
        checkMemoryScheme(uri);
        String id = checkAndGetFileSystemId(uri);
        synchronized (fileSystems) {
            MemoryFileSystem fs = fileSystems.get(id);
            if (null == fs) {
                throw new IllegalArgumentException("non existing filesystem : '" + id + "'");
            }
            return MemoryPath.create(fs, uri.getPath());
        }
    }

    // gives access to file bytes storage
    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {

        // SeekableByteChannel
        // -> see which byte buffer we can use to store file
        // -> something that is not fixed-size and is automatically increased/decreased when needed

        // ByteBuffer allows to map external memory (out of heap)
        // HeapByteBuffer (private in java.nio) seems to do this with storage on the heap
        // -> extending ByteBuffer seems possible by duplicating some of HeapByteBuffer
        // -> capacity seems to be fixed, I haven't seen any automatic rezise operation

        throw new UnsupportedOperationException("TODO : implement this");
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        if(!(dir instanceof MemoryPath)){
            throw new IllegalArgumentException("unexpected path type");
        }

        MemoryFileSystem fs = (MemoryFileSystem)dir.getFileSystem();
        return fs.newDirectoryStream((MemoryPath)dir);
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException("TODO : implement this");
        // zip fs : delegate to path implementation
    }

    @Override
    public void delete(Path path) throws IOException {
        throw new UnsupportedOperationException("TODO : implement this");
        // zip fs : delegate to path implementation
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        throw new UnsupportedOperationException("TODO : implement this");
        // zip fs : delegate to path implementation
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        throw new UnsupportedOperationException("TODO : implement this");
        // zip fs : delegate to path implementation
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
        if(!type.isAssignableFrom(Entry.class)){
            throw new UnsupportedOperationException("unsupported attribute type : "+ type);
        }
        Entry entry = asMemoryPath(path).findEntry();
        return type.cast(entry);
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        asMemoryPath(path);
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
    }
}
