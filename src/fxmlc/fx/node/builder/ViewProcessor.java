package fx.node.builder;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;

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
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

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
        var builder = new NodeBuilder(forms(eu));
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

    static String elementName(TypeMirror typeMirror) {
        var s = typeMirror.toString();
        var p = s.indexOf('<');
        return p > 0 ? s.substring(0,p) : s;
    }

    Forms forms(Elements eu) {
      return new Forms() {

        @Override
        Form makeForm(String className) {
            var c = eu.getTypeElement(className);
            return c == null ? null :
                new Form(className,properties(c),interfaces(c),superclass(c));
        }

        Map<String,String> properties(TypeElement te) {
            var map = new HashMap<String,String>();
            for (var e : te.getEnclosedElements()) {
                if (e.getKind() == ElementKind.METHOD) {
                    var m = (ExecutableElement)e;
                    // TODO: check parameter count = 0
                    var name = property(m.getSimpleName().toString());
                    if (name != null) {
                        map.put(name,elementName(m.getReturnType()));
                    }
                }
            }
            return map;
        }

        Set<String> interfaces(TypeElement te) {
            var set = new HashSet<String>();
            for (var tm : te.getInterfaces()) {
                set.add(elementName(tm));
            }
            return set;
        }

        Form superclass(TypeElement te) {
            var sc = te.getSuperclass();
            return sc != null ? get(elementName(sc)) : null;
        }

        @Override
        List<String> classesIn(String packageName) {
            var list = classesInBin(packageName);
            return list.isEmpty() ? super.classesIn(packageName) : list;
        }

        List<String> classesInBin(String packageName) {
            var pe = eu.getPackageElement(packageName);
            return pe != null ?
                pe.getEnclosedElements().stream()
                  .filter(e -> e.getKind() == ElementKind.CLASS)
                  .map(e -> e.toString())
                  .collect(Collectors.toList())
                : Collections.emptyList();
        }

      };
    }

}
