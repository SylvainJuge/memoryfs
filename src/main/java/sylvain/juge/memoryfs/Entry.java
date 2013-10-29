package sylvain.juge.memoryfs;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

class Entry implements BasicFileAttributes {

    private final boolean isDirectory;
    private final FileData data; // null for folders

    private Entry parent; // null for root
    private String name;
    private Entry entries; // null for files
    private Entry next;
    private Entry previous;

    // As long as this constructor remains private, we can "trust" calling code to provide consistent set of parameters
    // thus, we don't check them (directory has null data, file has non-null data, root has null name)
    private Entry(Entry parent, boolean isDirectory, String name, FileData data) {
        if (null != name) {
            checkName(name);
        }
        this.parent = parent;
        this.isDirectory = isDirectory;
        this.name = name;
        this.data = data;
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
        return new Entry(null, true, null, null);
    }

    static Entry newDirectory(Entry parent, String name) {
        return parent.addEntry(new Entry(parent, true, name, null));
    }

    static Entry newFile(Entry parent, String name) {
        return parent.addEntry(new Entry(parent, false, name, FileData.newEmpty()));
    }

    Entry getChild(String name) {
        Entry current = entries;
        while (current != null && !current.name.equals(name)) {
            current = current.next;
        }
        return current;
    }

    FileData getData(){
        return data;
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
            // simply use Objects.requireNonNull(newParent);
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

    public Entry copy(Entry targetParent, String targetName) {
        FileData dataCopy = isDirectory ? null : FileData.copy(data);
        Entry entry = new Entry(parent, isDirectory, targetName, dataCopy);
        targetParent.addEntry(entry);
        return entry;
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
        return isDirectory ? 0 : data.size();
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

    private static void checkName(String name){
        if (name.isEmpty() || name.contains("/") || name.contains("*") || ".".equals(name) || "..".equals(name)) {
            throw new InvalidNameException(name);
        }
    }


}
