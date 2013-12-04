package sylvain.juge.memoryfs;

import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

public class EntryTest {

    @Test
    public void rootPath() {
        assertThat(Entry.newRoot().getPath()).isEqualTo("/");
    }

    @Test
    public void unsupportedAttributes() {
        Entry entry = Entry.newRoot();
        assertThat(entry.lastAccessTime()).isNull();
        assertThat(entry.lastModifiedTime()).isNull();
        assertThat(entry.creationTime()).isNull();
    }

    @Test
    public void variousValidFileNames() {
        List<String> allowed = Arrays.asList(".a", "..a", "a..", "a.a", "a..b");
        for (String name : allowed) {
            Entry root = Entry.newRoot();
            Entry entry = Entry.newFile(root, name);
            assertEntry(entry).hasName(name);
        }
    }

    @Test
    public void tryToCreateFileWithInvalidName() {
        Entry root = Entry.newRoot();
        // note : we test only for invalid file names, some illegal charachers are already tested at path level
        for (String name : Arrays.asList("/", "/a", "a/", "a/a", ".", "..")) {
            boolean thrown = false;
            try {
                Entry.newFile(root, name);
                fail("should not be able to create file with name : " + name);
            } catch (InvalidNameException e) {
                thrown = true;
            }
            assertThat(thrown).isTrue();
        }
    }

    @Test
    public void createSingleFileInRoot() {
        Entry root = Entry.newRoot();
        Entry file = Entry.newFile(root, "file");

        assertEntry(file)
                .isFile()
                .hasParent(root)
                .hasName("file")
                .hasPath("/file");

        assertEntry(root).hasEntries(file);
    }

    @Test
    public void createTwoFoldersInRoot() {
        Entry root = Entry.newRoot();
        Entry folder1 = Entry.newDirectory(root, "a");
        Entry folder2 = Entry.newDirectory(root, "b");

        assertEntry(root)
                .isRoot()
                .hasEntries(folder1, folder2);

        assertEntry(folder1).hasNoEntry();
        assertEntry(folder2).hasNoEntry();
    }

    @Test
    public void createComplexFileTree() {
        // not really complex, but not elementary either
        // /a/b
        // /a/c/ (empty folder)
        // /d/e
        // /d/f

        Entry root = assertEntry(Entry.newRoot())
                .isRoot()
                .hasNoEntry()
                .value();

        Entry aFolder = assertEntry(Entry.newDirectory(root, "a"))
                .isDirectory()
                .hasParent(root)
                .hasName("a")
                .hasPath("/a")
                .hasNoEntry()
                .value();

        assertEntry(root).hasEntries(aFolder);

        Entry bFile = assertEntry(Entry.newFile(aFolder, "b"))
                .isFile()
                .hasParent(aFolder)
                .hasName("b")
                .hasPath("/a/b")
                .value();

        assertEntry(aFolder).hasEntries(bFile);

        Entry cFolder = assertEntry(Entry.newDirectory(root, "c"))
                .isDirectory()
                .hasParent(root)
                .hasName("c")
                .hasPath("/c")
                .hasNoEntry()
                .value();

        assertEntry(root).hasEntries(aFolder, cFolder);

        Entry dFolder = assertEntry(Entry.newDirectory(root, "d"))
                .isDirectory()
                .hasParent(root)
                .hasName("d")
                .hasPath("/d")
                .hasNoEntry()
                .value();

        Entry eFile = assertEntry(Entry.newFile(dFolder, "e"))
                .isFile()
                .hasParent(dFolder)
                .hasName("e")
                .hasPath("/d/e")
                .value();

        assertEntry(dFolder).hasEntries(eFile);

        Entry fFile = assertEntry(Entry.newFile(dFolder, "f"))
                .isFile()
                .hasParent(dFolder)
                .hasName("f")
                .hasPath("/d/f")
                .value();

        assertEntry(dFolder).hasEntries(eFile, fFile);

    }

    @Test
    public void removeSingle() {
        Entry root = Entry.newRoot();
        Entry a = Entry.newFile(root, "a");
        assertEntry(root)
                .hasEntries(a)
                .hasEntry("a", a);

        a.delete();

        assertEntry(root)
                .hasNoEntry()
                .doesNotHaveChild(a)
                .doesNotHaveChild("a");

    }

