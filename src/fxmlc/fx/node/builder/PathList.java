package fx.node.builder;

import java.io.IOException;
import java.io.InputStream;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;

import java.net.URI;
import java.net.URL;

import java.util.Collections;
import java.util.stream.Stream;

class PathList {

    static Stream<Path> get(URL url, String path) {
        try {
            return Files.list(fileSystem(url.toURI()).getPath(path));
        }
        catch (Exception e) {
            System.err.println(e);
        }
        return null;
    }

    static FileSystem fileSystem(URI uri) throws IOException {
        try {
            return FileSystems.getFileSystem(uri);
        } catch (FileSystemNotFoundException nf) {
            return FileSystems.newFileSystem(uri,Collections.emptyMap());
        }
    }

    static Stream<Path> walk(Path path) {
        try {
            return Files.walk(path,FileVisitOption.FOLLOW_LINKS);
        } catch (IOException e) {
            System.out.format("I/O error at %s : %s\n", path.toString(), e.toString() );
            return Stream.empty();
        }
    }

}
