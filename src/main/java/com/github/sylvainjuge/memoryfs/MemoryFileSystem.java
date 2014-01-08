package com.github.sylvainjuge.memoryfs;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.*;

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
        if (fs instanceof MemoryFileSystem || null == fs) {
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

    String getId() {
        return id;
    }

    FileStore getFileStore(Path path) throws IOException {
        Entry entry = findEntry(path);
        if (null == entry) {
            throw new DoesNotExistsException(path);
        }
        // yet we handle only 1 file store
        return store;
    }

    /**
     * @param path path
     * @return filesystem entry associated to this path, null if no such entry exists
     */
    Entry findEntry(Path path) {
        MemoryPath p = MemoryPath.asMemoryPath(path);
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

    Entry copy(Path source, Path target, CopyOption... options) throws IOException {
        // Copies a single file/folder withint the same fs instance,
        // and does not perform recursive copy for folders (see Files#copy(...) for details)

        MemoryFileSystem fs = asMemoryFileSystem(source.getFileSystem());

        Entry sourceEntry = fs.findEntry(source);
        if (null == sourceEntry) {
            throw new DoesNotExistsException(source);
        }

        Entry targetEntry = fs.findEntry(target);

        if (null == targetEntry) {

            Entry targetParent = fs.findEntry(target.getParent());
            if (null == targetParent) {
                targetParent = fs.createEntry(target.getParent(), true, true);
            }
            sourceEntry.copy(targetParent, target.getFileName().toString());

        } else {

            if (targetEntry.isDirectory() != sourceEntry.isDirectory()) {
                throw new ConflictException("copy target already exists and is not of same type");
            }
            if (!hasOption(REPLACE_EXISTING, options)) {
                throw new ConflictException("conflict");
            }

            if (!sourceEntry.isDirectory()) {
                throw new RuntimeException("wip : copy file");
            }

        }
        return targetEntry;
    }

    private static boolean hasOption(CopyOption option, CopyOption[] options) {
        for (CopyOption o : options) {
            if (option.equals(o)) {
                return true;
            }
        }
        return false;
    }

    void move(Path source, Path target, CopyOption... options) throws IOException {

        Entry sourceEntry = findEntry(source);
        if (null == sourceEntry) {
            throw new DoesNotExistsException(source);
        }
        Entry targetEntry = findEntry(target);

        boolean overwrite = hasOption(REPLACE_EXISTING, options);
        if (null != targetEntry) {
            if (!overwrite) {
                throw new ConflictException("");
            }
            Entry targetParentEntry = targetEntry.getParent();
            if (sourceEntry.isDirectory()) {
                Entry item = sourceEntry.getEntries();
                while (null != item) {
                    // TODO : what happens when there is a name conflict, do we overwrite too ?
                    item.move(targetEntry);
                }
                sourceEntry.delete();
            } else {
                targetEntry.delete();
                sourceEntry.move(targetParentEntry);
            }
        } else {
            Entry targetParentEntry = findEntry(target.getParent());
            if (null == targetParentEntry) {
                targetParentEntry = createEntry(target.getParent(), true, true);
            }
            String targetFileName = target.getFileName().toString();

            sourceEntry.rename(targetFileName);
            sourceEntry.move(targetParentEntry);
        }

    }

    public void createDirectory(Path path) throws IOException {
        createEntry(path, true, false);
    }

    private Entry createEntry(Path path, boolean directory, boolean createParents) throws IOException {
        Path absolutePath = path.toAbsolutePath();
        Path parent = absolutePath.getParent();
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
                    String name = MemoryPath.asMemoryPath(dir.getFileName()).getPath();
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

        String name = MemoryPath.asMemoryPath(absolutePath.getFileName()).getPath();
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
                return new DirectoryStreamPathIterator(MemoryFileSystem.this, startFolder);
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
        throw new UnsupportedOperationException("not supported");
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
        return SCHEME + ":/" + id;
    }

    public MemoryByteChannel newByteChannel(Path path, Set<? extends OpenOption> options) throws IOException {
        if (hasAnyOption(options, SPARSE, DELETE_ON_CLOSE, SYNC, DSYNC)) {
            throw new UnsupportedOperationException();
        }
        boolean isRead = hasAnyOption(options, READ);
        boolean isWrite = hasAnyOption(options, WRITE);
        if (!isRead && !isWrite) {
            isRead = true;
        } else if (isRead && isWrite) {
            throw new IllegalArgumentException("exactly one of read or write expected, mutualy exclusive");
        }

        boolean create = isWrite && hasAnyOption(options, CREATE, CREATE_NEW);
        boolean createNew = isWrite && hasAnyOption(options, CREATE_NEW);
        boolean truncate = create && hasAnyOption(options, TRUNCATE_EXISTING);

        Path absolutePath = path.toAbsolutePath();
        Entry entry = findEntry(absolutePath);

        if (isRead) {
            if (null == entry) throw new DoesNotExistsException(absolutePath);
            if (entry.isDirectory()) throw new InvalidRequestException("target path is a directory : " + absolutePath);
            return MemoryByteChannel.newReadChannel(entry.getData());
        } else {
            if (null == entry) {
                if (!create) throw new DoesNotExistsException(path);
                entry = createEntry(absolutePath, false, false);
            } else {
                if (createNew) throw new ConflictException("impossible to create new file, it already exists");
                if (truncate) entry.getData().truncate(0);
            }
            return MemoryByteChannel.newWriteChannel(entry.getData(), options.contains(APPEND));
        }
    }

    private static boolean hasAnyOption(Set<? extends OpenOption> set, OpenOption... option) {
        for (OpenOption o : option) {
            if (set.contains(o)) return true;
        }
        return false;
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
            if (null == current) {
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
