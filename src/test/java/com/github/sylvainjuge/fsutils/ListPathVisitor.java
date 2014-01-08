package com.github.sylvainjuge.fsutils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Lists all paths found while walking file tree
 */
public class ListPathVisitor extends SimpleFileVisitor<Path> {

    private final List<Path> paths;

    public ListPathVisitor() {
        this.paths = new ArrayList<>();
    }

    public List<Path> getList() {
        return paths;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        paths.add(dir);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        paths.add(file);
        return FileVisitResult.CONTINUE;
    }

}
