package fx.node.builder;

import java.io.IOException;
import java.io.UncheckedIOException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.FileSystems;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import java.util.regex.Pattern;

import fx.mvc.View;
import fx.mvc.Controller;
import javafx.application.Platform;

public class ViewCompiler {
    public static void main(String...args) throws Exception {
        Platform.startup(() -> new ViewCompiler().start(args));
    }

    void start(String...args) {
        if (args.length > 0) {
            var path = Paths.get(args[0]);
            if (Files.isDirectory(path)) {
                findFiles(path,args,1);
                System.exit(0);
            }
        }
        System.err.println("usage: ViewCompiler [directory [file|glob]] ...");
        System.exit(1);
    }

    NodeBuilder builder = new NodeBuilder(forms());

    void findFiles(Path dir, String[] args, int i) {
        var gc = Pattern.compile("[*?]").matcher("");
        while (i < args.length) {
            var arg = args[i++];
            if (gc.reset(arg).find()) {
                findFiles(dir,arg);
            } else {
                var path = Paths.get(arg);
                if (Files.isDirectory(path)) {
                    dir = path;
                } else {
                    path = dir.resolve(path);
                    if (Files.isRegularFile(path)) {
                        process(dir,path);
                    } else {
                        System.err.format("argument %d '%s' ignored\n",(i-1),arg);
                    }
                }
            }
        }
    }

    void findFiles(Path dir, String pattern) {
        var glob = FileSystems.getDefault().getPathMatcher("glob:"+pattern);
        PathList.walk(dir)
            .filter(Files::isRegularFile)
            .filter(glob::matches)
            .forEach(file -> process(dir,file));
    }

    void process(Path dir, Path file) {
        file = dir.relativize(file);
        var text = generateCode(dir,file);
        if (text != null) {
            storeCode(dir,file,text);
        }
    }

    String generateCode(Path dir, Path file) {

        var viewName = fileName(file).replace('/','.');
        var controllerName = viewName + "Controller";
        var controllerTag = Controller.class.getName();
        var viewType = defaultValue(View.class,"nodeType");
        var includeType = defaultValue(View.class,"includeType");

        var path = dir.resolve(file);
        try (var in = Files.newInputStream(path)) {
            var text = builder
                .setView(viewName,viewType)
                .setController(controllerName,controllerTag)
                .setInclude(includeType)
                .transform(in);
            System.out.println(
                "generated "+viewName+" source file for "+controllerName);
            return text;
        } catch (IOException e) {
            System.out.format("I/O error reading %s : %s\n", path.toString(), e.toString() );
            return null;
        }
    }

    void storeCode(Path dir, Path file, String text) {
        var path = filePath(dir,file,".java");
        try ( var out = Files.newBufferedWriter(path)) {
            out.write(text);
        }
        catch (IOException e) {
            System.out.format("I/O error writing %s : %s\n", path.toString(), e.toString() );
            throw new UncheckedIOException(e);
        }
    }

    static Path filePath(Path dir, Path file, String suffix) {
        var name = fileName(file.getFileName());
        return dir.resolve(file).getParent().resolve(name+suffix);
    }

    static String fileName(Path path) {
        var name = path.toString();
        var p = name.lastIndexOf('.');
        return p < 0 ? name : name.substring(0,p);
    }

    static String defaultValue(Class<?> annotation, String name) {
        try {
            return String.valueOf(annotation.getDeclaredMethod(name).getDefaultValue());
        } catch (Exception e) {
            return null;
        }
    }

    static Forms forms() {
      return new Forms() {

        @Override
        Form makeForm(String className) {
            if (className != null && !className.isBlank()) {
                try {
                    var c = Class.forName(className);
                    return new Form(className,properties(c),interfaces(c),superclass(c));
                }
                catch (Exception e) {
                    System.err.println(className+" : "+e.toString());
                    e.printStackTrace();
                }
            }
            return null;
        }

        Map<String,String> properties(Class<?> c) {
            var map = new HashMap<String,String>();
            for (var m:c.getMethods()) {
                // TODO: check parameter count = 0
                var name = property(m.getName());
                if (name != null) {
                    map.put(name,m.getReturnType().getName());
                }
            }
            return map;
        }

        Set<String> interfaces(Class<?> c) {
            var set = new HashSet<String>();
            for (var i:c.getInterfaces()) {
                set.add(i.getName());
            }
            return set;
        }

        Form superclass(Class<?> c) {
            var sc = c.getSuperclass();
            return sc != null ? get(sc.getName()) : null;
        }

      };
    }

}
