package sylvain.juge.memoryfs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

/**
 * base class for memory fs exceptions, allows to wrap to  IOException to ensure compatibility with nio API specifications
 */
class MemoryFileSystemException extends RuntimeException {

    MemoryFileSystemException(String msg) {
        super(msg);
    }

}

/**
 * exception thrown when a path that should exists does not exists
 */
class DoesNotExistsException extends FileNotFoundException {
    DoesNotExistsException(Path path) {
        super("path does not exists : " + path);
    }
}

/**
 * exception thrown when there is a conflict, when trying to create two files/folders with same same in the same directory
 * or when trying to create a file that should not exist.
 */
class ConflictException extends MemoryFileSystemException {
    ConflictException(String msg) {
        super(msg);
    }
}

class InvalidNameException extends MemoryFileSystemException {
    InvalidNameException(String name){
        super(String.format("not a valid name : \"%s\"", name));
    }
}

/** exception for use-case invalid requests like trying to read a folder */
class InvalidRequestException extends MemoryFileSystemException{
    InvalidRequestException(String msg){
        super(msg);
    }
}