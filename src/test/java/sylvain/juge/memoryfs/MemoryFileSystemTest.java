package sylvain.juge.memoryfs;

import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.NonWritableChannelException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static java.nio.file.StandardOpenOption.*;
import static org.fest.assertions.api.Assertions.assertThat;

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
        Entry entry = fs.findEntry(root);
        assertThat(entry).isNotNull();
        assertThat(entry.getParent()).isNull();
        assertThat(entry.isDirectory()).isTrue();

        // multiple equivalent paths leads to the same entry instance
        assertThat(fs.findEntry(root)).isSameAs(entry);
    }

    @Test
    public void findNonExistingEntry() {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath nonExistingPath = MemoryPath.create(fs, "non/existing/path");

        assertThat(fs.findEntry(nonExistingPath)).isNull();
    }

    @Test
    public void createDirectoryEntry() {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath root = MemoryPath.createRoot(fs);
        Path directory = root.resolve("directory");

        assertThat(fs.findEntry(directory)).describedAs("non existing directory shouldn't have any entry yet").isNull();

        Entry dirEntry = fs.createEntry(directory, true, false);
        assertThat(dirEntry).isNotNull();
        assertThat(dirEntry.isDirectory()).isTrue();

        assertThat(dirEntry.getParent()).isSameAs(fs.findEntry(root));

        assertThat(fs.findEntry(directory)).isSameAs(dirEntry);
    }


    @Test(expectedExceptions = DoesNotExistsException.class)
    public void failsToCreateFolderWithMissingParents() {
        failsToCreateWithMissingParent(true);
    }

    @Test(expectedExceptions = DoesNotExistsException.class)
    public void failsToCreateFileWithMissingParents(){
        failsToCreateWithMissingParent(false);
    }

    private static void failsToCreateWithMissingParent(boolean directory){
        MemoryFileSystem fs = newMemoryFs();

        MemoryPath path = MemoryPath.create(fs, "/anywhere/beyond/root");
        assertThat(fs.findEntry(path)).isNull();
        assertThat(fs.findEntry(path.getParent())).isNull();

        fs.createEntry(path, directory, false);
    }

    @Test(expectedExceptions = ConflictException.class)
    public void failsToCreateFileWhenAlreadyExists() {
        failsToCreateWhenAlreadyExists(false);
    }

    @Test(expectedExceptions = ConflictException.class)
    public void failsToCreateFolderWhenAlreadyExists() {
        failsToCreateWhenAlreadyExists(false);
    }

    private static void failsToCreateWhenAlreadyExists(boolean directory){
        MemoryFileSystem fs = newMemoryFs();

        MemoryPath path = MemoryPath.create(fs, "/existing");
        fs.createEntry(path, directory, false);
        Entry entry = fs.findEntry(path);
        assertThat(entry).isNotNull();

        fs.createEntry(path, directory, false);
    }

    @Test
    public void createDirectoryEntryWithParents() {
        createWithParents(true);
    }

    @Test
    public void createFileEntryWithParents() {
        createWithParents(false);
    }

    private static void createWithParents(boolean directory){
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath path = MemoryPath.create(fs, "/not/in/root");
        for (Path p : path) {
            assertThat(fs.findEntry(p)).isNull();
        }
        Entry entry = fs.createEntry(path, directory, true);
        assertThat(entry).isNotNull();
        assertThat(entry.isDirectory()).isEqualTo(directory);

        for(Path parent: path.getParent()){
            Entry e = fs.findEntry(parent);
            assertThat(e).describedAs("missing parent folder " + parent).isNotNull();
            assertThat(e.isDirectory()).isTrue();
        }
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
    public void deleteThenReCreateFile(){
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath path = MemoryPath.create(fs,"/a/b");
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

    @Test
    public void deleteFolderDeletesItsContent(){
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath folder = MemoryPath.create(fs, "/a");
        MemoryPath leaf = MemoryPath.create(fs, "/a/b");

        fs.createEntry(leaf, false, true);
        assertThat(fs.findEntry(leaf)).isNotNull();
        folder.findEntry().delete();
        assertThat(fs.findEntry(folder)).isNull();
        assertThat(fs.findEntry(leaf)).isNull();

    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldNotAllowToDeleteRoot(){
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath.createRoot(fs).findEntry().delete();
    }

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
    public void tryToReadFolder(){
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath folder = MemoryPath.create(fs, "/folder");
        fs.createEntry(folder, true, true);

        fs.newByteChannel(folder, Collections.singleton(READ));
    }

    @Test(expectedExceptions = DoesNotExistsException.class)
    public void tryToReadMissingFile(){
        tryOpenChannelMissingFile(READ);
    }

    @Test(expectedExceptions = DoesNotExistsException.class)
    public void tryToWriteMissingFile(){
        tryOpenChannelMissingFile(WRITE);
    }

    public void tryOpenChannelMissingFile(StandardOpenOption... options){
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath folder = MemoryPath.create(fs, "/missingFile");
        fs.newByteChannel(folder, new HashSet<>(Arrays.asList(options)));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void tryNotReadNorWriteChannel(){
        // at least one of read|write options is required
        tryChannelWithOptions();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void tryReadAndWriteChannel() {
        // at least one of read|write options is required
        tryChannelWithOptions(READ, WRITE);
    }

    @Test(expectedExceptions = DoesNotExistsException.class)
    public void tryToWriteMissingFileWithoutRequestToCreate(){
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath file = MemoryPath.create(fs, "/file");
        fs.newByteChannel(file, openOptions(WRITE));
    }

    @Test
    public void writeMissingFileAndRequestCreate() {
        writeMissingCreateNew(WRITE, CREATE);
    }
    // TODO : write existing file and request create : should ignore that file exists

    @Test
    public void writeMissingFileAndRequestCreateNew() {
        writeMissingCreateNew(WRITE, CREATE_NEW);
    }

    @Test(expectedExceptions = ConflictException.class)
    public void tryWriteExistingFileAndRequestCreateNew() {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath file = MemoryPath.create(fs, "/file");
        fs.createEntry(file, false, true);
        assertThat(fs.findEntry(file)).isNotNull();

        fs.newByteChannel(file, openOptions(WRITE, CREATE_NEW));
    }

    public MemoryByteChannel writeMissingCreateNew(StandardOpenOption... options){
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath file = MemoryPath.create(fs, "/file");
        assertThat(fs.findEntry(file)).isNull();

        MemoryByteChannel channel = fs.newByteChannel(file, openOptions(options));
        assertThat(channel).isNotNull();

        assertThat(fs.findEntry(file)).isNotNull();
        return channel;
    }

    private static Set<StandardOpenOption> openOptions(StandardOpenOption... options){
        return new HashSet<>(Arrays.asList(options));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void tryToCreateWithReadChannel() throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath file = MemoryPath.create(fs, "/file");

        fs.newByteChannel(file, openOptions(WRITE, CREATE));

        fs.newByteChannel(file, openOptions(READ, CREATE));
    }

    @Test
    public void tryToTruncateWithReadChannel() throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath file = MemoryPath.create(fs, "/file");
        byte[] data = {1, 2, 3};
        fs.newByteChannel(file, openOptions(WRITE, CREATE)).write(ByteBuffer.wrap(data));

        // truncate should just be ignored
        fs.newByteChannel(file,openOptions(READ, TRUNCATE_EXISTING));

        read(fs, file, data);
    }

    @Test
    public void tryChannelWithUnsupportedOptions(){
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

        write( fs, file, new byte[]{1, 2, 3, 4}, WRITE, CREATE_NEW);
        read(fs, file, new byte[]{1, 2, 3, 4});

        write(fs, file, new byte[]{5, 6, 7, 8}, WRITE, APPEND);
        read(fs, file, new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
    }

    @Test
    public void writeTruncate() throws IOException {
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath file = MemoryPath.create(fs, "/file");

        write( fs, file, new byte[]{1, 2, 3, 4}, WRITE, CREATE_NEW);
        read(fs, file, new byte[]{1, 2, 3, 4});

        write( fs, file, new byte[]{5, 6, 7, 8}, WRITE, TRUNCATE_EXISTING);
        read(fs, file, new byte[]{5, 6, 7, 8});
    }

    private static void write(MemoryFileSystem fs, Path path, byte[] toWrite, StandardOpenOption... options) throws IOException {
        MemoryByteChannel firstChannel = fs.newByteChannel(path, openOptions(options));
        write(firstChannel, toWrite);
    }

    private static void write(MemoryByteChannel channel, byte[] toWrite) throws IOException {
        assertThat(channel).isNotNull();
        assertThat(channel.isOpen()).isNotNull();
        channel.write(ByteBuffer.wrap(toWrite));
    }

    private static void read(MemoryFileSystem fs, Path path, byte[] expected) throws IOException {
        MemoryByteChannel channel = fs.newByteChannel(path, openOptions(READ));
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

    @Test(enabled = false)
    public void getPathMatcher() {
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
        throw new RuntimeException("TODO : implement getPathMatcher");
    }

    private static MemoryFileSystem newMemoryFs() {
        return MemoryFileSystem.builder(newProvider()).build();
    }

    private static MemoryFileSystemProvider newProvider() {
        return new MemoryFileSystemProvider();
    }

    private static void tryChannelWithOptions(StandardOpenOption... options){
        Set<StandardOpenOption> set = new HashSet<>(Arrays.asList(options));
        MemoryFileSystem fs = newMemoryFs();
        MemoryPath path = MemoryPath.create(fs, "/file");
        fs.newByteChannel(path, set);
    }
}
