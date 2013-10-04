package sylvain.juge.memoryfs;

import java.io.IOException;

class MemoryFileSystemException extends RuntimeException {

    private final IOException e;
    public MemoryFileSystemException(IOException e){
        this.e = e;
    }

    public IOException unwrap(){
        return e;
    }
}
