package fx.node.builder;

import java.io.IOException;
import java.io.UncheckedIOException;

import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

// ViewCompiler -s <directory> -> specify where to place generated source files
//              -c             -> generate class prototypes
//              -r <directory> ->
//              <file|glob> ...

public class ViewCompiler {
  public static void main(String...args) throws Exception {
    if (args.length > 0) {
      new ViewCompiler().start(args);
    } else {
      System.out.println("usage: ViewCompiler -s <directory> [-c] <file|glob> ...");
    }
  }

  void start(String...args) throws Exception {

    var files = new ArrayList<String>();
    var proto = false;
    Path dir = null, root = null;

    for (var i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "-s" -> dir = Paths.get(args[++i]);
        case "-r" -> root = Paths.get(args[++i]);
        case "-c" -> proto = true;
        default -> files.add(args[i]);
      }
    }

    Beans.provider = () -> new Reflector();
    generate(dir,files,proto,root);
  }

  void generate(Path dir, List<String> files, boolean proto, Path root) {
    for (var file : find(files)) {

      var part = fileName(file);
      var gen = new NodeBuilder(part.name);
      if (root != null) {
        var pkg = root.relativize(file).getParent();
        gen.setPackage(pkg.toString().replace('/','.'));
      }
      switch (part.suffix) {
        case "fxml" -> new FxmlDriver().transform(readChars(file),gen);
        case "jbml" -> new JbmlDriver().transform(readChars(file),gen);
        default -> {
          System.out.printf("skipping %s\n", file.toString());
          continue;
        }
      }
      var text = gen.view();
      var dest = target(dir,file,gen.packageName,part.name);
      writeChars(dest,text);
      System.out.printf("generated %s from %s\n", dest.toString(), file.toString());

      if (proto) {
        // TODO: generate prorotype.java and store
        prototypePart();
      }
    }
  }

  Path target(Path dir, Path file, String pkg, String name) {
    if (dir == null) {
      dir = file.getParent();
    } else {
      name = (pkg+'.'+name).replace('.','/');
    }
    return mkDirs(dir.resolve(name+".java"));
  }

  void prototypePart() {} // TODO:

  static Pattern glob = Pattern.compile("[\\*\\?\\[\\{]");

  Set<Path> find(List<String> list) {
    var files = new HashSet<Path>();
    for (var path:list) {
      var m = glob.matcher(path);
      if (m.find()) {
        findAll(files,path,m.start());
      } else {
        findOne(files,path);
      }
    }
    return files;
  }

  void findOne(Set<Path> set, String path) {
    var file = Paths.get(path);
    if (Files.isRegularFile(file)) {
      set.add(file);
    } else {
      System.out.println("file not found: "+path);
    }
  }

  void findAll(Set<Path> set, String path, int index) {
    var n = set.size();
    var dir = path.substring(0, path.lastIndexOf('/', index));
    var root = Paths.get(dir);
    var matcher = FileSystems.getDefault().getPathMatcher("glob:" + path);

    try {
      Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
          if (attrs.isRegularFile() && matcher.matches(file)) {
            set.add(file);
          }
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) { throw new UncheckedIOException(e); }

    if (set.size() == n) {
      System.out.println("no files found: "+path);
    }
  }

  static char[] readChars(Path file) {
    try { return Files.readString(file).toCharArray(); }
    catch (IOException e) { throw new UncheckedIOException(e); }
  }

  static void writeChars(Path file, CharSequence chars) {
    try { Files.writeString(file,chars); }
    catch (IOException e) { throw new UncheckedIOException(e); }
  }

  static Path mkDirs(Path file) {
    try { Files.createDirectories(file.getParent()); return file; }
    catch (IOException e) { throw new UncheckedIOException(e); }
  }

  record Part( String name, String suffix ) {}

  static Part fileName(Path path) {
    var f = path.getFileName().toString();
    var i = f.lastIndexOf('.');
    return i < 0 ? new Part( f, "" )
                 : new Part( f.substring(0,i), f.substring(i+1) );
  }

}
