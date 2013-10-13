package sylvain.juge.memoryfs;

import java.io.IOException;
import java.nio.file.Path;

/**
 * base class for memory fs exceptions, allows to wrap to  IOException to ensure compatibility with nio API specifications
 */
class MemoryFileSystemException extends RuntimeException {

    MemoryFileSystemException(String msg) {
        super(msg);
    }

    IOException toIOException() {
        return new IOException(getMessage(), this);
    }

}

/**
 * exception thrown when a path that should exists does not exists
 */
class DoesNotExistsException extends MemoryFileSystemException {
    DoesNotExistsException(Path path) {
        super("path does not exists : " + path);
    }
}

/**
 * exception thrown when there is a conflict, when trying to create two files/folders with same same in the same directory
 */
class ConflictException extends MemoryFileSystemException {
    ConflictException(String msg) {
        super(msg);
    }
}

class InvalidNameException extends MemoryFileSystemException {
    InvalidNameException(String name){
        super("not a valid name : "+name);
    }
}
