package sylvain.juge.memoryfs;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class MemoryFileSystem extends FileSystem {

    static final String SEPARATOR = "/";
    static final String SCHEME = "memory";

    private final MemoryFileSystemProvider provider;
    private final String id;
    private final FileStore store;

    private AtomicBoolean isOpen;

    // TODO : allow for more than one filestore
    private MemoryFileSystem(MemoryFileSystemProvider provider, String id, long capacity) {
        this.provider = provider;
        this.id = id;
        this.isOpen = new AtomicBoolean(true);
        this.store = MemoryFileStore.builder().capacity(capacity).build();
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
            return new MemoryFileSystem(provider, id, capacity);
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

    // TODO : see how we can remove this package-private method
    // testing should focus on actual FS id, not really on it's URI representation
    // URI is more important at MemoryPath level
    URI getUri() {
        return URI.create(SCHEME + ":/" + id);
    }

    String getId() {
        return id;
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
        // TODO : implement this
        return null;
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
        // TODO : implement this
        return null;
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
}
