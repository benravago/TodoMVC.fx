package fx.node.builder;

import java.io.CharArrayReader;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartElement;
import static javax.xml.stream.XMLStreamConstants.*;

import fx.util.LIFO;
import fx.util.FIFO;
import static fx.util.XML.*;
import static fx.node.builder.Beans.*;

class FxmlDriver {

  static XMLInputFactory factory = XMLInputFactory.newDefaultFactory();

  NodeBuilder gen;

  String transform(String name, char[] input) {
    transform(input, new NodeBuilder(name));
    return gen.view();
  }

  void transform(char[] input, NodeBuilder builder) {
    gen = builder;
    try {
      var in = new CharArrayReader(input);
      var reader = factory.createXMLEventReader(in);
      while (reader.hasNext()){
        var e = reader.nextEvent();
        switch (e.getEventType()) {
          case PROCESSING_INSTRUCTION -> processingInstruction((ProcessingInstruction)e);
          case START_ELEMENT -> startElement((StartElement)e);
          case END_ELEMENT -> endElement((EndElement)e);
          case CHARACTERS -> characters((Characters)e);
          case START_DOCUMENT, END_DOCUMENT -> {} // ignore
          case COMMENT -> {} // ignore
          default -> System.err.println("? "+e);
        }
      }
    }
    catch (Throwable t) { uncheck(t); }
  }

  void processingInstruction(ProcessingInstruction pi) {
    switch (pi.getTarget()) {
      case "package" -> gen.setPackage(pi.getData());
      case "import" -> gen.addImport(pi.getData());
    }
  }

  void startElement(StartElement e) {
    var tag = push(name(e.getName()));
    if (isSpecial(tag,e)) {
      // <URL>, <fx.include ..., etc
    } else {
      if (isProper(tag.name)) {
        tag.isNode = true;
        bean(tag,e); // <Type>
      } else {
        property(tag,e); // <property>, <fx.define>, etc.
      }
    }
  }

  void endElement(EndElement e) {
    pop();
  }

  void characters(Characters e) {
    var c = e.getData();
    if (!c.isBlank()) {
      System.err.printf("- characters: %s\n", c);
    }
    // TODO:
    // 1. use the character data as text;
    // 2. check current top of tags stack;
    // 3. call gen.varProperty(name,value); assume this is OK
  }


  LIFO<Tag> tags = new LIFO<>();

  class Tag {
    Tag up;
    String name;
    Runnable end;
    boolean isNode;
  }

  Tag push(String name) {
    var t = new Tag();
    t.up = tags.peek();
    t.name = name;
    tags.push(t);
    return t;
  }

  Tag pop() {
    var t = tags.pop();
    if (t.end != null) t.end.run();
    return t;
  }

  String label() {
    var t = tags.peek();
    if (t == null) return null;
    t = t.up;
    if (t == null || t.isNode) return null;
    return t.name;
  }

  void property(Tag tag, StartElement e) {
    if (e.getAttributes().hasNext()) {
      gen.bp.log("property "+tag.name+" has unused attributes");
    }
    // TODO:
    // 1. handle <children> -> call gen.newList(); set tag.end = gen::listEnd
    // 2. mark as 'isList' for contained elements
  }

  void bean(Tag tag, StartElement e) { // startVar()
    var attrs = attributes(e);
    var args = arguments(attrs,tag.name);
    gen.newVar(label(),tag.name);
    parameters(attrs,args);
    gen.argEnd();
    gen.varBody(null);
    if (args.length < attrs.length) {
      properties(attrs);
    }
    tag.end = (tag.up != null && tag.up.isNode) ? this::addItem : gen::bodyEnd;
  }

  void addItem() { gen.bodyEnd(); gen.itemEnd(); }

  void parameters(Attr[] attrs, String...args) {
    for (var i = 0; i < args.length; i++) {
      var arg = args[i];
      for (var j = 0; j < attrs.length; j++) {
        var a = attrs[j];
        if (a != null && arg.equals(a.name)) {
          gen.varArg(a.value);
          attrs[j] = null; // remove from list
          break; // inner loop
        }
      }
    }
  }

  void properties(Attr[] attrs) {
    // feed remaining attribute values
    for (var a:attrs) {
      if (a != null) {
        gen.varProperty(a.name, a.value);
      }
    }
  }

  class Attr { String name; String value; }

  Attr[] attributes(StartElement s) {
    var list = new FIFO<Attr>();
    var i = s.getAttributes();
    while (i.hasNext()) {
      var a = i.next();
      var b = new Attr();
      b.name = name(a.getName());
      b.value = a.getValue();
      list.add(b);
    }
    var x = 0;
    var y = new Attr[list.size()];
    for (var a:list) y[x++] = a;
    return y;
  }

  String[] arguments(Attr[] attrs, String tag) {
    // find a matching signature for the <Type>
    var args = gen.signatures(tag);
    if (args.length > 0) {
      var i = match(attrs,args);
      if (i > -1) return args[i];
    }
    return new String[0];
  }

  int match(Attr[] attrs, String[][] args) {
    SIG: for (var i = 0; i < args.length; i++) {
      var sig = args[i];
      ARG: for (var j = 0; j < sig.length; j++) {
        var arg = sig[j];
        for (var a:attrs) {
          if (a.name.equals(arg)) continue ARG; // found an arg in attrs
        }
        continue SIG; // arg not in attrs; try next sig
      }
      return i; // all arg's in attr's; use this sig
    }
    return -1; // no matching constructor signature
  }

  boolean isSpecial(Tag tag, StartElement s) {
    if ("URL".equals(tag.name)) {
      urlVar(tag,s);
    } else if ("fx.include".equals(tag.name)) {
      includeVar(tag,s);
    } else {
      return false;
    }
    return true;
  }

  void urlVar(Tag tag, StartElement s) { // <URL value="@resource" />
    gen.newVar(label(),"URL");
    var v = s.getAttributeByName(new QName("value"));
    if (v != null) gen.varArg(v.getValue());
    gen.argEnd();
  }

  void includeVar(Tag tag, StartElement s) { // <fx.include source="@ref" ... />
    var attrs = attributes(s);
    var label = label();
    gen.newVar(label,"fx.include");
    parameters(attrs,"source");
    gen.argEnd();
    if (attrs.length > 0) {
      gen.varBody(null);
      properties(attrs);
      gen.bodyEnd();
    }
    if (label == null) {
      tag.end = gen::itemEnd;
    }
  }

  @SuppressWarnings("unchecked")
  static <T extends Throwable,V> V uncheck(Throwable t) throws T { throw (T)t; }
}
