package sylvain.juge.memoryfs.util;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ProjectPathFinder {

    private ProjectPathFinder(){
        // uncallable constructor
    }

    /**
     * Searches folder with name {@code rootName} starting from current classpath root, then
     * upper until it reaches root or reaches first folder with this name.
     * @param rootName
     * @return null if not found, path to folder otherwise
     */
    public static Path getFolder(String rootName){
        URL classpathRoot = ProjectPathFinder.class.getClassLoader().getResource("");
        // replacing first slash is mandatory on windows
        Path folder = Paths.get(classpathRoot.getFile().replaceFirst("/", ""));
        while (null != folder && !rootName.equals(folder.getFileName().toString())) {
            folder = folder.getParent();
        }
        return null != folder ? folder : null;
    }
}
