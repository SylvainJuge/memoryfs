package sylvain.juge.memoryfs;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.file.StandardOpenOption.*;
import static sylvain.juge.memoryfs.MemoryPath.asMemoryPath;

public class MemoryFileSystem extends FileSystem {

    static final String SEPARATOR = "/";
    static final String SCHEME = "memory";

    private final MemoryFileSystemProvider provider;
    private final String id;
    private final FileStore store;

    private final Entry rootEntry = Entry.newRoot();
    private final List<Path> rootDirectories;

    private AtomicBoolean isOpen;

    static MemoryFileSystem asMemoryFileSystem(FileSystem fs) {
        if (fs instanceof MemoryFileSystem) {
            return (MemoryFileSystem) fs;
        }
        throw new ProviderMismatchException();
    }

    private MemoryFileSystem(MemoryFileSystemProvider provider, String id, long capacity) {
        this.provider = provider;
        this.id = id;
        this.isOpen = new AtomicBoolean(true);
        this.store = MemoryFileStore.builder().capacity(capacity).build();
        this.rootDirectories = new ArrayList<>();
        this.rootDirectories.add(MemoryPath.createRoot(this));
    }

    static class Builder {
        private final MemoryFileSystemProvider provider;
        private long capacity = 0;
        private String id = "";

        private Builder(MemoryFileSystemProvider provider) {
            this.provider = provider;
        }

        public Builder id(String id) {
            if (null == id) {
                throw new IllegalArgumentException("id is required");
            }
            this.id = id;
            return this;
        }

        public Builder capacity(long capacity) {
            if (capacity < 0) {
                throw new IllegalArgumentException("capacity can't be negative");
            }
            this.capacity = capacity;
            return this;
        }

        public MemoryFileSystem build() {
            return provider.registerFileSystem(new MemoryFileSystem(provider, id, capacity));
        }
    }

