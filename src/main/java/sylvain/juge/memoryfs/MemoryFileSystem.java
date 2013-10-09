package sylvain.juge.memoryfs;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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

    // URI design
    //
    // memory:/
    //
    // use cases :
    // - allow to have a dedicated FS instance for testing purpose
    // - code under test should access to this FS through
    //
    // - a path that is handled by memory fs (static)
    // --> how is FS instance created if it does not exists yet ?
    //
    // --> how to allow using isolated FS instances in the same provider ?
    // ---> maybe using specific paths
    // ---> use a random identifier

    //
    // - a provided instance of fileSystem (IoC)
    // --> providing a comprehensive API is useful here


    // TODO : allow for more than one filestore
    private MemoryFileSystem(MemoryFileSystemProvider provider, String id, long capacity) {
        this.provider = provider;
        this.id = id;
        this.isOpen = new AtomicBoolean(true);
        this.store = MemoryFileStore.builder().capacity(capacity).build();
        this.rootDirectories = new ArrayList<>();
        this.rootDirectories.add(MemoryPath.createRoot(this));
    }

    // TODO : should allow to build filestores with sane defaults
    // - should allow to create more than one fileStore (with a "mount" path)
    // - should allow to control available and usable space
    // - shoudl allow to control if FS is readonly or not
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

    private String idString(){
        return SCHEME + ":/" + id;
    }

    /**
     * @param path path
     * @return filesystem entry associated to this path, null if no such entry exists
     */
    Entry findEntry(Path path){
        MemoryPath p = asMemoryPath(path);
        if(p.isRoot()){
            return rootEntry;
        }

        Entry parentEntry = rootEntry;
        Entry childEntry = null;
        Iterator<String> it = p.partsIterator();
        while(it.hasNext()){
            String part = it.next();
            childEntry = parentEntry.getFiles().get(part);
            if(null == childEntry){
                return null;
            }
            parentEntry = childEntry;
        }
        return childEntry;
    }

    // things to test :
    // - parent exists but is not a directory
    // - multiple level of parents do not exists

    // - direct parent exists
    //   - fail if it is not a directory
    // - direct parent does not exists
    //  - we have to iterator over all ancestors to create folders when needed
    //  - while creating, if we encounter conflicts, we throw an error

    public Entry createEntry(Path path, boolean directory, boolean createParents) {
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

        if( null == parentEntry){
            throw new DoesNotExistsException(parent);
        }

        String name = asMemoryPath(path.getFileName()).getPath();
        return directory ?
                Entry.newDirectory(parentEntry, name) :
                Entry.newFile(parentEntry, name);

    }

    void deleteEntry(Path path){
        Entry entry = findEntry(path);
        if( rootEntry == entry){
            throw new IllegalArgumentException("deleting fs root is not allowed");
        }
        if( null != entry){
            entry.getParent().removeChild(entry);
        }
    }

    public DirectoryStream<Path> newDirectoryStream(final MemoryPath path) throws IOException {

        Entry entry = findEntry(path);
        if (null == entry || !entry.isDirectory()){
            throw new NotDirectoryException("not a valid directory : "+path);
        }

        final Iterator<String> contentIterator = path.partsIterator();
        return new DirectoryStream<Path>() {

            @Override
            public Iterator<Path> iterator() {
                return new Iterator<Path>() {
                    @Override
                    public boolean hasNext() {
                        return contentIterator.hasNext();
                    }

                    @Override
                    public Path next() {
                        return path.resolve(contentIterator.next());
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
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
        // TODO : implement this
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
}
