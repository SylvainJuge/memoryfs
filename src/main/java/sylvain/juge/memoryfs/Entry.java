package sylvain.juge.memoryfs;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class Entry implements BasicFileAttributes {

    private final boolean isDirectory;
    private final String name;
    private final Map<String, Entry> files;
    private final Entry parent;

    // TODO : keeping a reference on parent Map.Entry may allow to retrieve
    // both the file name and it's associated entry in a single structure
    // -> this avoids duplicating name in parent map and in entry itself
    // -> in this case we will have to rename this class for clarity.
    Entry(Entry parent, boolean isDirectory, String name) {
        this.parent = parent;
        this.isDirectory = isDirectory;
        Map<String,Entry> emptyMap = Collections.emptyMap();
        this.files = isDirectory ? new HashMap<String, Entry>() : emptyMap;
        this.name = name;
    }

    private Entry addChild(Entry child, String name){
        if( files.containsKey(name) ){
            throw new ConflictException(name + " already exists in folder");
        }
        this.files.put(name, child);
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

    Map<String,Entry> getFiles(){
        return files;
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
