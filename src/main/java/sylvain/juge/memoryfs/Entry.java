package sylvain.juge.memoryfs;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class Entry implements BasicFileAttributes {

    private final boolean isDirectory;
    private final String name;
    private final Entry parent;

    private Entry files;
    private Entry next;
    private Entry previous;

    // TODO : keeping a reference on parent Map.Entry may allow to retrieve
    // both the file name and it's associated entry in a single structure
    // -> this avoids duplicating name in parent map and in entry itself
    // -> in this case we will have to rename this class for clarity.
    Entry(Entry parent, boolean isDirectory, String name) {
        this.parent = parent;
        this.isDirectory = isDirectory;
        this.name = name;
    }

    private Entry addChild(Entry child, String name){
        Entry previous = null;
        Entry current = files;
        while (current != null && !current.name.equals(name)) {
            previous = current;
            current = current.next;
        }
        if( current != null){
            throw new ConflictException("name conflict : "+name);
        }
        if (files == null) {
            files = child;
        } else {
            previous.next = child;
            child.previous = previous;
        }
        return child;
    }

    static Entry newRoot(){
        return new Entry(null, true, null);
    }

    static Entry newDirectory(Entry parent, String name) {
        return parent.addChild(new Entry(parent, true, name),name);
    }

    static Entry newFile(Entry parent, String name) {
        return parent.addChild(new Entry(parent, false, name),name);
    }

    void removeChild(Entry child){
        if(!isDirectory){
            throw new IllegalStateException("can't remove child on a file");
        }
        Entry current = files;
        while( current != null && current != child){
            current = current.next;
        }
        if( current == null){
            throw new IllegalArgumentException("entry not found");
        }
        if( current.previous == null ){
            // remove 1st file in folder
            current.parent.files = current.next;
            if( current.next != null ){
                current.next.previous = null;
            }
        } else {
            current.previous.next = current.next;
        }
    }

    Entry getChild(String name){
        Entry current = files;
        while( current != null && !current.name.equals(name)){
            current = current.next;
        }
        return current;
    }

    Entry getParent() {
        return parent;
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


    @Override
    public String toString() {
        // note : not really efficient for very deep paths, but we can probably get with
        if( null == parent){
            return "/";
        }
        if ( null == parent.parent ){
            return "/"+name;
        }
        else return parent.toString() + "/" + name;
    }

    // - where we store file bytes
    // - contains all its attributes (may extend BasicFileAttributes)
    // - subclass for files/folders (only if required, especially if we store both the same way in the actual FS metadata)

    // what does FS is to map a MemoryPath to an Entry
}