    @Test
    public void removeFirst() {
        Entry root = Entry.newRoot();
        Entry a = Entry.newFile(root, "a");
        Entry b = Entry.newFile(root, "b");
        assertEntry(root).hasEntries(a, b);

        a.delete();

        assertEntry(root)
                .hasEntries(b)
                .doesNotHaveChild(a)
                .doesNotHaveChild("a")
                .hasEntry("b", b);
    }

    @Test
    public void removeLast() {
        Entry root = Entry.newRoot();
        Entry a = Entry.newFile(root, "a");
        Entry b = Entry.newFile(root, "b");
        assertEntry(root).hasEntries(a, b);

        b.delete();

        assertEntry(root)
                .hasEntries(a)
                .doesNotHaveChild(b)
                .doesNotHaveChild("b")
                .hasEntry("a", a);
    }

    @Test
    public void createEmptyRoot() {
        assertEntry(Entry.newRoot())
                .isRoot()
                .hasNoEntry();
    }

    @Test(expectedExceptions = ConflictException.class)
    public void createNameConflict() {
        Entry root = Entry.newRoot();
        Entry.newFile(root, "a");
        Entry.newFile(root, "a");
    }

    @Test
    public void rename() {
        Entry root = Entry.newRoot();
        Entry folder = Entry.newDirectory(root, "a");
        Entry file = Entry.newFile(folder, "b");

        // rename file
        file.rename("c");

        assertEntry(folder)
                .doesNotHaveChild("b")
                .hasEntry("c", file);
        assertEntry(file)
                .hasPath("/a/c")
                .hasName("c");

        // rename folder
        folder.rename("d");

        assertEntry(root)
                .hasEntries(folder)
                .hasEntry("d", folder);
        assertEntry(folder)
                .hasName("d")
                .hasPath("/d");

        assertEntry(file).hasPath("/d/c");

    }

    @Test(expectedExceptions = InvalidNameException.class)
    public void tryToRenameNull() {
        Entry root = Entry.newRoot();
        Entry.newFile(root, "file").rename(null);
    }

    @Test(expectedExceptions = ConflictException.class)
    public void tryToCreateNameConflictThroughRename() {
        Entry root = Entry.newRoot();
        Entry.newFile(root, "a");
        Entry.newFile(root, "b").rename("a");
    }

    @Test
    public void move() {

        // Note : we test only for files, but folders should behave the same
        // the important part is moving 1st, middle and last entries since
        // implementation uses a linked-list
        Entry root = Entry.newRoot();
        Entry a = Entry.newFile(root, "a");
        Entry b = Entry.newFile(root, "b");
        Entry c = Entry.newFile(root, "c");
        Entry folder = Entry.newDirectory(root, "folder");

        assertEntry(root).hasEntries(a, b, c, folder);
        assertEntry(folder).hasNoEntry();

        // move middle element
        b.move(folder);
        assertEntry(b).hasParent(folder);

        assertEntry(root).hasEntries(a, c, folder);
        assertEntry(folder).hasEntries(b);

        // move it back to root, note that order is different
        b.move(root);
        assertEntry(b).hasParent(root);

        assertEntry(root).hasEntries(a, c, folder, b);
        assertEntry(folder).hasNoEntry();

        // move 1st element
        a.move(folder);
        assertEntry(root).hasEntries(c, folder, b);
        assertEntry(folder).hasEntries(a);

        // move last element
        b.move(folder);
        assertEntry(root).hasEntries(c, folder);
        assertEntry(folder).hasEntries(a, b);

        // move the last remaining file
        c.move(folder);
        assertEntry(root).hasEntries(folder);
        assertEntry(folder).hasEntries(a, b, c);

    }

