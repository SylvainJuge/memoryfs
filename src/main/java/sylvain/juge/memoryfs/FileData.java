package sylvain.juge.memoryfs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

final class FileData extends ByteArrayOutputStream {

    private FileData(){
        super();
    }

    private FileData(byte[] data){
        super(data.length);
        count = data.length;
        System.arraycopy(data, 0, buf, 0, data.length);
    }

    public void truncate(int newSize){
        if( newSize < 0){
            throw new IllegalArgumentException("can't truncate to negative size");
        }
        if( newSize < count ){
            count = newSize;
        }
    }

    // allows to store up to 2^31 bytes (2Gb)
    public InputStream asInputStream(){
        return new ByteArrayInputStream(buf);
    }

    public static FileData newEmpty(){
        return new FileData();
    }

    public static FileData fromData(byte[] data){
        return new FileData(data);
    }
}
