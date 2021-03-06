package com.github.sylvainjuge.memoryfs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static com.github.sylvainjuge.memoryfs.MemoryFileSystem.SCHEME;
import static com.github.sylvainjuge.memoryfs.MemoryFileSystem.SEPARATOR;

public class MemoryPath implements Path {

    private static final String TWO_DOTS = "..";
    private static final String ONE_DOT = ".";

    private final MemoryFileSystem fs;
    private final List<String> parts;
    private final boolean absolute;

    // cached values (safe since class is immutable)
    private URI uri = null;
    private String path = null;

    static MemoryPath asMemoryPath(Path path) {
        if (path instanceof MemoryPath || null == path) {
            return (MemoryPath) path;
        }
        throw new ProviderMismatchException();
    }

    private static void checkPath(String path) {
        if (null == path || path.isEmpty() || path.contains("*") || path.contains("?")) {
            throw new InvalidPathException(path, "path required not empty and without illegal characters");
        }
    }

    static MemoryPath create(MemoryFileSystem fs, String path) {
        checkPath(path);

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

    static MemoryPath createRoot(MemoryFileSystem fs) {
        return create(fs, "/");
    }

    private MemoryPath(MemoryFileSystem fs, List<String> parts, int start, int end, boolean absolute) {
        if (null == fs) {
            throw new IllegalArgumentException("filesytem required");
        }
        if (start < 0 || parts.size() < end || end < start) {
            throw new IllegalArgumentException(String.format("invalid range [%d,%d[ in interval [0,%d[", start, end, parts.size()));
        }
        this.fs = fs;
        this.parts = new ArrayList<>(parts.subList(start, end));
        this.absolute = absolute;
    }

    boolean isRoot() {
        return absolute && parts.isEmpty();
    }

    /**
     * @return entry for this path in filesystem, null if no such entry exists
     */
    Entry findEntry() {
        return fs.findEntry(this);
    }

    /**
     * @return an iterator over path parts
     */
    public Iterator<String> partsIterator() {
        return parts.iterator();
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
        return new MemoryPath(fs, parts, 0, parts.size() - 1, absolute);
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
        return new MemoryPath(fs, parts, index, index + 1, false);
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        return new MemoryPath(fs, parts, beginIndex, endIndex, false);
    }

    private static MemoryPath toMemoryPath(Path path) {
        if (!(path instanceof MemoryPath)) {
            throw new ProviderMismatchException("only memory path supported");
        }
        return (MemoryPath) path;
    }

    @Override
    public boolean startsWith(Path other) {
        if (other == this) {
            return true;
        }
        MemoryPath path = toMemoryPath(other);
        if (absolute != path.isAbsolute() || parts.size() < path.parts.size()) {
            return false;
        }
        for (int i = 0; i < path.parts.size(); i++) {
            if (!parts.get(i).equals(path.parts.get(i))) {
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
        if (other == this) {
            return true;
        }
        MemoryPath path = toMemoryPath(other);
        if (parts.size() < path.parts.size()) {
            return false;
        }
        int i = path.parts.size() - 1;
        int offset = parts.size() - path.parts.size();
        while (0 <= i) {
            if (!parts.get(i + offset).equals(path.parts.get(i))) {
                return false;
            }
            i--;
        }
        if (0 < offset) {
            // there is an offset, path must be non-absolute to match
            return !path.absolute;
        } else {
            // there is not offset, if this is absolute, then
            return absolute || !path.absolute;
        }
    }

    @Override
    public boolean endsWith(String other) {
        return getPath().endsWith(other);
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
        MemoryPath path = toMemoryPath(other);
        if (other.isAbsolute()) {
            return path;
        }
        return toSibling(parts.size(), path);
    }

    @Override
    public Path resolve(String other) {
        return other.isEmpty() ? this : resolve(create(fs, other));
    }

    @Override
    public Path resolveSibling(Path other) {
        MemoryPath path = toMemoryPath(other);
        if (path.isAbsolute() || parts.size() < 2) {
            return path;
        }
        return toSibling(parts.size() - 1, path);
    }

    private Path toSibling(int end, MemoryPath sibling) {
        List<String> newParts = new ArrayList<>(parts.subList(0, end));
        for (String s : sibling.parts) {
            newParts.add(s);
        }
        return new MemoryPath(fs, newParts, 0, newParts.size(), absolute);
    }

    @Override
    public Path resolveSibling(String other) {
        return resolveSibling(create(fs, other));
    }

    @Override
    public Path relativize(Path other) {
        if (equals(other)) {
            return create(fs, ONE_DOT);
        }
        MemoryPath path = toMemoryPath(other);
        if (absolute != path.absolute) {
            return path;
        }
        // a/b a/b/c/d -> test if first path is prefix of other, return suffix
        int i = 0;
        while (i < parts.size() && i < path.parts.size() && parts.get(i).equals(path.parts.get(i))) {
            i++;
        }

        // with absolute paths, we can always compute a relative path since they share the root
        boolean relativePathBetweenAbsolutes = (i == 0 && absolute);

        // no prefix in common
        if (i <= 0 && !relativePathBetweenAbsolutes) {
            return other;
        }
        // we have some prefix in common
        if (0 < i && i <= parts.size()) {
            // other path is longer
            // this path is a prefix, we return remaining part of other path
            return path.subpath(i, path.getNameCount());
        }
        // this path is longer or same length -> we have to add .. to remove all trailing levels
        int trailingCount = relativePathBetweenAbsolutes ? parts.size() : parts.size() - path.parts.size();
        List<String> relativePath = new ArrayList<>();
        for (int j = 0; j < trailingCount; j++) {
            relativePath.add(TWO_DOTS);
        }
        relativePath.addAll(path.parts.subList(i, path.parts.size()));
        return new MemoryPath(fs, relativePath, 0, relativePath.size(), false);
    }

    @Override
    public URI toUri() {
        if (null == uri) {
            StringBuilder sb = new StringBuilder(SCHEME).append(":");
            if (!absolute) {
                sb.append(SEPARATOR);
            }
            String fsId = fs.getId();
            if (!fsId.isEmpty()) {
                sb.append(SEPARATOR).append(fsId);
            }
            sb.append(getPath());
            uri = URI.create(sb.toString());
        }
        return uri;
    }

    public String getPath() {
        if (null != path) {
            return path;
        }
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (0 < sb.length()) {
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
        return toAbsolutePath().normalize();
    }

    @Override
    public File toFile() {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        throw new UnsupportedOperationException("not supported yet");
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
        throw new UnsupportedOperationException("not supported yet");
    }

    @Override
    public Iterator<Path> iterator() {
        return new PathIterator(this);
    }

    @Override
    public int compareTo(Path other) {
        MemoryPath path = (MemoryPath) other;
        if (absolute && !path.absolute) {
            return -1;
        } else if (!absolute && path.absolute) {
            return 1;
        }
        int max = Math.min(path.parts.size(), parts.size());
        for (int i = 0; i < max; i++) {
            int itemCompare = parts.get(i).compareTo(path.parts.get(i));
            if (itemCompare != 0) {
                return itemCompare;
            }
        }
        // shortest first
        return parts.size() - path.parts.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MemoryPath other = (MemoryPath) o;
        if (fs != other.fs) return false;
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
        // note : we rely on default hashcode implementation, which should return
        // a different value for each fs instance, and it is consistent with implementation
        // of equals method.
        result = 32 * result + fs.hashCode();
        result = 31 * result + (absolute ? 1 : 0);
        for (String s : parts) {
            result = 31 * result + s.hashCode();
        }
        return result;
    }

    @Override
    public String toString() {
        return getPath();
    }

    private static class PathIterator implements Iterator<Path> {

        private int i;
        private Path path;

        private PathIterator(Path path) {
            this.path = path;
            this.i = 0;
        }

        @Override
        public boolean hasNext() {
            return i < path.getNameCount();
        }

        @Override
        public Path next() {
            if(hasNext()){
                return path.getName(i++);
            }
            throw new NoSuchElementException("iterator has no more elements");
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("not supported");
        }
    }
}
