package sylvain.juge.fsutils;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ProjectPathFinder {

    private ProjectPathFinder() {
        // uncallable constructor
    }

    /**
     * Searches folder with name {@code rootName} starting from current classpath root, then
     * upper until it reaches root or reaches first folder with this name.
     *
     * @param rootName project folder name
     * @return null if not found, path to folder otherwise
     */
    public static Path getFolder(String rootName) {
        URL classpathRoot = ProjectPathFinder.class.getClassLoader().getResource("");
        if (null == classpathRoot) {
            throw new IllegalStateException("unable to retrieve classpath root");
        }
        Path folder;
        try {
            folder = Paths.get(classpathRoot.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        while (null != folder && !rootName.equals(folder.getFileName().toString())) {
            folder = folder.getParent();
        }
        return null != folder ? folder : null;
    }
}
