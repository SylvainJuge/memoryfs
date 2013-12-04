package sylvain.juge.memoryfs;

import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

public class FileDataTest {

    @Test
    public void emptyHashCodeEquals() {
        FileData data1 = assertData(FileData.newEmpty())
                .isEmpty()
                .value();
        FileData data2 = assertData(FileData.newEmpty())
                .isDistinctCopyOf(data1)
                .isEmpty()
                .value();
        TestEquals.checkHashCodeEqualsConsistency(true, data1, data2);
    }

    @Test
    public void sameData() {
        byte[] data = new byte[]{1, 2, 3, 4};
        FileData data1 = assertData(FileData.fromData(data))
                .hasContent(data)
                .hasSize(4)
                .value();
        FileData data2 = assertData(FileData.fromData(data))
                .isDistinctCopyOf(data1)
                .value();
        TestEquals.checkHashCodeEqualsConsistency(true, data1, data2);
    }

    @Test
    public void dataCopyOnCreate() {
        // ensure that provided data is copied when created
        byte[] bytes = new byte[]{1, 2, 3, 4};
        byte[] bytesCopy = Arrays.copyOf(bytes, bytes.length);
        FileData data = assertData(FileData.fromData(bytes))
                .hasContent(bytes)
                .hasContent(bytesCopy)
                .value();
        Arrays.fill(bytes, (byte) 0);
        assertData(data).hasContent(bytesCopy);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void truncateNegativeSize() {
        FileData.fromData(new byte[1]).truncate(-1);
    }

    @Test
    public void truncateOutOfBoundsIgnored() {
        FileData data = FileData.fromData(new byte[1]);
        assertData(data).hasSize(1);
        data.truncate(2);
        assertData(data).hasSize(1);
    }

    @Test
    public void equalsWithAnotherType() {
        assertThat(FileData.newEmpty().equals(new Object())).isFalse();
    }

    @Test
    public void dataCopyOnCopy() {
        byte[] bytes = new byte[]{1, 2, 3, 4};
        FileData data = assertData(FileData.fromData(bytes))
                .hasContent(bytes)
                .hasSize(4)
                .value();
        FileData copy = FileData.copy(data);
        assertData(copy)
                .hasContent(bytes)
                .hasSize(data.size())
                .isDistinctCopyOf(data);
    }

    private static FileDataAssert assertData(FileData data) {
        return new FileDataAssert(data);
    }

    private static class FileDataAssert {
        private final FileData data;

        private FileDataAssert(FileData data) {
            this.data = data;
        }

        public FileDataAssert isEmpty() {
            assertThat(data.size()).isEqualTo(0);
            try {
                assertThat(data.asInputStream().read()).isLessThan(0);
            } catch (IOException e) {
                fail(e.getMessage());
            }
            return this;
        }

        public FileDataAssert hasContent(byte[] content) {
            ByteArrayInputStream expected = new ByteArrayInputStream(content);
            InputStream input = data.asInputStream();
            assertThat(input).hasContentEqualTo(expected);
            try {
                assertThat(input.read()).isLessThan(0);
            } catch (IOException e) {
                fail(e.getMessage());
            }
            return this;
        }

        public FileDataAssert isDistinctCopyOf(FileData other) {
            assertThat(data).isNotSameAs(other);
            assertThat(data.asInputStream()).hasContentEqualTo(other.asInputStream());
            return this;
        }

        public FileDataAssert hasSize(long size) {
            assertThat(data.size()).isEqualTo(size);
            return this;
        }

        public FileData value() {
            return data;
        }

    }
}
