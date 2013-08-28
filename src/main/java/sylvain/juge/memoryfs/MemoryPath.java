package sylvain.juge.memoryfs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Iterator;

public class MemoryPath implements Path {

    private static final String SEPARATOR = "/";
    private final MemoryFileSystem fs;
    private final String path;
    private final String[] pathParts;
    private final boolean absolute;


    MemoryPath(MemoryFileSystem fs, String path){
        if( null == fs){
            throw new IllegalArgumentException("filesytem required");
        }
        if( null == path || path.isEmpty()){
            throw new IllegalArgumentException("path required not empty, got : "+path);
        }
        this.fs = fs;
        this.path = path;
        pathParts = path.split("/");
        absolute = path.startsWith(SEPARATOR);
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
        return null;
    }

    @Override
    public Path getFileName() {
        return null;
    }

    @Override
    public Path getParent() {
        return null;
    }

    @Override
    public int getNameCount() {
        return 0;
    }

    @Override
    public Path getName(int index) {
        return null;
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        return null;
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
        if(!absolute){
            throw new RuntimeException("how to guess relative path uri ? with current folder ?");
        }
        sb.append(path.substring(1)); // remove 1st / at root of path
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

}
