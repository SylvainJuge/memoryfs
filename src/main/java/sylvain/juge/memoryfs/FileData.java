package sylvain.juge.memoryfs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

final class FileData {

    private final InternalOutputStream stream;

    private FileData(InternalOutputStream stream) {
        this.stream = stream;
    }

    public void truncate(int newSize) {
        stream.truncate(newSize);
    }

    public InputStream asInputStream() {
        return new ByteArrayInputStream(stream.internalBuffer());
    }

    public OutputStream asOutputStream() {
        return stream;
    }

    public long size() {
        return stream.size();
    }

    public static FileData copy(FileData data){
        return null == data ? null : fromData(data.stream.internalBuffer());
    }

    public static FileData newEmpty() {
        return new FileData(new InternalOutputStream());
    }

    public static FileData fromData(byte[] data) {
        return new FileData(new InternalOutputStream(data));
    }

    // Hashcode and equals are rather "costly" since they naively read the whole buffer

    // Note : if hashcode is called frequently without data change, we could avoid re-computing value
    // as long as data is not rewritten.
    // for equals, we could short-circuit wich cached hash code when available

    @Override
    public int hashCode() {
        return Arrays.hashCode(stream.internalBuffer());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FileData)) {
            return false;
        }
        FileData other = (FileData) o;
        return Arrays.equals(stream.internalBuffer(), other.stream.internalBuffer());
    }

    private static class InternalOutputStream extends ByteArrayOutputStream {

        private InternalOutputStream() {
            super();
        }

        private InternalOutputStream(byte[] data) {
            super(data.length);
            count = data.length;
            System.arraycopy(data, 0, buf, 0, data.length);
        }

        private byte[] internalBuffer() {
            return buf;
        }

        private void truncate(int newSize) {
            if (newSize < 0) {
                throw new IllegalArgumentException("can't truncate to negative size");
            }
            if (newSize < count) {
                count = newSize;
            }
        }
    }
}
