package sylvain.juge.fsutils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class FileDigest {
    private final String algorithm;
    private final int bufferSize;

    public FileDigest(String algorithm, int bufferSize) {
        this.algorithm = algorithm;
        this.bufferSize = bufferSize;
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("buffer size must be > 0");
        }
        try {
            MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public String digest(Path file) throws IOException {

        SeekableByteChannel channel = Files.newByteChannel(file, StandardOpenOption.READ);
        ByteBuffer readBuffer = ByteBuffer.wrap(new byte[bufferSize]);

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
        int n;
        do {
            readBuffer.clear();
            n = channel.read(readBuffer);
            if (0 < n) {
                digest.update(readBuffer);
            }
        } while (0 < n);

        StringBuilder sb = new StringBuilder();
        for (byte b : digest.digest()) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
