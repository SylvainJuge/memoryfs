package sylvain.juge.fsutils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes hash for all files found while walking file tree
 */
public class FileDigestVisitor extends SimpleFileVisitor<Path> {
    private final List<FileHash> hashes;
    private final FileDigest fileDigest;

    public FileDigestVisitor(String algorithm, int bufferSize) {
        hashes = new ArrayList<>();
        fileDigest = new FileDigest(algorithm, bufferSize);
    }

    public List<FileHash> getResult() {
        return hashes;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        hashes.add(new FileHash(file, fileDigest.digest(file)));
        return FileVisitResult.CONTINUE;
    }

    public static final class FileHash {
        private final Path path;
        private final String hash;

        private FileHash(Path path, String hash) {
            this.path = path;
            this.hash = hash;
        }

        public Path getPath() {
            return path;
        }

        public String getHash() {
            return hash;
        }

    }
}