    public static Builder builder(MemoryFileSystemProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider must be provided");
        }
        return new Builder(provider);
    }

    URI toUri(String path) {
        if (!path.startsWith(SEPARATOR)) {
            throw new InvalidPathException(path, "path must be absolute for URI conversion");
        }
        StringBuilder sb = new StringBuilder(SCHEME).append(":");
        if (!id.isEmpty()) {
            sb.append(SEPARATOR).append(id);
        }
        sb.append(path);
        return URI.create(sb.toString());
    }

    String getId() {
        return id;
    }

    private String idString() {
        return SCHEME + ":/" + id;
    }

    /**
     * @param path path
     * @return filesystem entry associated to this path, null if no such entry exists
     */
    Entry findEntry(Path path) {
        MemoryPath p = asMemoryPath(path);
        if (p.isRoot()) {
            return rootEntry;
        }

        Entry parentEntry = rootEntry;
        Entry childEntry = null;
        Iterator<String> it = p.partsIterator();
        while (it.hasNext()) {
            String part = it.next();
            childEntry = parentEntry.getChild(part);
            if (null == childEntry) {
                return null;
            }
            parentEntry = childEntry;
        }
        return childEntry;
    }

    Entry createEntry(Path path, boolean directory, boolean createParents) {
        Path parent = path.getParent();
        Entry parentEntry = findEntry(parent);

        if (null != parentEntry && !parentEntry.isDirectory()) {
            throw new ConflictException("parent folder is not a directory");
        }

        if (null == parentEntry && createParents) {
            // 1st entry is always a child of root
            parentEntry = rootEntry;

            for (Path dir : parent) {
                Entry dirEntry = findEntry(dir);
                if (null == dirEntry) {
                    String name = asMemoryPath(dir.getFileName()).getPath();
                    dirEntry = Entry.newDirectory(parentEntry, name);
                } else if (!dirEntry.isDirectory()) {
                    throw new ConflictException("conflict : path exists and is not a directory :" + dir);
                }
                parentEntry = dirEntry;
            }
        }

        if (null == parentEntry) {
            throw new DoesNotExistsException(parent);
        }

        String name = asMemoryPath(path.getFileName()).getPath();
        return directory ?
                Entry.newDirectory(parentEntry, name) :
                Entry.newFile(parentEntry, name);

    }

    DirectoryStream<Path> newDirectoryStream(Path path) throws IOException {

        final Entry startFolder = findEntry(path);
        if (null == startFolder) {
            throw new DoesNotExistsException(path);
        } else if (!startFolder.isDirectory()) {
            throw new NotDirectoryException("not a valid directory : " + path);
        }

        return new DirectoryStream<Path>() {

            @Override
            public Iterator<Path> iterator() {
                    return  new DirectoryStreamPathIterator(MemoryFileSystem.this, startFolder);
            }

            @Override
            public void close() throws IOException {
            }
        };
    }

    @Override
    public FileSystemProvider provider() {
        return provider;
    }

    @Override
    public void close() throws IOException {
        if (isOpen.getAndSet(false)) {
            provider.removeFileSystem(id);
        }
    }

    @Override
    public boolean isOpen() {
        return isOpen.get();
    }

    @Override
    public boolean isReadOnly() {
        // TODO : see usage at jdk level to see how we can use it
        return false;
    }

    @Override
    public String getSeparator() {
        return SEPARATOR;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return rootDirectories;
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return Collections.singletonList(store);
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return Collections.emptySet();
    }

    @Override
    public Path getPath(String first, String... more) {
        StringBuilder sb = new StringBuilder();
        sb.append(first);
        for (String s : more) {
            sb.append(SEPARATOR);
            sb.append(s);
        }
        return MemoryPath.create(this, sb.toString());
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        return null;
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public String toString() {
        return idString();
    }

    public MemoryByteChannel newByteChannel(Path path, Set<? extends OpenOption> options){
        // TODO : make this more readable
        if( options.contains(SPARSE)
                || options.contains(DELETE_ON_CLOSE)
                || options.contains(SYNC)
                || options.contains(DSYNC)
                ){
            throw new UnsupportedOperationException();

        }
        Path absolutePath = path.toAbsolutePath();

        Entry entry = findEntry(absolutePath);
        if (options.contains(READ)) {
            if (options.contains(WRITE)) {
                throw new IllegalArgumentException("read and write are mutualy exclusive options");
            }
            if( options.contains(CREATE) || options.contains(CREATE_NEW)){
                throw new IllegalArgumentException("create inconsistent options ");
            }
            if (null == entry) {
                throw new DoesNotExistsException(absolutePath);
            } else if (entry.isDirectory()) {
                throw new InvalidRequestException("target path is a directory : " + absolutePath);
            }
            return MemoryByteChannel.newReadChannel(entry.getData());
        } else if (options.contains(WRITE)) {
            if (null != entry && options.contains(CREATE_NEW)) {
                throw new ConflictException("impossible to create new file, it already exists");
            }
            if (null == entry) {
                if (!options.contains(CREATE) && !options.contains(CREATE_NEW)) {
                    throw new DoesNotExistsException(path);
                } else {
                    entry = createEntry(absolutePath, false, true);
                }
            } else if( options.contains(TRUNCATE_EXISTING)){
                entry.getData().truncate(0);
            }
            return MemoryByteChannel.newWriteChannel(entry.getData(), false);
        }
        throw new IllegalArgumentException("read or write option is required");
    }

    private static class DirectoryStreamPathIterator implements Iterator<Path> {

        private final MemoryFileSystem fs;
        private Entry current = null;

        DirectoryStreamPathIterator(MemoryFileSystem fs, Entry startFolder) {
            this.fs = fs;
            this.current = startFolder.getEntries();
        }

        @Override
        public boolean hasNext() {
            return null != current;
        }

        @Override
        public Path next() {
            if( null == current){
                throw new NoSuchElementException();
            }
            Path result = MemoryPath.create(fs, current.getPath());
            current = current.getNext();
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
