package fx.node.builder;

import java.util.Formatter;
import java.text.MessageFormat;

import fx.node.builder.Beans.Bean;
import fx.node.builder.Beans.Property;
import static fx.node.builder.Beans.*;

import fx.util.FIFO;
import fx.util.LIFO;

class NodeBuilder {

  String packageName;
  String simpleName;
  String controllerName;

  StringBuilder body = new StringBuilder();
  Formatter formatter = new Formatter(body);

  void format(String format, Object... args) {
    formatter.format(format,args);
  }

  static final String viewTemplate =
    "package {0};\n" +
    "@fx.mvc.Controller(\"{0}.{1}\")\n" +
    "class {2} '{' static javafx.scene.Parent view({0}.{1} _0) '{'\n" +
    "{3}" +
    "return _1;\n" +
    "'}}'\n";

  String view(String name) {
    resolveNames(name);
    return MessageFormat //         {0}             {1}         {2}   {3}
      .format(viewTemplate, packageName, controllerName, simpleName, body );
  }

  Beans bp = Beans.get();
  int nVar;
  Scope let;
  LIFO<Scope> scopes = new LIFO<>();

  class Scope {
    Scope(String n, String t) {
      bean = bp.get(t);
      name = n;
      seq = ++nVar;
    }
    Bean bean;
    String name;
    int seq;
    FIFO<Object> args = new FIFO<>(); // either constructor(args) or list.{add,addAll}(args)
    @Override public String toString() { return "_"+seq; }
  }

  void setPackage(String name) {
    packageName = name;
  }
  void addImport(String name) {
    bp.put(name);
  }

  void newVar(String name, String type) {
    let = new Scope(name,type);
  }
  void varArg(Object value) {
    let.args.add(value);
  }
  void argEnd() {
    var ctor = specialVar(let);
    if (ctor != null) {
      format("var _%d = %s;\n", let.seq, ctor);
    } else {
      var args = collect(let.args);
      format("var _%d = new %s(%s);\n", let.seq, let.bean.type, args);
    }
    if (let.name != null) {
      setProperty(let.name,let);
    } else {
      var s = scopes.peek();
      if (s != null) s.args.add(let);
    }
  }

  void newList(String tag) {
    scopes.peek().args.add(tag); // remember tag
  }
  void listEnd() {
    var s = scopes.peek();
    var c = collect(s.args);
    var i = c.indexOf(',');
    var all = c.indexOf(',',i+1) > 0 ? "All" : "";
    format("_%d.get%s().add%s(%s);\n", s.seq, proper(c.substring(0,i)), all, c.substring(i+1) );
  }

  void addItem() { // _1.getChildren().add(_3);
    var s = scopes.peek();
    format("_%d.get%s().add(_%d);\n", s.seq, bp.defaultProperty(s.bean), let.seq);
    let = s;
  }

  void varBody(String name) {
    if (name == null) {
      scopes.push(let);
    }
    // TODO: handle 'name: {...}' ?
  }
  void bodyEnd() {
    scopes.pop();
  }

  void setProperty(String property, Object value) {
    int i = property.indexOf('.');
    if (i < 0) {
      local(property,value);
    } else {
      if (isSpecial(property)) {
        special(property,value);
      } else {
        global(property,i,value);
      }
    }
  }

  static boolean isSpecial(String s) {
    return s.startsWith("fx.");
  }

  void special(String property, Object value) {
    switch (property) {
      case "fx.controller" -> { controllerName = String.valueOf(value); }
      case "fx.id" -> { local("id",value); }
      default -> format("// special %s `%s`\n", property, value);
    }
  }

  void global(String property, int dot, Object value) {
    var s = scopes.peek();
    var b = bp.get(property.substring(0,dot)); // n[0] = type
    property = property.substring(dot+1);      // n[1] = property
    var p = b.get(property);
    var c = resolve(p,value);
    format("%s.set%s(_%d,%s);\n", b.type, p.name, s.seq, c[2] );
  }

  void local(String property, Object value) {
    var s = scopes.peek();
    var p = s.bean.get(property);
    var c = resolve(p,value);
    format("_%d.%s%s%s(%s);\n", s.seq, c[0], p.name, c[1], c[2] );
  }

  // { String prefix, suffix, value; }
  String[] resolve(Property p, Object v) {
    var t = p.kind;
    if (t == ARRAY) {
      return new String[]{ "get", "().add", coerce(p.component,v) }; // isA(p.item)
    } else {
      var s = (t == OBJECT) ? object(p,v) : coerce(t,v);
      return new String[]{ "set", "", s };
    }
  }

  static String coerce(int isA, Object v) {
    if (v instanceof Scope s) {
      return s.toString();
    }
    return switch (isA) {
      case NUMBER, BOOLEAN -> String.valueOf(v);
      case STRING -> quote(v);
      case NULL -> "null";
      // case ARRAY, OBJECT
      default -> "?"+isA+'`'+v+'`';
    };
  }

  String object(Property p, Object v) {
    if (v instanceof String s) {
      if (bp.isAssignable(p.type, "java.lang.Enum")) { // (Enum.class.isAssignableFrom(p.type)) {
        var i = s.lastIndexOf('.');
        var e = i < 0 ? s : s.substring(i+1);
        return p.type + '.' + e.toUpperCase(); // p.type.getName() + '.' + e.toUpperCase();
      } else {
        return reference(s);
      }
    }
    return String.valueOf(v);
  }

  String reference(String s) {
    return switch(s.charAt(0)) {
      case '@' -> resource(s);
      case '#' -> function(s);
      default -> quote(s);
    };
  }

  String function(String s) {
    var ref = s.substring(1);
    controllerReferences.add(ref);
    return "_0::" + ref;
  }

  static String resource(String s) {
    return "_0.getClass().getResource(\"" + s.substring(1) + "\").toString()";
  }

  static String quote(Object v) {
    return "\"" + v + '"'; // TODO: encode " chars in v
  }

  static String collect(FIFO<Object> args) {
    if (args.isEmpty()) return "";
    var s = new StringBuilder();
    for (var a:args) {
      s.append(',').append(String.valueOf(a));
    }
    args.clear(); // clean-up the collector
    return s.substring(1);
  }

  void resolveNames(String name) {
    var i = name.lastIndexOf('.');
    if (i < 0) {
      packageName = null;
      simpleName = name;
    } else {
      packageName = name.substring(0,i);
      simpleName = name.substring(i+1);
    }
    if (controllerName == null) {
      controllerName = "None";
    }
    if (packageName == null) {
      var p = controllerName.lastIndexOf('.');
      packageName = p < 0 ? "none" : controllerName.substring(0,p);
    }
    if (controllerName.startsWith(packageName+'.')) {
      controllerName = controllerName.substring(packageName.length()+1);
    }
  }

  String specialVar(Scope s) {
    if (bp.isSameType(s.bean.type, "java.net.URL")) { // s.bean.type == java.net.URL.class
      var args = collect(s.args);
      if (!args.isEmpty()) return reference(args);
    }
    return null;
  }

  FIFO<String> controllerReferences = new FIFO<>();

/*
   static String controllerTemplate =
     "package example;
     "@fx.mvc.View("example.Login")
     "interface LoginController {
     "void handleSubmitButtonAction(javafx.event.ActionEvent e);
     "}

   String controller() {
     1. generate controllerReference body
     2. generate text from controllerTemplate
   }
*/

}
