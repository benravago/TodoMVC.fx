package fx.node.builder;

import fx.json.JbmlListener;
import fx.json.JbmlParser;
import fx.json.ParseInput;

class JbmlDriver implements JbmlListener {

  JbmlParser parser = new JbmlParser();
  NodeBuilder gen;

  String transform(String name, char[] input) {
    transform(input, new NodeBuilder());
    return gen.view(name);
  }

  void transform(char[] input, NodeBuilder target) {
    gen = target;
    var source = new ParseInput(input);
    parser.handler(this).reset(source).parse();
  }

  @Override
  public void directive(String action, String data) {
    switch (action) {
      case "package" -> gen.setPackage(data);
      case "import" -> gen.addImport(data);
    }
  }

  @Override public void entityStart(String name, String ident) { gen.newVar(name,ident); }
  @Override public void entityEnd() { gen.argEnd(); }
  @Override public void objectStart(String name) { gen.varBody(name); }
  @Override public void objectEnd() { gen.bodyEnd(); }
  @Override public void arrayStart(String name) { gen.newList(name); }
  @Override public void arrayEnd() { gen.listEnd(); }

  @Override public void stringValue(String name, String value) { property(name,value); }
  @Override public void numberValue(String name, Number value) { property(name,value); }
  @Override public void booleanValue(String name, Boolean value) { property(name,value); }
  @Override public void nullValue(String name) { property(name,null); }

  void property(String name, Object value) {
    if (name == null) gen.varArg(value);
    else gen.setProperty(name, value);
  }
}
