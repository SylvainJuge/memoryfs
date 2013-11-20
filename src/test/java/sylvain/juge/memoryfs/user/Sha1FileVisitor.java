package sylvain.juge.memoryfs.user;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

// TODO : make this class generic for all algorithms
// TODO : delegate file hash computation to another separate class to reuse it
/**
 * Computes SHA-1 for all files found while walking file tree
 */
class Sha1FileVisitor extends SimpleFileVisitor<Path> {
    private final List<String> hashes;
    private final List<Path> paths;

    public Sha1FileVisitor(){
        hashes = new ArrayList<>();
        paths = new ArrayList<>();
    }

    public List<String> getHashes(){
        return hashes;
    }

    public List<Path> getPaths(){
        return paths;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

        SeekableByteChannel channel = Files.newByteChannel(file, StandardOpenOption.READ);
        ByteBuffer readBuffer = ByteBuffer.wrap(new byte[8048]);

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        int n;
        do {
            readBuffer.clear();
            n = channel.read(readBuffer);
            if( 0 < n ){
                digest.update(readBuffer);
            }
        } while( 0 < n );

        StringBuilder sb = new StringBuilder();
        for(byte b:digest.digest()){
            sb.append(String.format("%02x",b));
        }
        hashes.add(sb.toString());
        paths.add(file);

        return FileVisitResult.CONTINUE;
    }
}
