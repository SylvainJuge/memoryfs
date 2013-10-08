package sylvain.juge.memoryfs;

import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

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

    // TODO : test that creating a readonly filesystem only contains readonly filestores
    // TODO : create a filesystem with mixex readonly/readwrite stores
    // TODO : allow to create runtime-configurable stores ?
    // -> otherwise creating empty-read-only filestores seems quite useless

    // TODO : multiple filestores in the same fs have different IDs
    // when there is a single store, there is no need to explicitely provide its name

    @Test
    public void buildWithDefaultValues() throws IOException {
        MemoryFileSystem fs = MemoryFileSystem
                .builder(newProvider())
                .build();

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
        MemoryFileSystem fs = MemoryFileSystem.builder(newProvider()).build();
        assertThat(fs.getPath("/absoluteSingle")).isEqualTo(MemoryPath.create(fs, "/absoluteSingle"));
        assertThat(fs.getPath("relative")).isEqualTo(MemoryPath.create(fs, "relative"));

        assertThat(fs.getPath("/a", "path")).isEqualTo(MemoryPath.create(fs, "/a/path"));
        assertThat(fs.getPath("a", "relative")).isEqualTo(MemoryPath.create(fs, "a/relative"));

        assertThat(fs.getPath("a/b", "c/d", "e")).isEqualTo(MemoryPath.create(fs, "a/b/c/d/e"));

    }

    @Test
    public void rootPathIsTheOnlyRoot() throws IOException {
        // with or without id, the fs root remains the same
        checkRootDirectories(MemoryFileSystem.builder(newProvider()).build(), "/");
        checkRootDirectories(MemoryFileSystem.builder(newProvider()).id("id").build(), "/");
    }

    @Test
    public void rootEntryAlwaysAvailable() {
        MemoryFileSystem fs = MemoryFileSystem.builder(newProvider()).build();
        MemoryPath root = MemoryPath.createRoot(fs);
        Entry entry = fs.findEntry(root);
        assertThat(entry).isNotNull();
        assertThat(entry.getParent()).isNull();
        assertThat(entry.isDirectory()).isTrue();
        assertThat(entry.getFiles()).isEmpty();

        // multiple equivalent paths leads to the same entry instance
        assertThat(fs.findEntry(root)).isSameAs(entry);
    }

    @Test
    public void findNonExistingEntry() {
        MemoryFileSystem fs = MemoryFileSystem.builder(newProvider()).build();
        MemoryPath nonExistingPath = MemoryPath.create(fs, "non/existing/path");

        assertThat(fs.findEntry(nonExistingPath)).isNull();
    }

    @Test
    public void createDirectoryEntry() {
        MemoryFileSystem fs = MemoryFileSystem.builder(newProvider()).build();
        MemoryPath root = MemoryPath.createRoot(fs);
        Path directory = root.resolve("directory");

        assertThat(fs.findEntry(directory)).describedAs("non existing directory shouldn't have any entry yet").isNull();

        Entry dirEntry = fs.createEntry(directory, true, false);
        assertThat(dirEntry).isNotNull();
        assertThat(dirEntry.isDirectory()).isTrue();

        assertThat(fs.findEntry(root).getFiles()).isNotEmpty();

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
        MemoryFileSystem fs = MemoryFileSystem.builder(newProvider()).build();

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
        MemoryFileSystem fs = MemoryFileSystem.builder(newProvider()).build();

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
        MemoryFileSystem fs = MemoryFileSystem.builder(newProvider()).build();
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
        MemoryFileSystem fs = MemoryFileSystem.builder(newProvider()).build();
        MemoryPath conflictingFile = MemoryPath.create(fs, "/a/b");
        MemoryPath fileToCreate = MemoryPath.create(fs, "/a/b/c");

        fs.createEntry(conflictingFile, false, true);
        // will fail because parent folder already created as file (and not directory)
        fs.createEntry(fileToCreate, false, true);
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
        throw new RuntimeException("TODO : implement getPathMatcher");
    }

    private static MemoryFileSystemProvider newProvider() {
        return new MemoryFileSystemProvider();
    }

}
