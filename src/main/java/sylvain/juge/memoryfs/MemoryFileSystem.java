package sylvain.juge.memoryfs;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class MemoryFileSystem extends FileSystem {

    public static final String SEPARATOR = "/";
    private final MemoryFileSystemProvider provider;
    private final String id;

    private AtomicBoolean isOpen;

    MemoryFileSystem(MemoryFileSystemProvider provider, String id) {
        this.provider = provider;
        this.id = id;
        this.isOpen = new AtomicBoolean(true);
    }

    @Override
    public FileSystemProvider provider() {
        return provider;
    }

    @Override
    public void close() throws IOException {
        if(isOpen.getAndSet(false)){
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
        return Collections.emptyList();
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
