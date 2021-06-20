package fx.node.builder;

import java.util.Formatter;
import java.text.MessageFormat;

import fx.node.builder.Beans.Bean;
import fx.node.builder.Beans.Property;

import static fx.node.builder.Beans.*;

import fx.util.FIFO;
import fx.util.LIFO;
import fx.util.XML;

class NodeBuilder {

  NodeBuilder(String name) {
    var p = name.lastIndexOf('.');
    if (p < 0) {
      simpleName = name;
    } else {
      packageName = name.substring(0,p);
      simpleName = name.substring(p+1);
    }
  }

  String simpleName;
  String packageName;
  String controllerName;

  StringBuilder body = new StringBuilder();
  Formatter formatter = new Formatter(body);

  void format(String format, Object... args) {
    formatter.format(format,args);
  }

  static final String viewTemplate =
    "package {0};\n" +
    "@fx.mvc.Controller(\"{1}\")\n" +
    "class {2} '{' static javafx.scene.Parent view({1} _0) '{'\n" +
    "{3}" +
    "return _1;\n" +
    "'}}'\n";

  String view() {
    return MessageFormat //        {0}               {1}           {2}    {3}
      .format(viewTemplate, packageName(), controllerName(), simpleName, body );
  }

  String packageName() {
    return packageName != null ? packageName
         : controllerName != null ? XML.prefix(controllerName)
         : "";
  }

  String controllerName() {
    return controllerName == null ? packageName + '.' + simpleName + "Controller"
         : controllerName.contains(".") ? controllerName
         : packageName + '.' + controllerName;
  }

  void setPackage(String name) {
    packageName = name;
  }

  Beans bp = Beans.get();

  void addImport(String name) {
    bp.put(name);
  }

  int nVar;
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

  void newVar(String name, String type) {
    Scope s;
    if (isInclude(type)) {
      s = new Scope(name,"Node");
      s.args.add(type);
    } else {
      s = new Scope(name,type);
    }
    scopes.push(s);
  }

  void varArg(Object value) {
    var s = scopes.peek();
    s.args.add(value);
  }

  Scope let; // last scope popped from stack

  void argEnd() {
    let = scopes.pop();
    var ctor = specialVar(let);
    if (ctor != null) {
      format("var _%d = %s;\n", let.seq, ctor);
    } else {
      var args = collect(let.args);
      format("var _%d = new %s(%s);\n", let.seq, let.bean.type, args);
    }
    if (let.name != null) {
      varProperty(let.name,let);
    } else {
      var s = scopes.peek();
      if (s != null) s.args.add(let); // collect children
    }
  }

  void varBody(String name) {
    if (let != null) {
      scopes.push(let);
      let = null;
    }
  }

  void bodyEnd() {
    let = scopes.pop();
  }

  void itemEnd() { // assumes 'let' is valid
    var s = scopes.peek();
    format("_%d.get%s().add(_%d);\n", s.seq, bp.defaultProperty(s.bean), let.seq);
  }

  void varProperty(String property, Object value) {  // setProperty
    var i = property.indexOf('.');
    if (i < 0) {
      setLocal(property,value);
    } else {
      if (isSpecial(property)) {
        setSpecial(property,value);
      } else {
        setGlobal(property,i,value);
      }
    }
  }

  void setGlobal(String property, int dot, Object value) {
    var s = scopes.peek();
    var b = bp.get(property.substring(0,dot)); // n[0] = type
    property = property.substring(dot+1);      // n[1] = property
    var p = b.get(property);
    var c = resolve(p,value);
    format("%s.set%s(_%d,%s);\n", b.type, p.name, s.seq, c[2] );
  }

  void setLocal(String property, Object value) {
    var s = scopes.peek();
    var p = s.bean.get(property);
    var c = resolve(p,value);
    format("_%d.%s%s%s(%s);\n", s.seq, c[0], p.name, c[1], c[2] );
  }

  static boolean isSpecial(String s) {
    return s.startsWith("fx.");
  }

  void setSpecial(String property, Object value) { // special
    switch (property) {
      case "fx.controller" -> { controllerName = String.valueOf(value); }
      case "fx.id" -> { setLabel(value); }
      case "fx.define" -> {} // NOTE: ignore for now
      default -> format("// special %s `%s`\n", property, value);
    }
  }

