package sylvain.juge.memoryfs;

import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static java.nio.file.Files.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.*;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static sylvain.juge.memoryfs.AssertPath.assertPath;

public class MemoryFileSystemTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullProviderNotAllowed() {
        MemoryFileSystem.builder(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullIdNotAllowed() {
        MemoryFileSystem
                .builder(newProvider())
                .id(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void negativeCapacityNotAllowed() {
        MemoryFileSystem
                .builder(newProvider())
                .capacity(-1);
    }

    @Test
    public void defaultId() {
        MemoryFileSystemProvider provider = newProvider();
        MemoryFileSystem fs = MemoryFileSystem
                .builder(provider)
                .build();
        assertThat(fs.getId()).isEqualTo("");
    }

    @Test
    public void openCloseState() throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        assertThat(fs.isOpen()).isTrue();
        fs.close();
        assertThat(fs.isOpen()).isFalse();
    }

    @Test
    public void hasNonDefaultToString() {
        MemoryFileSystem fs = newMemoryFs();
        assertThat(fs.toString().startsWith(fs.getClass().getName() + "@")).isFalse();
    }

    @Test
    public void doesNotSupportAnyAttributeView() {
        assertThat(newMemoryFs().supportedFileAttributeViews()).isEmpty();
    }

    @Test
    public void separatorIsSlash() {
        assertThat(newMemoryFs().getSeparator()).isEqualTo("/");
    }

    @Test
    public void buildThroughBuilderWithExplicitId() {
        MemoryFileSystem fs = MemoryFileSystem
                .builder(newProvider())
                .id("id")
                .build();
        assertThat(fs.getId()).isEqualTo("id");
    }

    @Test
    public void buildThroughUriWithExplicitIdInUri() throws IOException {
        URI uri = URI.create("memory:/fsId");
        MemoryFileSystem fs = (MemoryFileSystem) newProvider().newFileSystem(uri, null);
        assertThat(fs.getId()).isEqualTo("fsId");
    }

    @Test
    public void buildThroughUriWithoutExplicitId() throws IOException {
        URI uri = URI.create("memory:/");
        MemoryFileSystem fs = (MemoryFileSystem) newProvider().newFileSystem(uri, null);
        assertThat(fs.getId()).isEqualTo("");
    }

    @Test
    public void fsRegistrationInProvider() throws IOException {
        MemoryFileSystemProvider provider = newProvider();
        assertThat(provider.registeredFileSystems()).isEmpty();

        MemoryFileSystem fs = MemoryFileSystem.builder(provider).build();
        assertThat(provider.getFileSystem(URI.create("memory:/"))).isSameAs(fs);
        assertThat(provider.registeredFileSystems().get("")).isSameAs(fs);

        fs.close();

        assertThat(provider.registeredFileSystems()).isEmpty();
    }

    // TODO : create FS through Path ?
    // TODO : create FS with URI and with explicit parameters
    // TODO : test for concurrent access on open/close state

    @Test
    public void createFsWithCapacity() throws IOException {

        int capacity = 100;

        try (FileSystem fs = MemoryFileSystem
                .builder(newProvider())
                .capacity(capacity)
                .build()) {
            assertThat(fs.getFileStores()).hasSize(1);
            for (FileStore store : fs.getFileStores()) {
                assertThat(store.name()).isEqualTo("");
                assertThat(store.getTotalSpace()).isEqualTo(capacity);
                assertThat(store.getUsableSpace()).isEqualTo(capacity);
                assertThat(store.getUnallocatedSpace()).isEqualTo(capacity);
            }
        }
    }

    @Test
    public void buildWithDefaultValues() throws IOException {
        MemoryFileSystem fs = newMemoryFs();

        assertThat(fs.getId()).isEqualTo("");

        // singlefile store by default or size zero
        assertThat(fs.getFileStores()).hasSize(1);
        for (FileStore store : fs.getFileStores()) {
            assertThat(store.getTotalSpace()).isEqualTo(0);
            assertThat(store.getUnallocatedSpace()).isEqualTo(0);
            assertThat(store.getUsableSpace()).isEqualTo(0);
        }
    }

    @Test
    public void getPathFromParts() {
        MemoryFileSystem fs = newMemoryFs();
        assertThat(fs.getPath("/absoluteSingle")).isEqualTo(MemoryPath.create(fs, "/absoluteSingle"));
        assertThat(fs.getPath("relative")).isEqualTo(MemoryPath.create(fs, "relative"));

        assertThat(fs.getPath("/a", "path")).isEqualTo(MemoryPath.create(fs, "/a/path"));
        assertThat(fs.getPath("a", "relative")).isEqualTo(MemoryPath.create(fs, "a/relative"));

        assertThat(fs.getPath("a/b", "c/d", "e")).isEqualTo(MemoryPath.create(fs, "a/b/c/d/e"));

    }

    @Test
    public void rootPathIsTheOnlyRoot() throws IOException {
        // with or without id, the fs root remains the same
        checkRootDirectories(newMemoryFs(), "/");
        checkRootDirectories(MemoryFileSystem.builder(newProvider()).id("id").build(), "/");
    }

    @Test
    public void rootEntryAlwaysAvailable() {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath root = MemoryPath.createRoot(fs);

        assertPath(root)
                .isRoot()
                .isEmpty();
    }

    @Test
    public void pathToNonExisting() {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath nonExistingPath = MemoryPath.create(fs, "non/existing/path");

        assertPath(nonExistingPath).doesNotExists();
    }

    @Test
    public void createEmptyDirectory() throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath root = MemoryPath.createRoot(fs);
        Path directory = root.resolve("directory");

        assertPath(directory).doesNotExists();

        createDirectory(directory);

        assertPath(directory)
                .isDirectory()
                .isEmpty();
    }

    @Test
    public void createEmptyFile() throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath file = MemoryPath.create(fs, "/file");

        assertPath(file).doesNotExists();

        createFile(file);

        assertPath(file)
                .isFile()
                .isEmpty();
    }

