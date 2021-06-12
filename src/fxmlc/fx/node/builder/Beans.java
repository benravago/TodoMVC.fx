package fx.node.builder;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Supplier;

abstract class Beans {

  static final int
    OBJECT = 'O',
    ARRAY = 'A',
    STRING = 'S',
    NUMBER = 'N',
    BOOLEAN = 'B',
    NULL = 'U';

  class Bean {
    String type; // Class<?>
    int kind; // isA

    Map<String,Property> map = new HashMap<>();
    Property get(String name) { return find(this,name); }
  }

  class Property {
    String name; // should be Proper
    String type; // Class<?>
    int kind; // isA
    int component; // Class<?> item;
  }

  static Supplier<Beans> provider = () -> null;
  static Beans get() { return provider.get(); }

  Map<String,Bean> map = new HashMap<>();
  List<String> pkgs = new ArrayList<>();

  void reset() {
    map.clear();
    pkgs.clear();
  }

  // Class.simpleName
  Bean get(String simpleName) {
    var bean = map.get(simpleName);
    if (bean == null) {
      bean = lookup(simpleName);
      if (bean != null) {
        map.put(simpleName,bean);
      }
    }
    return bean;
  }

  Bean lookup(String cls) {
    for (var pkg:pkgs) {
      var bean = beanOf(pkg+cls);
      if (bean != null) return bean;
    }
    return null;
  }

  // Class.name or Package.name+'.*'
  void put(String qualifiedName) {
    if (qualifiedName.endsWith(".*")) {
      pkgs.add(qualifiedName.substring(0,qualifiedName.length()-1));
    } else {
      var bean = beanOf(qualifiedName);
      if (bean != null) {
        map.put(tag(qualifiedName),bean);
      } else {
        log("Class not found: "+qualifiedName);
      }
    }
  }

  String tag(String name) {
    var i = name.lastIndexOf('.');
    return i < 0 ? name : name.substring(i+1);
  }

  Property find(Bean bean, String name) {
    var prop = bean.map.get(name);
    if (prop == null) {
      prop = propertyOf(bean,name);
      if (prop != null) {
        bean.map.put(name,prop);
      } else {
        log("Property not found: "+name+" in "+bean.type);
      }
    }
    return prop;
  }

  abstract
  void log(String msg);

  abstract
  Bean beanOf(String name);
  abstract
  Property propertyOf(Bean bean, String property);

  abstract
  boolean isAssignable(String t1, String t2);

  boolean isSubtype(String t1, String t2) { return false; } // TODO: currently not used
  boolean isSameType(String t1, String t2) { return t1.equals(t2); } // simple implementation

  abstract
  String defaultProperty(Bean bean);

  abstract
  String[][] namedArg(Bean bean);

  String[][] signatures(Bean bean) {
    var sig = namedArg(bean);
    // prune list to only signatures with all @NamedArg parameters
    if (sig.length > 0) {
      int i = 0;
      var a = new String[sig.length][];
      for (var b:sig) {
        if (b == null) continue;
        if (b.length == 0) continue;
        var n = 0;
        for (var c:b) if (c != null) n++;
        if (b.length == n) a[i++] = b;
      }
      if (i < sig.length) {
        sig = new String[i][];
        if (i > 0) {
          System.arraycopy(a,0,sig,0,i);
        }
      }
    }
    return sig;
  }


  static String proper(String s) {
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  static boolean isProper(String s) {
    return Character.isUpperCase(s.charAt(0));
  }

}
