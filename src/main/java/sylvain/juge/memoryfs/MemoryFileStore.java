package sylvain.juge.memoryfs;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

public class MemoryFileStore extends FileStore {

    private final String name;
    private final boolean readOnly;
    private long totalSpace = 0;
    private long usableSpace = 0;
    private long unallocatedSpace = 0;

    private MemoryFileStore(String name, boolean readOnly, long capacity) {
        this.name = name;
        this.readOnly = readOnly;
        this.totalSpace = capacity;
        this.usableSpace = capacity;
        this.unallocatedSpace = capacity;
    }

    static class Builder {
        private long capacity = 0;
        private boolean readOnly = false;
        private String name = "";

        MemoryFileStore build() {
            return new MemoryFileStore(name, readOnly, capacity);
        }

        public Builder readOnly(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        public Builder capacity(long capacity) {
            this.capacity = capacity;
            return this;
        }
    }

    static Builder builder() {
        return new Builder();
    }


    @Override
    public String name() {
        return name;
    }

    @Override
    public String type() {
        return "memory";
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public long getTotalSpace() throws IOException {
        return totalSpace;
    }

    @Override
    public long getUsableSpace() throws IOException {
        return usableSpace;
    }

    @Override
    public long getUnallocatedSpace() throws IOException {
        return unallocatedSpace;
    }

    @Override
    public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
        return false;
    }

    @Override
    public boolean supportsFileAttributeView(String name) {
        return false;
    }

    @Override
    public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
        return null;
    }

    @Override
    public Object getAttribute(String attribute) throws IOException {
        return null;
    }
}
