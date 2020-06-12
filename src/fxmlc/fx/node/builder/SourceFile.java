package fx.node.builder;

import java.io.IOException;
import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;

import javax.annotation.processing.ProcessingEnvironment;

// Netbeans notes for SOURCE_PATH:
//  1. check 'Run Compilation in External VM'
//  2. add '-sourcepath src' to 'Additional Compiler Options'

// Eclipse notes for SOURCE_PATH:
//  1. enable annotation processing
//  2. add '-Asourcepath=src'

class SourceFile {

    static FileObject get(ProcessingEnvironment env, String path, String file) throws Exception {
        try {
            return env.getFiler().getResource(StandardLocation.SOURCE_PATH,path,file);
        }
        catch(Exception ex) {
            var dirs = sourcePath(env);
            if (dirs != null) {
                var source = sourceFile(dirs,path.replace('.','/'),file);
                if (source != null) {
                    return fileObject(source);
                }
            }
            throw ex;
        }
    }

    static String[] sourcePath(ProcessingEnvironment env) {
        var src = env.getOptions().get("sourcepath");
        return src != null ? src.split(",") : null;
    }

    static Path sourceFile(String[] dirs, String path, String file) {
        for (var dir:dirs) {
            var source = Paths.get(dir.trim(),path,file);
            if (Files.isReadable(source)) {
                return source;
            }
        }
        return null;
    }

    static FileObject fileObject(Path source) {
        return new SimpleJavaFileObject(source.toUri(),JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                return Files.readString(source);
            }
            @Override
            public InputStream openInputStream() throws IOException {
                return Files.newInputStream(source);
            }
        };
    }

}