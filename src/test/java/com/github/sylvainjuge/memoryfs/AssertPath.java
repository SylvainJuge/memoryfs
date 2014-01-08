package com.github.sylvainjuge.memoryfs;

import org.fest.assertions.api.Assertions;
import org.fest.assertions.api.IterableAssert;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.fest.assertions.api.Assertions.fail;

/**
 * Helps to write assertions on memory path, tries to check all possible entry points and invariants for each assertion.
 */
class AssertPath extends IterableAssert<Path> {
    private final Path path;

    public static AssertPath assertThat(Path path) {
        return new AssertPath(path);
    }

    private AssertPath(Path path) {
        super(path);
        this.path = path;
    }

    public AssertPath exists() {
        isInstanceOf(MemoryPath.class);

        Entry entry = MemoryPath.asMemoryPath(path).findEntry();
        MemoryFileSystem fs = MemoryFileSystem.asMemoryFileSystem(path.getFileSystem());
        Assertions.assertThat(entry).isSameAs(fs.findEntry(path));

        try {
            Assertions.assertThat(Files.isSameFile(path, path)).isTrue();
            Assertions.assertThat(Files.isHidden(path)).isFalse();
            Assertions.assertThat(Files.getFileStore(path)).isNotNull();
        } catch (IOException e) {
            fail(e.getMessage());
        }
        Assertions.assertThat(Files.isExecutable(path)).isTrue();
        Assertions.assertThat(Files.isReadable(path)).isTrue();
        Assertions.assertThat(Files.isWritable(path)).isTrue();
        Assertions.assertThat(Files.isSymbolicLink(path)).isFalse();
        return this;
    }

    public AssertPath isDirectory() {
        exists();
        Entry entry = MemoryPath.asMemoryPath(path).findEntry();
        Assertions.assertThat(entry.isDirectory()).isTrue();
        Assertions.assertThat(Files.isDirectory(path)).isTrue();
        Assertions.assertThat(Files.isRegularFile(path)).isFalse();
        return this;
    }

    public AssertPath isFile() {
        exists();
        Entry entry = MemoryPath.asMemoryPath(path).findEntry();
        Assertions.assertThat(entry.isDirectory()).isFalse();
        Assertions.assertThat(Files.isDirectory(path)).isFalse();
        Assertions.assertThat(Files.isRegularFile(path)).isTrue();
        return this;
    }

    public AssertPath isEmptyFile() {
        isFile();
        try {
            Assertions.assertThat(Files.readAllBytes(path)).isEmpty();
            Assertions.assertThat(Files.newInputStream(path).read()).isLessThan(0);

            ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[1]);
            Assertions.assertThat(byteBuffer.position()).isEqualTo(0);
            Assertions.assertThat(Files.newByteChannel(path).read(byteBuffer)).isLessThan(0);
            Assertions.assertThat(byteBuffer.position()).isEqualTo(0); // buffer remains untouched

        } catch (IOException e) {
            fail(e.getMessage());
        }
        return this;
    }

    public AssertPath isEmptyDirectory() {
        isDirectory();

        Assertions.assertThat(MemoryPath.asMemoryPath(path).findEntry().getEntries()).isNull();

        DirectoryStream<Path> dirStream = null;
        try {
            dirStream = Files.newDirectoryStream(path);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        Assertions.assertThat(dirStream).isNotNull();
        iteratorNoMoreItems(dirStream.iterator());
        return this;
    }

    private void iteratorNoMoreItems(Iterator<?> it) {
        Assertions.assertThat(it).isNotNull();
        Assertions.assertThat(it.hasNext()).isFalse();
        boolean thrown = false;
        try {
            it.next();
        } catch (NoSuchElementException e) {
            thrown = true;
        }
        Assertions.assertThat(thrown).isTrue();
    }

    public AssertPath isRoot() {
        exists();
        isDirectory();
        Assertions.assertThat(path.getParent()).isNull();
        Assertions.assertThat(path.isAbsolute()).isTrue();
        return this;
    }

    private static void assertDoesNotExists(IOTask... tasks) {
        for (IOTask task : tasks) {
            Throwable thrown = null;
            try {
                task.run();
            } catch (IOException e) {
                thrown = e;
            }
            Assertions.assertThat(thrown).isInstanceOf(DoesNotExistsException.class);
        }
    }

    private static interface IOTask {
        public void run() throws IOException;
    }

    public AssertPath doesNotExists() {
        Assertions.assertThat(MemoryPath.asMemoryPath(path).findEntry()).isNull();
        Assertions.assertThat(Files.exists(path)).isFalse();

        assertDoesNotExists(
                new IOTask() {
                    @Override
                    public void run() throws IOException {
                        Files.newDirectoryStream(path);
                    }
                }, new IOTask() {
                    @Override
                    public void run() throws IOException {
                        Path target = path.resolveSibling("fakeTarget");
                        Files.copy(path, target);
                    }
                }, new IOTask() {
                    @Override
                    public void run() throws IOException {
                        Files.newByteChannel(path);
                    }
                }, new IOTask() {
                    @Override
                    public void run() throws IOException {
                        Path target = path.resolveSibling("fakeTarget");
                        Files.move(path, target);
                    }
                }, new IOTask() {
                    @Override
                    public void run() throws IOException {
                        Files.getFileStore(path);
                    }
                }
        );
        return this;
    }

    public AssertPath isAbsolute() {
        Assertions.assertThat(path.isAbsolute()).isTrue();
        assertThat(path.getRoot()).isRoot();
        Assertions.assertThat(path.toAbsolutePath()).isEqualTo(path);
        return this;
    }

    public AssertPath isRelative() {
        Assertions.assertThat(path.isAbsolute()).isFalse();
        // relative path does not have root
        Assertions.assertThat(path.getRoot()).isNull();

        Path absolutePath = path.toAbsolutePath();
        assertThat(absolutePath).isAbsolute();

        absolutePath.endsWith(path);
        absolutePath.endsWith(MemoryPath.asMemoryPath(path).getPath());

        return this;
    }

    public AssertPath contains(byte[] data) {
        isFile();
        byte[] actual = new byte[0];
        try {
            actual = Files.readAllBytes(path);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        Assertions.assertThat(actual).isEqualTo(data);
        return this;
    }

    public AssertPath contains(Path item) {
        Assertions.assertThat(getContent()).contains(item);
        return this;
    }

    public AssertPath containsExactly(Path... items) {
        Assertions.assertThat(getContent()).containsExactly(items);
        return this;
    }

    private List<Path> getContent() {
        isDirectory();
        List<Path> paths = new ArrayList<>();
        try {
            for (Path p : Files.newDirectoryStream(path)) {
                Assertions.assertThat(p.getParent()).isEqualTo(path);
                paths.add(p);
            }
        } catch (IOException e) {
            fail(e.getMessage());
        }
        return paths;
    }
}