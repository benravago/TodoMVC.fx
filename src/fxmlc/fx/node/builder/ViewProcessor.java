package fx.node.builder;

import java.util.Set;

import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import static javax.tools.Diagnostic.Kind.*;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

import fx.mvc.View;
import fx.mvc.Controller;

public class ViewProcessor extends AbstractProcessor {

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
            return SourceFile.get(processingEnv,path,file).openInputStream();
        }
        catch (Exception nf) {
            processingEnv.getMessager().printMessage(WARNING,
                path.replace('.','/')+'/'+file+" file not found in -sourcepath");
            return null;
        }
    }

}