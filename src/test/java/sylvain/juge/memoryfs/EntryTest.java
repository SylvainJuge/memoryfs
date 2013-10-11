package sylvain.juge.memoryfs;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

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
                .hasChild("a", a);

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
                .hasChild("b", b);
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
                .hasChild("a", a);
    }


    @Test
    public void createEmptyRoot() {
        assertEntry(Entry.newRoot())
                .isRoot()
                .hasNoEntry();
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
            // TODO : must have null data reference
            return this;
        }

        public EntryAssert isFile() {
            assertThat(entry.isDirectory()).isFalse();
            assertThat(entry.getEntries()).isNull();
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

        public EntryAssert hasChild(String name, Entry child) {
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

        public Entry value() {
            return entry;
        }

    }
}
