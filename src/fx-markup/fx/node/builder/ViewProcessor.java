package fx.node.builder;

import java.util.Set;
import java.util.Optional;
import java.util.function.BiFunction;

import java.io.IOException;
import java.io.UncheckedIOException;

import javax.tools.FileObject;
import static javax.tools.Diagnostic.Kind.*;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

import fx.mvc.View;

public class ViewProcessor extends AbstractProcessor {

  @Override
  public void init(ProcessingEnvironment pe) {
    super.init(pe);
    Beans.provider = () -> new Refractor(processingEnv);
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
    roundEnv.getElementsAnnotatedWith(View.class)
      .stream()
      .filter(e -> e.getKind() == ElementKind.CLASS)
      .map(e -> new Job(e, e.getAnnotation(View.class).value()) )
      .map(j -> { return j.view.isBlank() ? Optional.<Job>empty() : Optional.of(j); })
      .forEach(j ->
        j.map(this::findSource)
         .map(this::getTransform)
         .map(this::generateCode)
         .ifPresent(k ->
            note("generated " + k.view + " source file for " + k.element)
         )
      );
    return true;
  }

  record Job(Element element, String view) {}

  Source findSource(Job job) {
    var p = splitPath(job.view); // view is fqcn format -> package.path.Class
    var file = findResource( p[0], p[1]+".jbml" );
    if (file == null) {
      file = findResource( p[0], p[1]+".fxml" );
    }
    if (file == null) {
      error("no markup file found for " + job.view + " in -sourcepath");
      return null;
    }
    return new Source(job,file);
  }

  record Source(Job job, FileObject file) {}

  Transform getTransform(Source src) {
    var f = src.file.getName();
    var s = f.substring(f.length()-5);
    BiFunction<String,char[],String> form =
      switch (s) {
        case ".jbml" -> this::jbmlTransform;
        case ".fxml" -> this::fxmlTransform;
        default -> null;
      };
    if (form == null) {
      error("no markup/transform available for "+src.job.view);
      return null;
    }
    return new Transform(src.job, src.file, form);
  }

  record Transform(Job job, FileObject file, BiFunction<String,char[],String> form) {}

  Job generateCode(Transform gen) {
    var in = readChars(gen.file);
    var out = gen.form.apply(gen.job.view, in);
    writeChars(gen.job.view, out);
    return gen.job;
  }

  void note(String msg) { processingEnv.getMessager().printMessage(NOTE,msg); }
  void warn(String msg) { processingEnv.getMessager().printMessage(WARNING,msg); }
  void error(String msg) { processingEnv.getMessager().printMessage(ERROR,msg); }

  static String[] splitPath(String s) {
    var i = s.lastIndexOf('.');
    return i < 0 ? new String[]{ "", s }
                 : new String[]{ s.substring(0,i), s.substring(i+1) };
  }

  FileObject findResource(String pkg, String file) { // add suffix
    try { return SourceFile.get(processingEnv, pkg, file); }
    catch (Exception ignore) { return null; }
  }

  char[] readChars(FileObject file) {
    try {
      var text = file.getCharContent(true).toString();
      return text.isBlank() ? new char[0] : text.toCharArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  void writeChars(String className, String text) {
    if (text.isBlank()) return;
    try (var out = processingEnv.getFiler().createSourceFile(className).openWriter()) {
      out.write(text);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  String jbmlTransform(String view, char[] src) {
    if (src.length == 0) return "";
    return new JbmlDriver().transform(view,src);
  }

  String fxmlTransform(String view, char[] src) {
    if (src.length == 0) return "";
    return new FxmlDriver().transform(view,src);
  }

}
