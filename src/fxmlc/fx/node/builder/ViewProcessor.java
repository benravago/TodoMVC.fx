package fx.node.builder;

import java.util.Set;

import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.tools.StandardLocation;
import static javax.tools.Diagnostic.Kind.*;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

import fx.mvc.View;
import fx.mvc.Controller;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;

public class ViewProcessor extends AbstractProcessor {

    List<Path> sourcePath;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        sourcePath = getSourcePath(processingEnv.getOptions().get("sourcepath"));
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(View.class.getName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var set = roundEnv.getElementsAnnotatedWith(View.class);
        if (!set.isEmpty()) {
            process(set);
        }
        return true;
    }

    void process(Set<? extends Element> set) {
        var eu = processingEnv.getElementUtils();
        var builder = new NodeBuilder(new Forms(eu));
        for (var e:set) {
            var k = e.getKind();
            if (k == ElementKind.CLASS || k == ElementKind.INTERFACE) {
                process(builder,(TypeElement)e);
            }
        }
    }

    void process(NodeBuilder builder, TypeElement e) {
        var controller = (TypeElement)e;
        var view = controller.getAnnotation(View.class);
        var code = generateCode( builder, controller, view );
        if (code != null) {
            var viewName = view.value();
            storeCode( code, viewName );
            processingEnv.getMessager().printMessage(NOTE,
                "generated "+viewName+" source file for "+controller);
        }
    }

    void storeCode(String code, String className) {
        try ( var out = processingEnv.getFiler().createSourceFile(className).openWriter() ) {
            out.write(code);
        }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }

    String generateCode(NodeBuilder builder, TypeElement controller, View view) {
        var viewName = view.value();
        try ( var in = getInputStream(viewName) ) {
            if (in == null) return null;
            var viewType = view.nodeType();
            var includeType = view.includeType();
            var controllerTag = Controller.class.getName();
            var controllerName = controller.toString();
            return builder
                .setView(viewName,viewType)
                .setController(controllerName,controllerTag)
                .setInclude(includeType)
                .transform(in);
        }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }

    InputStream getInputStream(String file) {
        String path = "";
        var p = file.lastIndexOf('.');
        if (p > -1) {
            path = file.substring(0,p);
            file = file.substring(p+1);
        }
        file += ".fxml";
        try {
            var stream = fromSourcePath(path,file);
            return stream != null ? stream : fromStandardPath(path,file);
        }
        catch (Exception nf) {
            processingEnv.getMessager().printMessage(WARNING,
                path.replace('.','/')+'/'+file+" file not found in -sourcepath");
            return null;
        }
    }

    InputStream fromStandardPath(String path, String file) throws Exception {
        return processingEnv.getFiler()
               .getResource(StandardLocation.SOURCE_PATH,path,file)
               .openInputStream();
    }

    InputStream fromSourcePath(String path, String file) throws Exception {
        for (var dir:sourcePath) {
            path = path.replace('.','/') + '/' + file;
            var source = dir.resolve(path);
            if (Files.isRegularFile(source)) {
                return Files.newInputStream(source);
            }
        }
        return null;
    }

    List<Path> getSourcePath(String paths) {
        var list = new ArrayList<Path>();
        if (paths != null && !paths.isBlank()) {
            for (var p:paths.split(",")) {
                if (p.isBlank()) continue;
                var path = Paths.get(p.trim());
                if (Files.isDirectory(path)) {
                    list.add(path);
                }
            }
        }
        return list;
    }

}

// Netbeans notes for SOURCE_PATH:
//  1. check 'Run Compilation in External VM'
//  2. add '-sourcepath src' to 'Additional Compiler Options'

// Eclipse notes for SOURCE_PATH:
//  1. enable annotation processing
//  2. add '-Asourcepath=src' 