  void setLabel(Object value) {
    var s = scopes.peek();
    format("%s %s = _%d;\n", s.bean.type, value, s.seq);
    if (s.bean.get("id") != null) {
      setLocal("id",value);
    }
    // TODO: maybe use *.getProperties().put("id",value) as alternative to *.setId(value)
  }

  void newList(String name) {
    var s = scopes.peek();
    s.args.add(name); // remember list name
  }

  void listEnd() {
    var s = scopes.peek();
    var c = collect(s.args);
    var i = c.indexOf(',');
    var all = c.indexOf(',',i+1) > 0 ? "All" : "";
    format("_%d.get%s().add%s(%s);\n", s.seq, proper(c.substring(0,i)), all, c.substring(i+1) );
  }

  static String collect(FIFO<Object> args) {
    if (args.isEmpty()) return "";
    var b = new StringBuilder();
    for (var a:args) {
      b.append(',').append(String.valueOf(a));
    }
    args.clear(); // clean-up the collector
    return b.substring(1);
  }

  static boolean isInclude(Object s) {
    return "fx.include".equals(s);
  }

  String specialVar(Scope s) {
    var fx = s.args.peek();
    if (isInclude(fx)) {
      return includeVar(s);
    } else {
      if ("java.net.URL".equals(s.bean.type)) return urlRef(s);
      if ("javafx.scene.image.Image".equals(s.bean.type)) return urlVar(s);
    }
    return null;
  }

  String includeFunction = bp.defaultValue("fx.mvc.View","includeFunction");

  String includeVar(Scope s) {
    s.args.take(); // fx.include
    var source = clip(s.args.take()); // file.path
    return "%s(%s.class,\"%s\")".formatted( includeFunction, simpleName, source );
  }

  static String clip(Object v) {
    var s = String.valueOf(v);
    var p = s.lastIndexOf('/');
    if (p < 0) p = 0;
    p = s.indexOf('.',p);
    return p < 0 ? s : s.substring(0,p);
  }

  String urlVar(Scope s) {
    return "new %s(%s)".formatted(s.bean.type, urlRef(s));
  }

  String urlRef(Scope s) {
    var args = collect(s.args);
    return args.isEmpty() ? null : reference(args);
  }

  String reference(String s) {
    return switch(s.charAt(0)) {
      case '@' -> resource(s);
      case '#' -> function(s);
      case '$' -> s.substring(1); // local label
      default -> quote(s);
    };
  }

  String enumerate(Property p, Object v) {
    if (v instanceof String s) {
      var i = s.lastIndexOf('.');
      var e = i < 0 ? s : s.substring(i+1);
      return p.type + '.' + e.toUpperCase();
    }
    return String.valueOf(v);
  }

  static String quote(Object v) {
    return "\"" + v + '"'; // TODO: encode " chars in v
  }

  String resource(String s) {
    return simpleName+".class.getResource(\"" + s.substring(1) + "\").toString()";
  }

  String function(String s) {
    var ref = s.substring(1);
    controllerReferences.add(ref);
    return "_0::" + ref;
  }

  FIFO<String> controllerReferences = new FIFO<>(); // TODO: for generating controller prototype

  /*
  static String controllerTemplate =
    "package example;
    "@fx.mvc.View("example.Login")
    "interface LoginController {
    "void handleSubmitButtonAction(javafx.event.ActionEvent e);
    "}
  */

  // { String prefix, suffix, value; }
  String[] resolve(Property p, Object v) {
    return switch(p.kind) {
      case ARRAY -> new String[]{ "get", "().add", coerce(p.component,v) };
      case OBJECT -> new String[]{ "set", "", object(v) };
      case ENUM -> new String[]{ "set", "", enumerate(p,v) };
      default -> new String[]{ "set", "", coerce(p.kind,v) };
    };
  }

  String object(Object v) {
    return v instanceof String s? reference(s) : String.valueOf(v);
  }

  static String coerce(int isA, Object v) {
    if (v instanceof Scope s) {
      return s.toString();
    }
    return switch (isA) {
      case NUMBER, BOOLEAN -> String.valueOf(v);
      case STRING -> quote(v);
      case NULL -> "null";
      default -> "?"+isA+'`'+v+'`';
    };
  }

  // helper api for FXML driver

  boolean isCollection(String property) {
    var s = scopes.peek();
    var p = s.bean.get(property);
    return p != null && p.kind == ARRAY;
  }

  String[][] signatures(String type) {
    var b = bp.get(type);
    return bp.signatures(b);
  }

}
