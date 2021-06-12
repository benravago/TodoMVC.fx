package fx.node.builder;

import java.io.CharArrayReader;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartElement;
import static javax.xml.stream.XMLStreamConstants.*;

import static fx.node.builder.Beans.*;

import fx.util.FIFO;
import fx.util.LIFO;
import static fx.util.XML.*;

class FxmlDriver {

  static XMLInputFactory factory = XMLInputFactory.newDefaultFactory();

  NodeBuilder gen;

  String transform(String name, char[] input) {
    transform(input, new NodeBuilder());
    return gen.view(name);
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

  int depth; // <Type> nesting depth
  LIFO<String> defer = new LIFO<>(); // stack of <property>

  void startElement(StartElement s) {
    if (specialElement(s)) return;
    var ident = name(s.getName());
    var attr = attributes(s);
    if (isProper(ident)) { // <Type ...>
      gen.newVar(defer.peek(),ident);
      var used = parameters(ident,attr);
      gen.argEnd();
      gen.varBody(null);
      depth++;
      if (used < attr.length) {
        properties(attr);
      }
    } else {
      if (attr.length == 0) {
        defer.push(ident); // <property>
      } else {
        // not expected: how to handle <property attribute="value">
      }
    }
  }

  int parameters(String ident, Attr[] attrs) {
    var args = match(ident,attrs);
    // feed attribute values in signature args order
    for (var arg:args) {
      for (var j = 0; j < attrs.length; j++) {
        var a = attrs[j];
        if (a != null && a.name.equals(arg)) {
          gen.varArg(a.value);
          attrs[j] = null; // remove from list
          break; // inner loop
        }
      }
    }
    return args.length;
  }

  void properties(Attr[] attrs) {
    for (var a:attrs) {
      if (a != null) {
        gen.setProperty(a.name, a.value);
      }
    }
  }

  void endElement(EndElement e) {
    var ident = name(e.getName());
    if (isProper(ident)) { // </Type>
      depth--;
      gen.bodyEnd();
      if (depth > 0 && defer.isEmpty()) {
        gen.addItem(); // free-standing <Type ...>...</Type>
      }
    } else {
      defer.pop(); // </property>
    }
  }

  void characters(Characters e) {
    var c = e.getData();
    if (!c.isBlank()) {
      System.out.printf("- characters: %s\n", c);
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

  String[] match(String ident, Attr[] attrs) {
    // find a matching signature for the <Type>
    var b = gen.bp.get(ident);
    var args = gen.bp.signatures(b);
    if (args.length > 0) {
      var i = match(args,attrs);
      if (i > -1) return args[i];
    }
    return new String[0];
  }

  int match(String[][] args, Attr[] attrs) {
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

  boolean specialElement(StartElement s) {
    if (s.getName().getLocalPart().equals("URL")) {
      // <URL value="@resource" />
      gen.newVar(defer.peek(),"URL");
      var v = s.getAttributeByName(new QName("value"));
      if (v != null) gen.varArg(v.getValue());
      gen.argEnd();
      return true;
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  static <T extends Throwable,V> V uncheck(Throwable t) throws T { throw (T)t; }
}
