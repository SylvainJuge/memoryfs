package sylvain.juge.memoryfs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

/**
 * Helps to write assertions on memory path, tries to check all possible entry points and invariants for each assertion.
 */
class AssertPath {
    private final MemoryPath path;
    private final MemoryFileSystem fs;

    public static AssertPath assertPath(Path path){
        return new AssertPath(path);
    }

    private AssertPath(Path path){
        assertThat(path).isNotNull();
        this.path = MemoryPath.asMemoryPath(path);
        this.fs = MemoryFileSystem.asMemoryFileSystem(path.getFileSystem());
    }

    public AssertPath exists(){
        Entry entry = path.findEntry();
        assertThat(entry).isSameAs(fs.findEntry(path));
        try {
            assertThat(Files.isSameFile(path, path)).isTrue();
            assertThat(Files.isHidden(path)).isFalse();
        } catch (IOException e) {
            fail(e.getMessage());
        }
        assertThat(Files.isExecutable(path)).isTrue();
        assertThat(Files.isReadable(path)).isTrue();
        assertThat(Files.isWritable(path)).isTrue();
        assertThat(Files.isSymbolicLink(path)).isFalse();
        return this;
    }

    public AssertPath isDirectory(){
        exists();
        Entry entry = path.findEntry();
        assertThat(entry.isDirectory()).isTrue();
        assertThat(Files.isDirectory(path)).isTrue();
        assertThat(Files.isRegularFile(path)).isFalse();
        return this;
    }

    public AssertPath isFile(){
        exists();
        Entry entry = path.findEntry();
        assertThat(entry.isDirectory()).isFalse();
        assertThat(Files.isDirectory(path)).isFalse();
        assertThat(Files.isRegularFile(path)).isTrue();
        return this;
    }

    public AssertPath isEmpty(){
        exists();
        Entry entry = path.findEntry();
        if (entry.isDirectory()) {

            assertThat(entry.getEntries()).isNull();

            DirectoryStream<Path> dirStream = null;
            try {
                dirStream = Files.newDirectoryStream(path);
            } catch (IOException e) {
                fail(e.getMessage());
            }
            assertThat(dirStream).isNotNull();
            iteratorNoMoreItems(dirStream.iterator());

        } else {
            try {
                assertThat(Files.readAllBytes(path)).isEmpty();
                assertThat(Files.newInputStream(path).read()).isLessThan(0);

                ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[1]);
                assertThat(byteBuffer.position()).isEqualTo(0);
                assertThat(Files.newByteChannel(path).read(byteBuffer)).isLessThan(0);
                assertThat(byteBuffer.position()).isEqualTo(0); // buffer remains untouched

            } catch (IOException e) {
                fail(e.getMessage());
            }
        }
        return this;
    }

    private void iteratorNoMoreItems(Iterator<?> it){
        assertThat(it).isNotNull();
        assertThat(it.hasNext()).isFalse();
        boolean thrown = false;
        try {
            it.next();
        } catch (NoSuchElementException e) {
            thrown = true;
        }
        assertThat(thrown).isTrue();
    }

    public AssertPath isRoot(){
        exists();
        isDirectory();
        assertThat(path.getParent()).isNull();
        assertThat(path.isAbsolute()).isTrue();
        return this;
    }

    public AssertPath doesNotExists() {
        assertThat(path.findEntry()).isNull();
        assertThat(Files.exists(path)).isFalse();

        Throwable thrown = null;

        try {
            fs.newDirectoryStream(path);
        } catch (IOException | DoesNotExistsException e) {
            thrown = e;
        }
        assertThat(thrown).isInstanceOf(DoesNotExistsException.class);

        try {
            fs.copy(path, null);
        } catch (DoesNotExistsException e) {
            thrown = e;
        }
        assertThat(thrown).isInstanceOf(DoesNotExistsException.class);

        try {
            fs.newByteChannel(path, EnumSet.noneOf(StandardOpenOption.class));
        } catch (DoesNotExistsException e) {
            thrown = e;
        }
        assertThat(thrown).isInstanceOf(DoesNotExistsException.class);

        try {
            fs.move(path, null);
        } catch (DoesNotExistsException e) {
            thrown = e;
        }
        assertThat(thrown).isInstanceOf(DoesNotExistsException.class);

        return this;
    }

    public AssertPath isAbsolute(){
        assertThat(path.isAbsolute()).isTrue();
        assertPath(path.getRoot()).isRoot();
        assertThat(path.toAbsolutePath()).isEqualTo(path);
        return this;
    }

    public AssertPath isRelative(){
        assertThat(path.isAbsolute()).isFalse();
        // relative path does not have root
        assertThat(path.getRoot()).isNull();

        Path absolutePath = path.toAbsolutePath();
        assertPath(absolutePath).isAbsolute();

        absolutePath.endsWith(path);
        absolutePath.endsWith(path.getPath());

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
        assertThat(actual).isEqualTo(data);
        return this;
    }
}
