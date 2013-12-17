package sylvain.juge.fsutils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;


// TODO : make it work when copying form and to the same FS implementations
// -> we can't copy and walk file tree at the same time
// -> requires to first make a list of files to copy, then to copy them

/**
 * Copies files and folder recursively from source to destination.
 * Also works between two different filesystem implementations
 */
public class CopyVisitor extends SimpleFileVisitor<Path> {

    private final Path src;
    private final Path dst;

    public CopyVisitor(Path src, Path dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        return copy(dir);
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        return copy(file);
    }

    private FileVisitResult copy(Path item) throws IOException {
        Path srcItem = src.relativize(item);

        // TODO : we may bypass this step when source and target fs instances rely on same implementation
        Path targetPath = dst;
        for (int i = 0; i < srcItem.getNameCount(); i++) {
            targetPath = targetPath.resolve(srcItem.getName(i).toString());
        }
        Files.copy(item, targetPath);
        return FileVisitResult.CONTINUE;
    }
}
