package sylvain.juge.memoryfs;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

public class EntryTest {

    // TODO : try to create name conflict
    // -> when creating entry
    // -> when renaming entry
    // -> when copying entry

    // entry copy should be a full copy
    // -> files & folders : with same content (but distinct from original copy)

    // TODO : try to create files/folders with illegal characters like / * . ..

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
    public void rename(){
        Entry root = Entry.newRoot();
        Entry folder = Entry.newDirectory(root,"a");
        Entry file = Entry.newFile(folder,"b");

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
    public void tryToCreateNameConflictThroughRename(){
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

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void moveNull(){
        Entry.newFile(Entry.newRoot(), "file").move(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void tryToMoveToFile(){
        Entry root = Entry.newRoot();
        Entry a = Entry.newFile(root, "a");
        Entry b = Entry.newFile(root, "b");
        a.move(b);
    }

    @Test(expectedExceptions = ConflictException.class)
    public void tryToCreateNameConflictThroughMove(){

        Entry root = Entry.newRoot();
        Entry a = Entry.newFile(root,"a");
        Entry folder = Entry.newDirectory(root, "folder");
        Entry.newFile(folder,"a");

        a.move(folder);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void tryToMoveFolderWithinItself(){
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

    // TODO : define an exception more appropriate than one about "argument"

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void deleteRootFails() {
        Entry.newRoot().delete();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void moveRootFails() {
        Entry.newRoot().move(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void renameRootFails() {
        Entry.newRoot().rename("");
    }

    @Test(enabled = false)
    public void copyFile(){
        Entry root = Entry.newRoot();
        Entry file = Entry.newFile(root, "file");
        Entry copy = file.copy(root, "copy");
        // copy a single file
        // file data should be identical, but not the same instance
        assertEntry(copy).isCopyOf(file);
    }

    @Test(enabled = false)
    public void copyFolder(){
        // copy a folder with files & sub-folders
        // all original files must be still in place
        // all copies should be available into target folder with same data

        // -> will require to add assertions to test file data
        // things to test :
        // - empty data
        // - identical data as another file
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

            // TODO : must have null data reference
            return this;
        }

        public EntryAssert isFile() {
            assertThat(entry.isDirectory()).isFalse();
            assertThat(entry.getEntries()).isNull();
            assertThat(entry.isRegularFile()).isTrue();
            assertThat(entry.isOther()).isFalse();
            assertThat(entry.isSymbolicLink()).isFalse();
            // TODO : must not have non-null data reference
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

        public EntryAssert isCopyOf(Entry expectedCopy){
            assertThat(entry).isNotSameAs(expectedCopy);
            if( entry.isDirectory()){
                // Note : folder copies must allow different ordering of elements and still be "equivalent"
                // only topology have to be the same, not all implementation details like next/previous.
                fail("not supported yet");
            }
            assertEntry(expectedCopy).hasCopyOfData(expectedCopy);
            return this;
        }

        public EntryAssert hasCopyOfData(Entry expectedSameData){
            // equal copy but not same instance
            assertThat(expectedSameData.getData())
                    .isNotSameAs(entry.getData())
                    .isEqualTo(entry.getData());
            // we miss a "has same hashcode" assertion here
            assertThat(expectedSameData.getData().hashCode()).isEqualTo(entry.getData().hashCode());
            return this;
        }


        public Entry value() {
            return entry;
        }

    }
}