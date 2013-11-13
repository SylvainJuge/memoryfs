package sylvain.juge.memoryfs;

import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

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
    public void findNonExistingEntry() {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath nonExistingPath = MemoryPath.create(fs, "non/existing/path");

        assertPath(nonExistingPath)
                .doesNotExists();
    }

    @Test
    public void createDirectoryEntry() {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath root = MemoryPath.createRoot(fs);
        Path directory = root.resolve("directory");

        assertPath(directory)
                .doesNotExists();

        fs.createEntry(directory, true, false);

        assertPath(directory)
                .isDirectory();
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

        Files.createFile(file);
        assertPath(file).isFile();

        Files.createDirectory(folder);
        Files.createFile(file1);
        Files.createFile(file2);

        // only file and folder must be in directory stream.

        DirectoryStream<Path> stream = fs.newDirectoryStream(root);
        assertThat(stream).containsExactly(file, folder);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void directoryStreamIteratorNotModifiable() throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath root = MemoryPath.createRoot(fs);

        fs.newDirectoryStream(root);

        fs.newDirectoryStream(root).iterator().remove();
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
        Files.createFile(file);
        assertPath(file).isFile();

        fs.newDirectoryStream(file);
    }

    @Test(expectedExceptions = DoesNotExistsException.class)
    public void tryDirectoryStreamOnNonExistingFolder() throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        Path folder = MemoryPath.createRoot(fs).resolve("folder");
        assertPath(folder).doesNotExists();

        fs.newDirectoryStream(folder);
    }

    @Test(expectedExceptions = ConflictException.class)
    public void tryToCreateConflictByCreatingParents() {

        MemoryFileSystem fs = newMemoryFs();
        Path fileConflict = MemoryPath.createRoot(fs).resolve("fileConflict");
        fs.createEntry(fileConflict, false, false);

        Path fileToCreate = fileConflict.resolve("subfolder/file");
        fs.createEntry(fileToCreate, false, true);
        fail("should have thrown exception");
    }

    @Test(expectedExceptions = DoesNotExistsException.class)
    public void failsToCreateFolderWithMissingParents() {
        failsToCreateWithMissingParent(true);
    }

    @Test(expectedExceptions = DoesNotExistsException.class)
    public void failsToCreateFileWithMissingParents() {
        failsToCreateWithMissingParent(false);
    }

    @Test(expectedExceptions = ProviderMismatchException.class)
    public void safeCastWithUnexpectedClass() {
        MemoryFileSystem.asMemoryFileSystem(FileSystems.getDefault());
    }

    private static void failsToCreateWithMissingParent(boolean directory) {
        MemoryFileSystem fs = newMemoryFs();

        MemoryPath path = MemoryPath.create(fs, "/anywhere/beyond/root");
        assertPath(path).doesNotExists();
        assertPath(path.getParent()).doesNotExists();

        fs.createEntry(path, directory, false);
    }

    @Test(expectedExceptions = ConflictException.class)
    void failsToCreateWhenAlreadyExists() throws IOException {
        MemoryFileSystem fs = newMemoryFs();

        MemoryPath path = MemoryPath.create(fs, "/existing");
        Files.createFile(path);
        assertPath(path).isFile();

        Files.createFile(path);
    }

    @Test
    public void createDirectoryEntryWithParents() {
        createWithParents(true);
    }

    @Test
    public void createFileEntryWithParents() {
        createWithParents(false);
    }

    private static void createWithParents(boolean directory) {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath path = MemoryPath.create(fs, "/not/in/root");
        assertThat(fs.findEntry(path)).isNull();

        Entry entry = fs.createEntry(path, directory, true);
        assertThat(entry).isNotNull();
        assertThat(entry.isDirectory()).isEqualTo(directory);

        Entry parentFolder = fs.findEntry(path.getParent());
        assertThat(parentFolder.isDirectory()).isTrue();
        assertThat(parentFolder).describedAs("missing parent folder " + path.getParent()).isNotNull();
    }

    @Test(expectedExceptions = ConflictException.class)
    public void createWithParentsConflict() {
        // when there exist a file entry which exists and is not a directory
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath conflictingFile = MemoryPath.create(fs, "/a/b");
        MemoryPath fileToCreate = MemoryPath.create(fs, "/a/b/c");

        fs.createEntry(conflictingFile, false, true);
        // will fail because parent folder already created as file (and not directory)
        fs.createEntry(fileToCreate, false, true);
    }

    @Test
    public void deleteThenReCreateFile() {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath path = MemoryPath.create(fs, "/a/b");
        Entry entry = fs.createEntry(path, false, true);
        Entry parentFolder = entry.getParent();
        assertThat(fs.findEntry(path)).isSameAs(entry);
        entry.delete();
        assertThat(fs.findEntry(path)).isNull();

        // parent folder is not deleted
        assertThat(fs.findEntry(path.getParent())).isSameAs(parentFolder);

        // once deleted, we can create it again
        Entry reCreatedEntry = fs.createEntry(path, false, true);
        assertThat(reCreatedEntry.getParent()).isSameAs(parentFolder);
    }

    // Note : probably already partialy tested at entry level
    // see if it allows more coverage, otherwise delete this test
    @Test
    public void deleteFolderDeletesItsContent() {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath folder = MemoryPath.create(fs, "/a");
        MemoryPath leaf = MemoryPath.create(fs, "/a/b");

        fs.createEntry(leaf, false, true);
        assertThat(fs.findEntry(leaf)).isNotNull();
        folder.findEntry().delete();
        assertThat(fs.findEntry(folder)).isNull();
        assertThat(fs.findEntry(leaf)).isNull();

    }

    @Test(expectedExceptions = InvalidRequestException.class)
    public void tryToDeleteRoot() {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath.createRoot(fs).findEntry().delete();
    }


    @Test
    public void copyDirectoryDoesNotCopyItsContent() {
        // This is defautlt copy behavior, we have to use a dedicated visitor
        // to perform an in-depth copy
        copyOrMoveDirectoryContent(true);
    }

    @Test
    public void moveDirectoryMovesItsContent() {
        // When moving a folder, everything inside have to remain in place
        copyOrMoveDirectoryContent(false);
    }

    private static void copyOrMoveDirectoryContent(boolean copy) {
        // copy directory does not copy content
        // move directory moves content

        MemoryFileSystem fs = newMemoryFs();
        Path root = MemoryPath.createRoot(fs);
        Path source = root.resolve("source");
        Path fileInFolder = source.resolve("file");

        fs.createEntry(source, true, false);
        Entry fileEntry = fs.createEntry(fileInFolder, false, false);

        Path target = root.resolve("target");

        if (copy) {
            fs.copy(source, target);
        } else {
            fs.move(source, target);
        }

        Entry targetEntry = fs.findEntry(target);
        assertThat(targetEntry.isDirectory());
        if (copy) {
            assertThat(targetEntry.getEntries()).isNull();
        } else {
            assertThat(targetEntry.getEntries()).isSameAs(fileEntry);
        }
    }

    @Test(expectedExceptions = ConflictException.class)
    public void tryToCopyCreateConflict() {
        tryToCopyOrMoveCreateConflict(true);
    }

    @Test(expectedExceptions = ConflictException.class)
    public void tryToMoveCreateConflict() {
        tryToCopyOrMoveCreateConflict(false);
    }

    public void tryToCopyOrMoveCreateConflict(boolean copy) {
        MemoryFileSystem fs = newMemoryFs();
        Path root = MemoryPath.createRoot(fs);
        Path folder = root.resolve("folder");
        Path conflict = root.resolve("conflict");
        fs.createEntry(folder, true, false);
        fs.createEntry(conflict, false, false);

        if (copy) {
            fs.copy(folder, conflict);
        } else {
            fs.move(folder, conflict);
        }
    }

    @Test
    public void copyDirectoryConflictOverwriteDoesNotDelete() {
        copyOrMoveDirectoryConflictOverwriteDoesNotDelete(true);
    }

    @Test
    public void moveDirectoryConflictOverwriteDoesNotDelete() {
        copyOrMoveDirectoryConflictOverwriteDoesNotDelete(false);
    }

    private static void copyOrMoveDirectoryConflictOverwriteDoesNotDelete(boolean copy) {
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
        Path titanic = root.resolve("titanic");
        Path boatCaptain = titanic.resolve("captain");
        Entry captainEntry = fs.createEntry(boatCaptain, false, true);

        Path iceberg = root.resolve("iceberg");
        Entry icebergEntry = fs.createEntry(iceberg, true, false);

        if (copy) {
            fs.copy(iceberg, titanic, StandardCopyOption.REPLACE_EXISTING);
        } else {
            fs.move(iceberg, titanic, StandardCopyOption.REPLACE_EXISTING);
        }

        if (copy) {
            // with copy, original iceberg is still in place
            assertThat(fs.findEntry(iceberg)).isSameAs(icebergEntry);
        } else {
            // with move, it has been merged with the boat
            assertThat(fs.findEntry(iceberg)).isNull();
        }

        // whatever the scenario, captain is still on the boat
        assertThat(fs.findEntry(boatCaptain)).isSameAs(captainEntry);

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

        Files.createFile(original);
        Files.write(original, new byte[]{1, 2, 3});

        Path copy = root.resolve("copy");
        fs.copy(original, copy);

        Files.write(original, new byte[]{4, 5, 6});
        assertThat(Files.readAllBytes(original)).isEqualTo(new byte[]{4, 5, 6});

        assertThat(Files.readAllBytes(copy)).isEqualTo(new byte[]{1, 2, 3});

    }

    @Test(expectedExceptions = DoesNotExistsException.class)
    public void tryToCopyMissingFile() {
        tyToCopyOrMoveMissingFile(true);
    }

    @Test(expectedExceptions = DoesNotExistsException.class)
    public void tryToMoveMissingFile() {
        tyToCopyOrMoveMissingFile(false);
    }

    private static void tyToCopyOrMoveMissingFile(boolean copy) {
        // self-explainatory
        MemoryFileSystem fs = newMemoryFs();
        Path root = MemoryPath.createRoot(fs);
        Path missing = root.resolve("missing");
        Path target = root.resolve("target");
        if (copy) {
            fs.copy(missing, target);
        } else {
            fs.move(missing, target);
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

        read(fs, filePath, data);
    }

    @Test(expectedExceptions = InvalidRequestException.class)
    public void tryToReadFolder() {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath folder = MemoryPath.create(fs, "/folder");
        fs.createEntry(folder, true, true);

        fs.newByteChannel(folder, EnumSet.of(READ));
    }

    @Test(expectedExceptions = DoesNotExistsException.class)
    public void tryToReadMissingFile() {
        tryOpenChannelMissingFile(EnumSet.of(READ));
    }

    @Test(expectedExceptions = DoesNotExistsException.class)
    public void tryToWriteMissingFile() {
        tryOpenChannelMissingFile(EnumSet.of(WRITE));
    }

    public void tryOpenChannelMissingFile(Set<? extends OpenOption> options) {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath folder = MemoryPath.create(fs, "/missingFile");
        fs.newByteChannel(folder, options);
    }

    @Test
    public void noReadWriteOptionDefaultsToRead() throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath file = MemoryPath.create(fs, "/file");
        fs.createEntry(file, false, false);
        MemoryByteChannel channel = fs.newByteChannel(file, EnumSet.noneOf(StandardOpenOption.class));

        // if channel is not a read channel, this call will throw an exception
        channel.read(ByteBuffer.wrap(new byte[0]));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void tryReadAndWriteChannel() {
        // at most one of read or write options is required
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath path = MemoryPath.create(fs, "/file");
        fs.newByteChannel(path, EnumSet.of(READ, WRITE));
    }

    @Test(expectedExceptions = DoesNotExistsException.class)
    public void tryToWriteMissingFileWithoutRequestToCreate() {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath file = MemoryPath.create(fs, "/file");
        fs.newByteChannel(file, EnumSet.of(WRITE));
    }

    @Test
    public void writeMissingFileAndRequestCreate() {
        writeMissingCreateNew(EnumSet.of(WRITE, CREATE));
    }
    // TODO : write existing file and request create : should ignore that file exists

    @Test
    public void writeMissingFileAndRequestCreateNew() {
        writeMissingCreateNew(EnumSet.of(WRITE, CREATE_NEW));
    }

    @Test(expectedExceptions = ConflictException.class)
    public void tryWriteExistingFileAndRequestCreateNew() {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath file = MemoryPath.create(fs, "/file");
        fs.createEntry(file, false, true);
        assertThat(fs.findEntry(file)).isNotNull();

        fs.newByteChannel(file, EnumSet.of(WRITE, CREATE_NEW));
    }

    public MemoryByteChannel writeMissingCreateNew(Set<? extends OpenOption> options) {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath file = MemoryPath.create(fs, "/file");
        assertThat(fs.findEntry(file)).isNull();

        MemoryByteChannel channel = fs.newByteChannel(file, options);
        assertThat(channel).isNotNull();

        assertThat(fs.findEntry(file)).isNotNull();
        return channel;
    }

    @Test
    public void tryToTruncateWithReadChannel() throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath file = MemoryPath.create(fs, "/file");
        byte[] data = {1, 2, 3};
        fs.newByteChannel(file, EnumSet.of(WRITE, CREATE)).write(ByteBuffer.wrap(data));

        // truncate should just be ignored
        fs.newByteChannel(file, EnumSet.of(READ, TRUNCATE_EXISTING));

        read(fs, file, data);
    }

    @Test
    public void tryChannelWithUnsupportedOptions() {
        // we bypass most of checks by trying to write to an existing file
        // but must fail since we try to use an insupported option.
        for (StandardOpenOption unsuported : Arrays.asList(SPARSE, DELETE_ON_CLOSE, SYNC, DSYNC)) {
            boolean thrown = false;
            try {
                writeMissingCreateNew(EnumSet.of(WRITE, CREATE, unsuported));
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

        write(fs, file, new byte[]{1, 2, 3, 4}, EnumSet.of(WRITE, CREATE_NEW));
        read(fs, file, new byte[]{1, 2, 3, 4});

        write(fs, file, new byte[]{5, 6, 7, 8}, EnumSet.of(WRITE, APPEND));
        read(fs, file, new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
    }

    @Test
    public void writeTruncate() throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath file = MemoryPath.create(fs, "/file");

        write(fs, file, new byte[]{1, 2, 3, 4}, EnumSet.of(WRITE, CREATE_NEW));
        read(fs, file, new byte[]{1, 2, 3, 4});

        write(fs, file, new byte[]{5, 6, 7, 8}, EnumSet.of(WRITE, TRUNCATE_EXISTING));
        read(fs, file, new byte[]{5, 6, 7, 8});
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

    private static void write(MemoryFileSystem fs, Path path, byte[] toWrite, Set<? extends OpenOption> options) throws IOException {
        MemoryByteChannel firstChannel = fs.newByteChannel(path, options);
        write(firstChannel, toWrite);
    }

    private static void write(MemoryByteChannel channel, byte[] toWrite) throws IOException {
        assertThat(channel).isNotNull();
        assertThat(channel.isOpen()).isNotNull();
        channel.write(ByteBuffer.wrap(toWrite));
    }

    private static void read(MemoryFileSystem fs, Path path, byte[] expected) throws IOException {
        MemoryByteChannel channel = fs.newByteChannel(path, EnumSet.of(READ));
        assertThat(channel).isNotNull();
        assertThat(channel.isOpen()).isTrue();
        byte[] actual = new byte[expected.length];
        channel.read(ByteBuffer.wrap(actual));
        assertThat(expected).isEqualTo(expected);
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
