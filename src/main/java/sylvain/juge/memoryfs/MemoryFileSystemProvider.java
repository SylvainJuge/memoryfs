package sylvain.juge.memoryfs;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static sylvain.juge.memoryfs.MemoryFileSystem.SCHEME;
import static sylvain.juge.memoryfs.MemoryFileSystem.asMemoryFileSystem;
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

    public MemoryFileSystem registerFileSystem(MemoryFileSystem fs) {
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

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        return asMemoryFileSystem(path.getFileSystem()).newByteChannel(path, options);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        // TODO : path type is already enforced by code below, see how we can have a common exception for this
        // => see (outside of tests) where such exception is required in API spec
        if (!(dir instanceof MemoryPath)) {
            throw new IllegalArgumentException("unexpected path type");
        }
        return asMemoryFileSystem(dir.getFileSystem()).newDirectoryStream(dir);
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        asMemoryFileSystem(dir.getFileSystem()).createEntry(dir, true, true);
    }

    @Override
    public void delete(Path path) throws IOException {
        Entry entry = asMemoryFileSystem(path.getFileSystem()).findEntry(path);
        if (null == entry) {
            throw new NoSuchFileException(path.toString());
        }
        entry.delete();
    }

    // test cases
    // - target path exists ( use overwrite option to see what to do)
    // - target path exists and is not of the same type (folder|file)
    // - creates target parent folder if it does not exists


    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        asMemoryFileSystem(source.getFileSystem()).move(source, target, options);
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        asMemoryFileSystem(source.getFileSystem()).copy(source, target, options);
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
        findEntry(path);
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        return null;
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        if (!type.isAssignableFrom(Entry.class)) {
            throw new UnsupportedOperationException("unsupported attribute type : " + type);
        }
        return type.cast(findEntry(path));
    }

    private static Entry findEntry(Path path) throws NoSuchFileException {
        Entry entry = asMemoryPath(path).findEntry();
        if( null == entry){
            // required for Files.exists(Path) to work, since it assumes file existence when this method
            // does not throw exception, and does not care when it returns null
            throw new NoSuchFileException(path.toString());
        }
        return entry;
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
