package sylvain.juge.memoryfs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MemoryPath implements Path {

    private static final String SEPARATOR = "/";
    private final MemoryFileSystem fs;
    private final List<String> parts;
    private final boolean absolute;

    // TODO : add another constructor to avoid re-parsing when creating form existing path
    MemoryPath(MemoryFileSystem fs, String path) {
        if (null == fs) {
            throw new IllegalArgumentException("filesytem required");
        }
        if (null == path || path.isEmpty()) {
            throw new IllegalArgumentException("path required not empty, got : " + path);
        }
        this.fs = fs;
        parts = new ArrayList<>();
        for (String s : path.split(SEPARATOR)) {
            if (!s.isEmpty()) {
                parts.add(s);
            }
        }
        absolute = path.startsWith(SEPARATOR);
    }

    private boolean isRoot(){
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
        if(isRoot()){
            return this;
        }
        return absolute ? new MemoryPath(fs, "/") : null;
    }

    @Override
    public Path getFileName() {
        return null;
    }

    @Override
    public Path getParent() {
        if(isRoot()){
            return null;
        }
        // note : must not resolve path . and .., probably ~ too.
        throw new RuntimeException("TODO implement getParent for non-root elements");
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
        StringBuilder sb = new StringBuilder();
        if (absolute) {
            sb.append(SEPARATOR);
        }
        for (int i = 0; i < parts.size() && i <= index; i++) {
            if (0 < i) {
                sb.append(SEPARATOR);
            }
            sb.append(parts.get(i));
        }
        return new MemoryPath(fs, sb.toString());
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        throw new RuntimeException("TODO : implement subpath");
    }

    @Override
    public boolean startsWith(Path other) {
        return false;
    }

    @Override
    public boolean startsWith(String other) {
        return false;
    }

    @Override
    public boolean endsWith(Path other) {
        return false;
    }

    @Override
    public boolean endsWith(String other) {
        return false;
    }

    @Override
    public Path normalize() {
        // if path does not have any item nor root, return empty path
        // otherwise, we need to strip uneccesary items like . and ...
        // a/b/../c -> a/c
        // a/.. -> ""
        // a/../b -> b
        // ../a -> ??? can't be normalized unless resolved as relative
        // ./a -> a
        // a/. -> a
        // a/./b -> a/b
        // a/ -> a ( we strip the last useless '/' )
        return null;
    }

    @Override
    public Path resolve(Path other) {
        return null;
    }

    @Override
    public Path resolve(String other) {
        return null;
    }

    @Override
    public Path resolveSibling(Path other) {
        return null;
    }

    @Override
    public Path resolveSibling(String other) {
        return null;
    }

    @Override
    public Path relativize(Path other) {
        return null;
    }

    @Override
    public URI toUri() {
        StringBuilder sb = new StringBuilder();
        sb.append(fs.getUri());
        if (!absolute) {
            throw new RuntimeException("how to guess relative path uri ? with current folder ?");
        }
        for (int i = 0; i < parts.size(); i++) {
            if (0 < i) {
                sb.append(SEPARATOR);
            }
            sb.append(parts.get(i));
        }
        return URI.create(sb.toString());
    }

    @Override
    public Path toAbsolutePath() {
        return null;
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
        return null;
    }

    @Override
    public int compareTo(Path other) {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MemoryPath other = (MemoryPath) o;

        if (absolute != other.absolute) return false;
        if(parts.size()!=other.parts.size()) return false;

        for(int i=0;i<parts.size();i++){
            if(!parts.get(i).equals(other.parts.get(i))) return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = 31;
        for (String s : parts) {
            result = 31 * result;
            result += s.hashCode();
        }
        return result;
    }
}
