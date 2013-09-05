package sylvain.juge.memoryfs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import static sylvain.juge.memoryfs.MemoryFileSystem.SEPARATOR;

public class MemoryPath implements Path {

    private static final String TWO_DOTS = "..";
    private static final String ONE_DOT = ".";
    private final MemoryFileSystem fs;
    private final List<String> parts;
    private final boolean absolute;

    // cached values (safe since class is immutable)
    private URI uri = null;
    private String path = null;

    static MemoryPath create(MemoryFileSystem fs, String path) {
        if (null == path || path.isEmpty()) {
            throw new IllegalArgumentException("path required not empty, got : " + path);
        }
        boolean absolute = path.startsWith(SEPARATOR);
        List<String> parts = new ArrayList<>();
        for (String s : path.split(SEPARATOR)) {
            if (!s.isEmpty()) {
                if (absolute && parts.isEmpty() && TWO_DOTS.equals(s)) {
                    throw new IllegalArgumentException("invalid absolute path : can't go upper than root");
                }
                parts.add(s);
            }
        }
        return new MemoryPath(fs, parts, 0, parts.size(), absolute);
    }

    private MemoryPath(MemoryFileSystem fs, List<String> parts, int start, int end, boolean absolute) {
        if (null == fs) {
            throw new IllegalArgumentException("filesytem required");
        }
        if (start < 0 || parts.size() < end || end < start) {
            throw new IllegalArgumentException(String.format("invalid range [%d,%d[ in list %s", start, end, Arrays.toString(parts.toArray())));
        }
        this.fs = fs;
        this.parts = parts.subList(start, end);
        this.absolute = absolute;
    }

    private boolean isRoot() {
        return absolute && parts.isEmpty();
    }

    @Override
    public FileSystem getFileSystem() {
        return fs;
    }

    @Override
    public boolean isAbsolute() {
        return absolute;
    }

    @Override
    public Path getRoot() {
        if (isRoot()) {
            return this;
        }
        return absolute ? create(fs, SEPARATOR) : null;
    }

    @Override
    public Path getFileName() {
        return parts.isEmpty() ? null :
                new MemoryPath(fs, parts, parts.size() - 1, parts.size(), false);
    }

    @Override
    public Path getParent() {
        if (isRoot()) {
            return null;
        }
        if (parts.size() == 1) {
            return getRoot();
        }
        return getName(parts.size() - 2);
    }

    @Override
    public int getNameCount() {
        return parts.size();
    }

    @Override
    public Path getName(int index) {
        if (index < 0 || parts.size() <= index) {
            throw new IllegalArgumentException("invalid name index : " + index);
        }
        // last item is current path itself
        if (index == parts.size() - 1) {
            return this;
        }
        return new MemoryPath(fs, parts, 0, index + 1, absolute);
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        throw new RuntimeException("TODO : implement subpath");
    }

    private static MemoryPath toMemoryPath(Path path){
        if(!(path instanceof MemoryPath)){
            throw new ProviderMismatchException("only memory path supported");
        }
        return (MemoryPath)path;
    }

    @Override
    public boolean startsWith(Path other) {
        if(other == this){
            return true;
        }
        MemoryPath path = toMemoryPath(other);
        if(absolute != path.isAbsolute() || parts.size() < path.parts.size() ){
            return false;
        }
        for(int i=0;i<path.parts.size();i++){
            if(!parts.get(i).equals(path.parts.get(i))){
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean startsWith(String other) {
        return getPath().startsWith(other);
    }

    @Override
    public boolean endsWith(Path other) {
        if( other == this){
            return true;
        }
        toMemoryPath(other);
        return false;
    }

    @Override
    public boolean endsWith(String other) {
        return false;
    }

    @Override
    public Path normalize() {
        List<String> normalized = new ArrayList<>();
        for (String part : parts) {
            switch (part) {
                case ONE_DOT:
                    if (normalized.isEmpty()) {
                        normalized.add(part);
                    }
                    break;
                case TWO_DOTS:
                    int last = normalized.size() - 1;
                    if (normalized.isEmpty() || TWO_DOTS.equals(normalized.get(last))) {
                        normalized.add(part);
                    } else {
                        normalized.remove(last);
                    }
                    break;
                default:
                    if (normalized.size() == 1 && ONE_DOT.equals(normalized.get(0))) {
                        normalized.clear();
                    }
                    normalized.add(part);
            }
        }
        return new MemoryPath(fs, normalized, 0, normalized.size(), absolute);
    }

    @Override
    public Path resolve(Path other) {
        toMemoryPath(other);
        return null;
    }

    @Override
    public Path resolve(String other) {
        return null;
    }

    @Override
    public Path resolveSibling(Path other) {
        toMemoryPath(other);
        return null;
    }

    @Override
    public Path resolveSibling(String other) {
        return null;
    }

    @Override
    public Path relativize(Path other) {
        toMemoryPath(other);
        return null;
    }

    @Override
    public URI toUri() {
        if( null == uri){
            uri = fs.toUri((absolute ? "" : SEPARATOR )+getPath());
        }
        return uri;

        // note :
        // - when fs path is "memory:/"
        //    root path is the same as fs path
        // - when fs path is memory:/id
        //    root path is memory:/id/
        // - when file store is provided
        // - default id : / -> memory:/fs1/
        // - explicit id : / -> memory:/id/fs1/
        //
        // using an empty id is not suitable due to constraints in URI format
        // memory://<host> would match with an empty id

        // ===> do we really need file store here ?
        // -> path : represents any path
        // -> fs knows how to translate this path to file system and file store
        // --> actual path in file system & file store is a subpath of total path
        //
    }

    public String getPath(){
        if( null != path){
            return path;
        }
        StringBuilder sb = new StringBuilder();
        for (String part:parts) {
            if(0 < sb.length()){
                sb.append(SEPARATOR);
            }
            sb.append(part);
        }
        path = (absolute ? SEPARATOR : "") + sb.toString();
        return path;
    }

    @Override
    public Path toAbsolutePath() {
        if (absolute) {
            return this;
        }
        return new MemoryPath(fs, parts, 0, parts.size(), true);
    }

    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        return null;
    }

    @Override
    public File toFile() {
        // TODO : how to create file instance that is mapped to memory for legacy ?
        return null;
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        return null;
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
        return null;
    }

    @Override
    public Iterator<Path> iterator() {
        return new PathIterator(this);
    }

    @Override
    public int compareTo(Path other) {
        toMemoryPath(other);
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MemoryPath other = (MemoryPath) o;

        if (absolute != other.absolute) return false;
        if (parts.size() != other.parts.size()) return false;

        for (int i = 0; i < parts.size(); i++) {
            if (!parts.get(i).equals(other.parts.get(i))) return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = 19;
        result = 31 * result + (absolute ? 1 : 0);
        for (String s : parts) {
            result = 31 * result + s.hashCode();
        }
        return result;
    }

    @Override
    public String toString() {
        return toUri().toString();
    }

    private static class PathIterator implements Iterator<Path>{

        private int i;
        private Path path;
        private PathIterator(Path path){
            this.path = path;
            this.i = 0;
        }
        @Override
        public boolean hasNext() {
            return i < path.getNameCount();
        }

        @Override
        public Path next() {
            return path.getName(i++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("not supported");
        }
    }
}