    @Test(expectedExceptions = NullPointerException.class)
    public void moveNull() {
        Entry.newFile(Entry.newRoot(), "file").move(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void tryToMoveToFile() {
        Entry root = Entry.newRoot();
        Entry a = Entry.newFile(root, "a");
        Entry b = Entry.newFile(root, "b");
        a.move(b);
    }

    @Test(expectedExceptions = InvalidRequestException.class)
    public void tryToMoveRoot() {
        Entry.newRoot().move(Entry.newRoot());
    }

    @Test(expectedExceptions = InvalidRequestException.class)
    public void tryToRenameRoot() {
        Entry.newRoot().rename("anything");
    }

    @Test(expectedExceptions = InvalidNameException.class)
    public void tryToRenameToNull() {
        Entry.newFile(Entry.newRoot(), "file").rename(null);
    }

    @Test(expectedExceptions = InvalidNameException.class)
    public void tryToRenameToEmpty() {
        Entry.newFile(Entry.newRoot(), "file").rename("");
    }

    @Test(expectedExceptions = ConflictException.class)
    public void tryToCreateNameConflictThroughMove() {

        Entry root = Entry.newRoot();
        Entry a = Entry.newFile(root, "a");
        Entry folder = Entry.newDirectory(root, "folder");
        Entry.newFile(folder, "a");

        a.move(folder);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void tryToMoveFolderWithinItself() {
        Entry root = Entry.newRoot();
        Entry folder1 = Entry.newDirectory(root, "folder1");
        Entry folder2 = Entry.newDirectory(folder1, "folder2");

        folder1.move(folder2);
    }

    @Test
    public void moveToSameLocationIsNoOp() {
        Entry root = Entry.newRoot();
        Entry a = Entry.newFile(root, "a");
        Entry b = Entry.newFile(root, "b");
        Entry c = Entry.newFile(root, "c");

        assertEntry(root).hasEntries(a, b, c);

        b.move(root);

        // if operation is a no-op, order of elements don't change
        assertEntry(root).hasEntries(a, b, c);
    }

    @Test(expectedExceptions = InvalidRequestException.class)
    public void deleteRootFails() {
        Entry.newRoot().delete();
    }

    @Test(expectedExceptions = InvalidRequestException.class)
    public void moveRootFails() {
        Entry.newRoot().move(Entry.newRoot());
    }

    @Test(expectedExceptions = InvalidRequestException.class)
    public void renameRootFails() {
        Entry.newRoot().rename("");
    }

    @Test
    public void fileIsCopyOfIself() {
        // test self test
        Entry file = Entry.newFile(Entry.newRoot(), "file");
        assertEntry(file).isCopyOf(file);
    }

    @Test
    public void copyFile() {
        // copy a single file
        // file data should be identical, but not the same instance
        Entry root = Entry.newRoot();
        Entry file = Entry.newFile(root, "file");
        Entry copy = file.copy(root, "copy");
        assertEntry(copy).isDistinctCopyOf(file);
    }

    @Test(expectedExceptions = ConflictException.class)
    public void tryToCreateConflictThroughCopy() {
        Entry root = Entry.newRoot();
        Entry file = Entry.newFile(root, "file");
        file.copy(root, "file");
    }

    @Test
    public void createFileWithData() {
        Entry root = Entry.newRoot();
        String name = "fileName";
        Entry file = newFileWithNameAsData(root, name);

        assertEntry(file).hasData(name.getBytes());
    }

    @Test(enabled = false)
    public void copyFolder() {
        Entry root = Entry.newRoot();
        Entry folderToCopy = Entry.newDirectory(root, "toCopy");
        Entry folder = Entry.newDirectory(folderToCopy, "folder");
        Entry fileInFolder = newFileWithNameAsData(folder, "fileInFolder");
        Entry file = newFileWithNameAsData(folderToCopy, "file");

        Entry copy = folderToCopy.copy(root, "copy");
        assertEntry(copy)
                .hasPath("/copy");


        // copy a folder with files & sub-folders
        // all original files must be still in place
        // all copies should be available into target folder with same data

        // -> will require to add assertions to test file data
        // things to test :
        // - empty data
        // - identical data as another file
    }

    private static Entry newFileWithNameAsData(Entry parent, String name) {
        Entry result = Entry.newFile(parent, name);
        try {
            result.getData().asOutputStream().write(name.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertThat(result.getData().size()).isEqualTo(name.length());
        return result;
    }

    private static EntryAssert assertEntry(Entry entry) {
        return new EntryAssert(entry);
    }

    private static class EntryAssert {
        private final Entry entry;

        private EntryAssert(Entry entry) {
            this.entry = entry;
            assertThat(entry).isNotNull();
            assertThat(entry.isOther()).isFalse();
            assertThat(entry.fileKey()).isSameAs(entry);
        }

        public EntryAssert isRoot() {
            isDirectory();
            assertThat(entry.getParent()).isNull();
            assertThat(entry.getNext()).isNull();
            return this;
        }

        public EntryAssert isDirectory() {
            assertThat(entry.isDirectory()).describedAs(entry + " expacted to be a directory").isTrue();
            assertThat(entry.isRegularFile()).isFalse();
            assertThat(entry.isOther()).isFalse();
            assertThat(entry.isSymbolicLink()).isFalse();
            assertThat(entry.size()).isEqualTo(0);
            assertThat(entry.getData()).isNull();
            return this;
        }

        public EntryAssert isFile() {
            assertThat(entry.isDirectory()).isFalse();
            assertThat(entry.getEntries()).isNull();
            assertThat(entry.isRegularFile()).isTrue();
            assertThat(entry.isOther()).isFalse();
            assertThat(entry.isSymbolicLink()).isFalse();
            assertThat(entry.getData()).isNotNull();
            assertThat(entry.getData().size()).isEqualTo(entry.size());
            return this;
        }

        public EntryAssert hasParent(Entry parent) {
            assertThat(entry.getParent()).isSameAs(parent);
            return this;
        }

        public EntryAssert hasName(String name) {
            assertThat(entry.getParent().getChild(name)).isSameAs(entry);
            return this;
        }

        public EntryAssert hasEntries(Entry... entries) {
            isDirectory();
            List<Entry> actual = new ArrayList<>();
            for (Entry e = entry.getEntries(); e != null; e = e.getNext()) {
                actual.add(e);
                assertThat(e.getParent()).isSameAs(entry);
            }
            assertThat(actual).containsExactly(entries);
            return this;
        }

        public EntryAssert doesNotHaveChild(Entry child) {
            isDirectory();
            Entry e = entry.getEntries();
            while (null != e && e != child) {
                e = e.getNext();
            }
            assertThat(e).isNull();
            return this;
        }

        public EntryAssert doesNotHaveChild(String name) {
            isDirectory();
            assertThat(entry.getChild(name)).isNull();
            return this;
        }

        public EntryAssert hasEntry(String name, Entry child) {
            isDirectory();
            assertThat(entry.getChild(name)).isSameAs(child);
            Entry e = entry.getEntries();
            while (e != null && e != child) {
                e = e.getNext();
            }
            assertThat(e).isSameAs(child);
            return this;
        }

        public EntryAssert hasNoEntry() {
            isDirectory();
            assertThat(entry.getEntries()).isNull();
            return this;
        }

        public EntryAssert hasPath(String path) {
            assertThat(entry.getPath()).isEqualTo(path);
            return this;
        }

        public EntryAssert isCopyOf(Entry expectedCopy) {
            if (entry.isDirectory()) {
                // Note : folder copies must allow different ordering of elements and still be "equivalent"
                // only topology have to be the same, not all implementation details like next/previous.
                fail("not supported yet");
            }
            assertEntry(expectedCopy).hasSameData(expectedCopy);
            return this;
        }

        public EntryAssert isDistinctCopyOf(Entry expectedCopy) {
            isCopyOf(expectedCopy);
            assertThat(entry).isNotSameAs(expectedCopy);
            assertThat(entry.getData()).isNotSameAs(expectedCopy.getData());
            return this;
        }

        public EntryAssert hasData(byte[] expected) {
            isFile();
            byte[] actualBytes = new byte[expected.length];
            try (InputStream input = entry.getData().asInputStream()) {
                for (int i = 0; i < actualBytes.length; i++) {
                    actualBytes[i] = (byte) input.read();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            assertThat(actualBytes).isEqualTo(expected);
            return this;
        }

        public EntryAssert hasSameData(Entry expectedSameData) {
            isFile();
            assertThat(entry.getData().asInputStream())
                    .hasContentEqualTo(expectedSameData.getData().asInputStream());
            assertThat(expectedSameData.getData().hashCode()).isEqualTo(entry.getData().hashCode());
            return this;
        }

        public Entry value() {
            return entry;
        }

    }
}
