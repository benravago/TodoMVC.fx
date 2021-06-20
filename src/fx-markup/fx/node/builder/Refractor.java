package fx.node.builder;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

import fx.util.FIFO;

class Refractor extends Beans { // use java.lang.model.* api

  // Beans.provider = () -> new Refractor(processingEnv);

  Refractor(ProcessingEnvironment env) {
    elements = env.getElementUtils();
    types = env.getTypeUtils();
    messager = env.getMessager();
    setUp();
  }

  final Messager messager;

  @Override
  void log(String msg) { messager.printMessage(Kind.NOTE,msg); };

  final Elements elements;
  final Types types;

  @Override
  Bean beanOf(String fqcn) {
    var te = elements.getTypeElement(fqcn);
    if (te != null) {
      var bean = new Bean();
      bean.type = fqcn;
      bean.kind = isA(te.asType());
      return bean;
    }
    return null;
  }

  @Override
  Property propertyOf(Bean bean, String name) {
    var part = new Property();
    part.name = proper(name);
    var m = propertyType(bean,part.name);
    if (m instanceof DeclaredType dt) {
      part.type = qualifiedName(dt);
      part.kind = isA(dt);
      if (part.kind == ARRAY) {
        part.component = isA(typeArgument(dt));
      }
    } else if (m instanceof PrimitiveType pt) {
      part.type = box(pt.toString());
      part.kind = part.type.equals("java.lang.Boolean") ? BOOLEAN : NUMBER;
    } else {
      log("unknown propertyType "+bean.type+' '+name+' '+m);
      part = null;
    }
    return part;
  }

  TypeMirror propertyType(Bean bean, String name) {
    var is = "is" + name;
    var get = "get" + name;
    for (var te = elements.getTypeElement(bean.type); te != null; te = superclass(te)) {
      for (var p:te.getEnclosedElements()) {
        if (p instanceof ExecutableElement e && e.getKind() == ElementKind.METHOD) {
          var n = e.getSimpleName();
          if (n.contentEquals(is) || n.contentEquals(get)) {
            var c = e.getModifiers().contains(Modifier.STATIC) ? 1 : 0;
            if (c == e.getParameters().size()) {
              return e.getReturnType();
            }
          }
        }
      }
    }
    return null;
  }

  TypeElement superclass(TypeElement te) {
    return te.getSuperclass() instanceof DeclaredType dt
        && dt.asElement() instanceof TypeElement st ? st : null;
  }

  String qualifiedName(DeclaredType dt) {
    return dt.asElement() instanceof TypeElement te
         ? te.getQualifiedName().toString() : dt.toString();
  }

  TypeMirror typeArgument(DeclaredType dt) {
    var t = dt.getTypeArguments();
    return t.isEmpty() ? null : t.get(0);
  }

  @Override
  String defaultProperty(Bean bean) {
    var te = elements.getTypeElement(bean.type);
    if (te == null) return null;
    var v = annotation(te,"DefaultProperty","value");
    return v != null ? proper(v) : childrenProperty(te);
  }

  String childrenProperty(TypeElement pt) {
    for (var te = pt; te != null; te = superclass(te)) {
      for (var p:te.getEnclosedElements()) {
        if (p instanceof ExecutableElement e && e.getKind() == ElementKind.METHOD) {
          if (e.getSimpleName().contentEquals("getChildren") && e.getParameters().size() == 0) {
            return "Children";
          }
        }
      }
    }
    return null;
  }

  @Override
  String[][] namedArg(Bean bean) {
    var te = elements.getTypeElement(bean.type);
    if (te == null) return null;
    // scan constructors
    var ctor = new FIFO<FIFO<String>>();
    for (var c:te.getEnclosedElements()) {
      if (c instanceof ExecutableElement e && e.getKind() == ElementKind.CONSTRUCTOR) {
        var arg = new FIFO<String>();
        for (var p:e.getParameters()) {
          arg.add(annotation(p,"NamedArg","value"));
        }
        ctor.add(arg);
      }
    }
    var sig = new String[ctor.size()][]; var i = 0;
    for (var arg:ctor) {
      var name = new String[arg.size()]; int j = 0;
      for (var p:arg) name[j++] = p;
      sig[i++] = name;
    }
    return sig;
  }

  String annotation(Element e, String name, String item) {
    for (var am:e.getAnnotationMirrors()) {
      var at = am.getAnnotationType().asElement();
      if (at.getSimpleName().contentEquals(name)) {
        for (var ev:am.getElementValues().entrySet()) {
          if (ev.getKey().getSimpleName().contentEquals(item)) {
            return ev.getValue().getValue().toString();
          }
        }
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  <T> T defaultValue(String annotation, String name) {
    var a = elements.getTypeElement(annotation);
    if (a != null) {
      for (var p:a.getEnclosedElements()) {
        if (p.getKind() == ElementKind.METHOD && p instanceof ExecutableElement e) {
          if (e.getSimpleName().contentEquals(name)) return (T) e.getDefaultValue().getValue();
        }
      }
    }
    return null;
  }

  int isA(TypeMirror t) {
    return switch(t.getKind()) {
      case BOOLEAN -> BOOLEAN;
      case BYTE, CHAR, SHORT, INT, LONG, FLOAT, DOUBLE -> NUMBER;
      case ARRAY -> ARRAY;
      case DECLARED -> declared((DeclaredType)t);
      default -> NULL;
    };
  }

  int declared(DeclaredType t) {
    return switch (t.asElement().getKind()) {
      case ENUM -> ENUM;
      case CLASS, INTERFACE -> assignable(t);
      default -> NULL;
    };
  }

  int assignable(TypeMirror t) {
    var e = types.erasure(t); // remove <E> for isAssignable(); for jdk's javac
    if (types.isAssignable(e,jlCharSequence)) return STRING;
    if (types.isAssignable(e,juCollection)) return ARRAY;
    if (types.isAssignable(e,jlNumber)) return NUMBER;
    if (types.isAssignable(e,jlBoolean)) return BOOLEAN;
    return OBJECT;
  }

  TypeMirror jlNumber, jlBoolean, jlCharSequence, juCollection;

  void setUp() {
    jlCharSequence = elements.getTypeElement("java.lang.CharSequence").asType();
    jlNumber = elements.getTypeElement("java.lang.Number").asType();
    jlBoolean = elements.getTypeElement("java.lang.Boolean").asType();
    juCollection = elements.getTypeElement("java.util.Collection").asType();
  }

  static String box(String p) {
    return switch(p) {
      case "boolean" -> "java.lang.Boolean";
      case "byte" -> "java.lang.Byte";
      case "short" -> "java.lang.Short";
      case "int" -> "java.lang.Integer";
      case "long" -> "java.lang.Long";
      case "char" -> "java.lang.Character";
      case "float" -> "java.lang.Float";
      case "double" -> "java.lang.Double";
      default -> "?"+p;
    };
  }
}
