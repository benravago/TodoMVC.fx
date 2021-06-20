package fx.node.builder;

import java.util.Collection;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javafx.beans.DefaultProperty;
import javafx.beans.NamedArg;

class Reflector extends Beans { // use java.lang.reflect.* api

  // Beans.provider = () -> new Reflector();

  static Class<?> type(Bean bean) {
    try {
      return Class.forName(bean.type);
    } catch (ClassNotFoundException ignore) {
      return null;
    }
  }

  static int isA(Type t) {
    if (t instanceof Class<?> c) {
      if (Enum.class.isAssignableFrom(c)) return ENUM;
      if (CharSequence.class.isAssignableFrom(c)) return STRING;
      if (Collection.class.isAssignableFrom(c) || c.isArray()) return ARRAY;
      if (Boolean.class.isAssignableFrom(c) || Boolean.TYPE.equals(c)) return BOOLEAN;
      if (Number.class.isAssignableFrom(c) || c.isPrimitive()) return NUMBER;
      return OBJECT;
    }
    return NULL;
  }

  @Override
  void log(String msg) { System.out.println(msg); }

  @Override
  Bean beanOf(String fqcn) {
    var bean = new Bean();
    bean.type = fqcn;
    var c = type(bean);
    if (c != null) {
      bean.kind = isA(c);
      return bean;
    }
    return null;
  }

  Property propertyOf(Bean bean, String name) {
    var p = new Property();
    p.name = proper(name);
    var m = propertyType(bean,p.name);
    if (m instanceof ParameterizedType pt) {
      var rt = pt.getRawType();
      p.type = rt.getTypeName();
      p.kind = isA(rt);
      if (p.kind == ARRAY) {
        p.component = isA(typeArgument(pt));
      }
    } else if (m instanceof Class<?> c) {
      p.type = c.getName();
      p.kind = isA(c);
    } else {
      return null;
    }
    return p;
  }

  Type propertyType(Bean bean, String name) {
    var is = "is" + name;
    var get = "get" + name;
    for (var m : type(bean).getMethods()) {
      var pc = Modifier.isStatic(m.getModifiers()) ? 1 : 0;
      var n = m.getName();
      if ((n.equals(get) || n.equals(is)) && m.getParameterCount() == pc) {
        return m.getGenericReturnType();
      }
    }
    return null;
  }

  Type typeArgument(ParameterizedType pt) {
    var t = pt.getActualTypeArguments();
    return t.length > 0 ? t[0] : null;
  }

  @Override
  String defaultProperty(Bean bean) {
    var c = type(bean);
    var a = c.getAnnotation(DefaultProperty.class);
    if (a != null) {
      return proper(a.value());
    }
    try {
      c.getMethod("getChildren");
      return "Children";
    }
    catch (NoSuchMethodException ignore) {
      return null;
    }
  }

  @Override
  String[][] namedArg(Bean bean) {
    var ctor = type(bean).getConstructors();
    var sig = new String[ctor.length][];
    for (var i = 0; i < ctor.length; i++) {
      var pa = ctor[i].getParameterAnnotations(); // Annotation[][]
      if (pa.length == 0) continue;
      sig[i] = new String[pa.length];
      for (var j = 0; j < pa.length; j++) {
        for (var a:pa[j]) {
          if (a instanceof NamedArg na) {
            sig[i][j] = na.value();
            break; // inner loop
          }
        }
      }
    }
    return sig;
  }

  @SuppressWarnings("unchecked")
  @Override
  <T> T defaultValue(String annotation, String name) {
    try {
      return (T) Class.forName(annotation).getMethod(name).getDefaultValue();
    } catch (Exception e) {
      return null;
    }
  }

}
