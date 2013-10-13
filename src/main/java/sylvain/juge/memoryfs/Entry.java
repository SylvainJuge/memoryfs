package sylvain.juge.memoryfs;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

class Entry implements BasicFileAttributes {

    private final boolean isDirectory;

    private Entry parent;
    private String name;
    private Entry entries;
    private Entry next;
    private Entry previous;

    private Entry(Entry parent, boolean isDirectory, String name) {
        this.parent = parent;
        this.isDirectory = isDirectory;
        this.name = name;
    }

    private Entry addEntry(Entry child) {
        Entry previous = null;
        Entry current = entries;
        while (current != null && !current.name.equals(child.name)) {
            previous = current;
            current = current.next;
        }
        if (current != null) {
            throw new ConflictException("name conflict : " + child.name);
        }
        child.next = null;
        if (entries == null) {
            entries = child;
            child.previous = null;
        } else {
            previous.next = child;
            child.previous = previous;
        }
        child.parent = this;
        return child;
    }

    static Entry newRoot() {
        return new Entry(null, true, null);
    }

    static Entry newDirectory(Entry parent, String name) {
        return parent.addEntry(new Entry(parent, true, name));
    }

    static Entry newFile(Entry parent, String name) {
        return parent.addEntry(new Entry(parent, false, name));
    }

    Entry getChild(String name) {
        Entry current = entries;
        while (current != null && !current.name.equals(name)) {
            current = current.next;
        }
        return current;
    }

    Entry getParent() {
        return parent;
    }

    Entry getNext() {
        return next;
    }

    Entry getEntries() {
        return entries;
    }

    public void rename(String newName){
        if( null == parent){
            throw new IllegalArgumentException("can't rename root");
        }
        if( null == newName) {
            throw new InvalidNameException(newName);
        }
        Entry existingEntry = parent.getChild(newName);
        if( null != existingEntry){
            throw new ConflictException("name conflict : " + newName);
        }
        this.name = newName;
    }

    public void move(Entry newParent){
        if (null == parent) {
            throw new IllegalArgumentException("can't move root");
        }
        if (null == newParent) {
            // TODO : use a classic NPE probably more appropriate
            throw new IllegalArgumentException("parent entry required");
        }
        if (!newParent.isDirectory) {
            throw new IllegalArgumentException("directory expected");
        }
        // ensure that we don't move within itself
        Entry e = newParent;
        while (e != null && e != this) {
            e = e.parent;
        }
        if (e == this) {
            throw new IllegalArgumentException("can't move within itself");
        }

        if( parent != newParent) {
            delete();
            newParent.addEntry(this);
        }
    }

    public void delete() {
        if (null == parent) {
            throw new IllegalArgumentException("deleting fs root is not allowed");
        }
        if (previous == null) {
            // remove 1st file in folder
            parent.entries = next;
            if (next != null) {
                next.previous = null;
            }
        } else {
            previous.next = next;
        }
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

    String getPath() {
        List<String> parts = new ArrayList<>();
        for (Entry e = this; e != null; e = e.parent) {
            if (null != e.name) {
                parts.add(e.name);
            }
        }
        if (parts.isEmpty()) {
            return MemoryFileSystem.SEPARATOR;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = parts.size() - 1; i >= 0; i--) {
            sb.append(MemoryFileSystem.SEPARATOR);
            sb.append(parts.get(i));
        }
        return sb.toString();
    }


    @Override
    public String toString() {
        return getPath();
    }

    // - where we store file bytes
    // - contains all its attributes (may extend BasicFileAttributes)
    // - subclass for files/folders (only if required, especially if we store both the same way in the actual FS metadata)

    // what does FS is to map a MemoryPath to an Entry
}
