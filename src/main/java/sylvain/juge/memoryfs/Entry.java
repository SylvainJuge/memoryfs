package sylvain.juge.memoryfs;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;

class Entry implements BasicFileAttributes {

    private final boolean isDirectory;
    private final Map<String, Entry> files;

    Entry(boolean isDirectory) {
        this.isDirectory = isDirectory;
        this.files = isDirectory ? new HashMap<String, Entry>() : null;
    }

    static Entry newDirectory() {
        return new Entry(true);
    }

    static Entry newFile() {
        return new Entry(false);
    }

    Map<String,Entry> getFiles(){
        return files;
    }

    @Override
    public FileTime lastModifiedTime() {
        return null;
    }

    @Override
    public FileTime lastAccessTime() {
        return null;
    }

    @Override
    public FileTime creationTime() {
        return null;
    }

    @Override
    public boolean isRegularFile() {
        return !isDirectory;
    }

    @Override
    public boolean isDirectory() {
        return isDirectory;
    }

    @Override
    public boolean isSymbolicLink() {
        return false;
    }

    @Override
    public boolean isOther() {
        return false;
    }

    @Override
    public long size() {
        return 0;
    }

    @Override
    public Object fileKey() {
        return null;
    }


    // - where we store file bytes
    // - contains all its attributes (may extend BasicFileAttributes)
    // - subclass for files/folders (only if required, especially if we store both the same way in the actual FS metadata)

    // what does FS is to map a MemoryPath to an Entry
}