    @Test
    public void directoryStreamListsFolderContent() throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath root = MemoryPath.createRoot(fs);

        // should list only files within folder, not deeper

        Path file = root.resolve("file");
        Path folder = root.resolve("folder");
        Path file1 = root.resolve("folder/file1");
        Path file2 = root.resolve("folder/file2");

        createFile(file);
        assertPath(file).isFile();

        createDirectory(folder);
        createFile(file1);
        createFile(file2);

        // only file and folder must be in directory stream.

        // TODO : add method on assertPath to check folder contains other paths
        assertThat(newDirectoryStream(root)).containsExactly(file, folder);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void directoryStreamIteratorNotModifiable() throws IOException {
        MemoryPath root = MemoryPath.createRoot(newMemoryFs());
        newDirectoryStream(root).iterator().remove();
    }

    @Test
    public void emptyDirectoryIsEmpty() {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath root = MemoryPath.createRoot(fs);

        assertPath(root)
                .isDirectory()
                .isEmpty();
    }

    @Test(expectedExceptions = NotDirectoryException.class)
    public void tryDirectoryStreamOnFile() throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        Path file = MemoryPath.createRoot(fs).resolve("file");
        createFile(file);
        assertPath(file).isFile();

        newDirectoryStream(file);
    }

    @Test(expectedExceptions = DoesNotExistsException.class)
    public void tryDirectoryStreamOnNonExistingFolder() throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        Path folder = MemoryPath.createRoot(fs).resolve("folder");
        assertPath(folder).doesNotExists();

        newDirectoryStream(folder);
    }

    @Test(expectedExceptions = ConflictException.class)
    public void tryToCreateConflictByCreatingParents() throws IOException {

        MemoryFileSystem fs = newMemoryFs();
        Path fileConflict = MemoryPath.createRoot(fs).resolve("fileConflict");
        createFile(fileConflict);

        Path fileToCreate = fileConflict.resolve("subfolder/file");
        createDirectories(fileToCreate.getParent());
        createFile(fileToCreate);
        fail("should have thrown exception");
    }

    @Test(expectedExceptions = DoesNotExistsException.class)
    public void tryToCreateFolderWithMissingParents() throws IOException {
        failsToCreateWithMissingParent(true);
    }

    @Test(expectedExceptions = DoesNotExistsException.class)
    public void tryToCreateFileWithMissingParents() throws IOException {
        failsToCreateWithMissingParent(false);
    }

    @Test(expectedExceptions = ProviderMismatchException.class)
    public void safeCastWithUnexpectedClass() {
        MemoryFileSystem.asMemoryFileSystem(FileSystems.getDefault());
    }

    private static void failsToCreateWithMissingParent(boolean directory) throws IOException {
        // createFile and createDirectory methods should not silently create parent folders
        MemoryFileSystem fs = newMemoryFs();

        MemoryPath path = MemoryPath.create(fs, "/anywhere/beyond/root");
        assertPath(path).doesNotExists();
        assertPath(path.getParent()).doesNotExists();

        if (directory) {
            createDirectory(path);
        } else {
            createFile(path);
        }
    }

    @Test(expectedExceptions = ConflictException.class)
    void failsToCreateWhenAlreadyExists() throws IOException {
        MemoryFileSystem fs = newMemoryFs();

        MemoryPath path = MemoryPath.create(fs, "/existing");
        createFile(path);
        assertPath(path).isFile();

        createFile(path);
    }

    @Test
    public void createDirectoryWithParents() throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath path = MemoryPath.create(fs, "/not/in/root");

        assertPath(path).doesNotExists();

        createDirectories(path);

        assertPath(path).isDirectory();
    }

    @Test(expectedExceptions = ConflictException.class)
    public void createWithParentsConflict() throws IOException {
        // when there exist a file entry which exists and is not a directory
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath conflictingFile = MemoryPath.create(fs, "/a/b");
        MemoryPath conflict = MemoryPath.create(fs, "/a/b/c");

        createDirectories(conflictingFile.getParent());
        createFile(conflictingFile);

        // will fail because "b" parent folder already created as file (and not directory)
        createDirectories(conflict);
    }

    @Test
    public void deleteThenReCreateFile() throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath path = MemoryPath.create(fs, "/a/b");

        createDirectory(path.getParent());
        createFile(path);

        assertPath(path).isFile();

        delete(path);

        assertPath(path).doesNotExists();

        // parent folder is not deleted
        assertPath(path.getParent()).isDirectory();

        // once deleted, we can create it again
        createFile(path);
        assertPath(path).isFile();
    }

    // Note : probably already partialy tested at entry level
    // see if it allows more coverage, otherwise delete this test
    @Test
    public void deleteFolderDeletesItsContent() throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath folder = MemoryPath.create(fs, "/a");
        MemoryPath file = MemoryPath.create(fs, "/a/b");

        createDirectory(folder);
        createFile(file);
        assertPath(file).exists();

        delete(folder);

        assertPath(folder).doesNotExists();
        assertPath(file).doesNotExists();

    }

    @Test(expectedExceptions = InvalidRequestException.class)
    public void tryToDeleteRoot() {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath.createRoot(fs).findEntry().delete();
    }


    @Test
    public void copyDirectoryDoesNotCopyItsContent() throws IOException {
        // This is defautlt copy behavior, we have to use a dedicated visitor
        // to perform an in-depth copy
        copyOrMoveDirectoryContent(true);
    }

    @Test
    public void moveDirectoryMovesItsContent() throws IOException {
        // When moving a folder, everything inside have to remain in place
        copyOrMoveDirectoryContent(false);
    }

    private static void copyOrMoveDirectoryContent(boolean copy) throws IOException {
        // copy directory does not copy content
        // move directory moves content

        MemoryFileSystem fs = newMemoryFs();
        Path root = MemoryPath.createRoot(fs);
        Path source = root.resolve("source");
        Path fileInFolder = source.resolve("file");

        createDirectory(source);
        createFile(fileInFolder);

        Path target = root.resolve("target");
        assertPath(target).doesNotExists();

        if (copy) {
            fs.copy(source, target);
        } else {
            fs.move(source, target);
        }

        assertPath(target).isDirectory();
        if (copy) {
            assertPath(target).isEmpty();
        } else {
            assertPath(target).contains(target.resolve("file"));
            assertPath(fileInFolder).doesNotExists();
        }
    }

    @Test(expectedExceptions = ConflictException.class)
    public void tryToCopyCreateConflict() throws IOException {
        tryToCopyOrMoveCreateConflict(true);
    }

    @Test(expectedExceptions = ConflictException.class)
    public void tryToMoveCreateConflict() throws IOException {
        tryToCopyOrMoveCreateConflict(false);
    }

    public void tryToCopyOrMoveCreateConflict(boolean copy) throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        Path root = MemoryPath.createRoot(fs);
        Path folder = root.resolve("folder");
        Path conflict = root.resolve("conflict");
        createDirectory(folder);
        createFile(conflict);

        if (copy) {
            copy(folder, conflict);
        } else {
            move(folder, conflict);
        }
    }

    @Test
    public void copyDirectoryConflictOverwriteDoesNotDelete() throws IOException {
        copyOrMoveDirectoryConflictOverwriteDoesNotDelete(true);
    }

    @Test
    public void moveDirectoryConflictOverwriteDoesNotDelete() throws IOException {
        copyOrMoveDirectoryConflictOverwriteDoesNotDelete(false);
    }

    private static void copyOrMoveDirectoryConflictOverwriteDoesNotDelete(boolean copy) throws IOException {
        // when there is a conflict while copying/moving a directory,
        // the overwrite option should just skip error, and the existing
        // folder content must remain unaltered (only attributes may be copied, and since we don't handle them yet...)

        // create folder with one file
        // create another folder, and copy/move to 1st folder as target with overwrite
        // the file within initial folder must still be there

        // in plain english : we move/copy iceberg into titanic
        // and the captain must still be there

        MemoryFileSystem fs = newMemoryFs();
        Path root = MemoryPath.createRoot(fs);
        Path titanic = createDirectories(root.resolve("titanic"));
        Path boatCaptain = createFile(titanic.resolve("captain"));

        Path iceberg = createDirectory(root.resolve("iceberg"));

        if (copy) {
            copy(iceberg, titanic, REPLACE_EXISTING);
        } else {
            move(iceberg, titanic, REPLACE_EXISTING);
        }

        if (copy) {
            // with copy, original iceberg is still in place
            assertPath(iceberg).exists();
        } else {
            // with move, it has been merged with the boat
            assertPath(iceberg).doesNotExists();
        }

        // whatever the scenario, captain is still on the boat
        assertPath(boatCaptain).isFile();

        // since we don't handle attributes, there is nothing special to check on target folder

    }

    @Test
    public void copyFileCopiesItsContent() throws IOException {
        // create file with reference data
        // copy this file
        // alter original file data
        // copy must have the initial reference data (thus a copy)

        MemoryFileSystem fs = newMemoryFs();
        Path root = MemoryPath.createRoot(fs);
        Path original = root.resolve("original");

        createFile(original);
        write(original, new byte[]{1, 2, 3});
        assertPath(original).contains(new byte[]{1, 2, 3});

        Path copy = root.resolve("copy");
        copy(original, copy);

        write(original, new byte[]{4, 5, 6});
        assertPath(original).contains(new byte[]{4, 5, 6});

        assertPath(copy).contains(new byte[]{1, 2, 3});

    }

    @Test(expectedExceptions = DoesNotExistsException.class)
    public void tryToCopyMissingFile() throws IOException {
        tyToCopyOrMoveMissingFile(true);
    }

    @Test(expectedExceptions = DoesNotExistsException.class)
    public void tryToMoveMissingFile() throws IOException {
        tyToCopyOrMoveMissingFile(false);
    }

    private static void tyToCopyOrMoveMissingFile(boolean copy) throws IOException {
        // self-explainatory
        MemoryFileSystem fs = newMemoryFs();
        Path root = MemoryPath.createRoot(fs);
        Path missing = root.resolve("missing");
        Path target = root.resolve("target");
        if (copy) {
            copy(missing, target);
        } else {
            move(missing, target);
        }
    }

    @Test(enabled = false, expectedExceptions = ConflictException.class)
    public void tryToCreateConflictWithMove() {
        // create folder1 with a file "a"
        // create file "a"
        // move "a" to /folder => must throw an exception
    }

    @Test(enabled = false)
    public void copyConflictWithOverwrite() {
        // create file with some known reference data
        // same as previous, but with overwrite
        // and should properly overwrite original data
    }

    // copy :
    // - copy file must copy its data, copy folder should not copy its content
    // - do we need to create parents when they do not exist ?
    // - overwrite allows to overwrite files and merge folders
    // - if conflict without overwrite, must throw an exception

    // Nice improvement : provide option to copy recursively

    // move :
    // - do we need to create parents when they do not exist ?
    // overwrite allows to overwrite if there is a (name) conflict
    // if conflict without overwrite, what should we do ? merge ?
    // -> Supporting an atomic move may be useful when such conflicts may arise


    @Test
    public void readChannel() throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath filePath = MemoryPath.create(fs, "/file");

        // create file manually directly in guts of fs impl
        Entry entry = fs.createEntry(filePath, false, true);
        byte[] data = filePath.getPath().getBytes();
        entry.getData().asOutputStream().write(data);

        assertPath(filePath)
                .isFile()
                .contains(data);
    }

    @Test(expectedExceptions = InvalidRequestException.class)
    public void tryToReadFolder() throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath folder = MemoryPath.create(fs, "/folder");
        createDirectory(folder);

        newByteChannel(folder, EnumSet.of(READ));
    }

    @Test(expectedExceptions = DoesNotExistsException.class)
    public void tryToReadMissingFile() throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath folder = MemoryPath.create(fs, "/missingFile");
        newByteChannel(folder, READ);
    }

    @Test(expectedExceptions = DoesNotExistsException.class)
    public void tryToWriteMissingFile() throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath folder = MemoryPath.create(fs, "/missingFile");
        newByteChannel(folder, WRITE);
    }

    @Test
    public void noReadWriteOptionDefaultsToRead() throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath file = MemoryPath.create(fs, "/file");
        createFile(file);

        SeekableByteChannel channel = newByteChannel(file, EnumSet.noneOf(StandardOpenOption.class));

        // if channel is not a read channel, this call will throw an exception
        channel.read(ByteBuffer.wrap(new byte[0]));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void tryReadAndWriteChannel() throws IOException {
        // at most one of read or write options is required
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath path = MemoryPath.create(fs, "/file");
        newByteChannel(path, READ, WRITE);
    }

    @Test(expectedExceptions = DoesNotExistsException.class)
    public void tryToWriteMissingFileWithoutRequestToCreate() throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath file = MemoryPath.create(fs, "/file");
        newByteChannel(file, WRITE);
    }

    @Test
    public void writeMissingFileAndRequestCreate() throws IOException {
        writeMissingCreateNew(WRITE, CREATE);
    }

    @Test
    public void writeMissingFileAndRequestCreateNew() throws IOException {
        writeMissingCreateNew(WRITE, CREATE_NEW);
    }

    @Test(expectedExceptions = ConflictException.class)
    public void tryWriteExistingFileAndRequestCreateNew() {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath file = MemoryPath.create(fs, "/file");
        fs.createEntry(file, false, true);
        assertThat(fs.findEntry(file)).isNotNull();

        fs.newByteChannel(file, EnumSet.of(WRITE, CREATE_NEW));
    }

    public SeekableByteChannel writeMissingCreateNew(OpenOption... options) throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath file = MemoryPath.create(fs, "/file");
        assertPath(file).doesNotExists();

        SeekableByteChannel channel = newByteChannel(file, options);
        assertThat(channel).isNotNull();

        assertPath(file).exists().isEmpty();

        return channel;
    }

    @Test
    public void tryToTruncateWithReadChannel() throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath file = MemoryPath.create(fs, "/file");
        byte[] data = {1, 2, 3};
        newByteChannel(file, WRITE, CREATE).write(ByteBuffer.wrap(data));

        // truncate should just be ignored
        newByteChannel(file, READ, TRUNCATE_EXISTING);

        assertPath(file)
                .isFile()
                .contains(data);
    }

    @Test
    public void tryChannelWithUnsupportedOptions() throws IOException {
        // we bypass most of checks by trying to write to an existing file
        // but must fail since we try to use an insupported option.
        for (StandardOpenOption unsuported : Arrays.asList(SPARSE, DELETE_ON_CLOSE, SYNC, DSYNC)) {
            boolean thrown = false;
            try {
                writeMissingCreateNew(WRITE, CREATE, unsuported);
            } catch (UnsupportedOperationException e) {
                thrown = true;
            }
            assertThat(thrown).describedAs("should not support channel option : " + unsuported).isTrue();
        }
    }

    @Test
    public void writeAppend() throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath file = MemoryPath.create(fs, "/file");

        write(file, new byte[]{1, 2, 3, 4}, WRITE, CREATE_NEW);
        assertPath(file).contains(new byte[]{1, 2, 3, 4});

        write(file, new byte[]{5, 6, 7, 8}, WRITE, APPEND);
        assertPath(file).contains(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
    }

    @Test
    public void writeTruncate() throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath file = MemoryPath.create(fs, "/file");

        assertPath(file).doesNotExists();

        write(file, new byte[]{1, 2, 3, 4}, WRITE, CREATE_NEW);
        assertPath(file).contains(new byte[]{1, 2, 3, 4});

        write(file, new byte[]{5, 6, 7, 8}, WRITE, TRUNCATE_EXISTING);
        assertPath(file).contains(new byte[]{5, 6, 7, 8});
    }

    // watch service not implemented
    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void watchServiceNotImplemented() throws IOException {
        newMemoryFs().newWatchService();
    }

    // user principal service not implemented
    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void userPrincipalServiceNotImplemented() {
        newMemoryFs().getUserPrincipalLookupService();
    }

    private static void checkRootDirectories(MemoryFileSystem fs, String root, String... expectedSubPaths) throws IOException {
        MemoryPath rootPath = MemoryPath.create(fs, root);
        assertThat(fs.getRootDirectories()).containsOnly(rootPath);

        List<Path> subPaths = getSubPaths(rootPath);
        assertThat(subPaths).isEqualTo(toPath(fs, expectedSubPaths));
    }

    private static List<Path> getSubPaths(final Path start) throws IOException {
        final List<Path> subPaths = new ArrayList<>();
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!dir.equals(start)) {
                    subPaths.add(dir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                subPaths.add(file);
                return FileVisitResult.CONTINUE;
            }
        });
        return subPaths;
    }

    private static List<Path> toPath(MemoryFileSystem fs, String... paths) {
        List<Path> result = new ArrayList<>();
        for (String path : paths) {
            result.add(MemoryPath.create(fs, path));
        }
        return result;
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void pathMatcherNotImplemented() {
        newMemoryFs().getPathMatcher(null);

        // /* : all items in root
        // /a/* : all items in folder a
        // /abc* : all items in root that start with "abc"
        // ab*def : all items that start with "ab" and end with "def"
        // a?c : all items of 3 characters whose middle character is unknown
        //
        // implementation idea :
        // transform input to set of matchers, and use special matchers for wildcards
        // * : will match anything
        // ** : will match anything at any depth
        // use pcre regex for other cases.
    }

    private static MemoryFileSystem newMemoryFs() {
        MemoryFileSystem fs = MemoryFileSystem.builder(newProvider()).build();
        assertThat(fs.isReadOnly()).isFalse();
        return fs;
    }

    private static MemoryFileSystemProvider newProvider() {
        return new MemoryFileSystemProvider();
    }

}
