package fx.json;

import static java.lang.Character.isJavaIdentifierStart;

public class JbmlParser extends JslParser<JbmlListener> {

  @Override
  void prologue() {
    directive("package");
    while (directive("import")) {}
    beanType(); // parse bean type
  }

  void beanType() {
    var c = nonWhitespace();
    if (isJavaIdentifierStart(c) || isIdentifierPart(c)) {
      special();
      handler.entityStart(null, name);
      c = nonWhitespace();
      if (c == '(') {
        parameters();
      } else {
        position--;
        name = null;
      }
      handler.entityEnd();
      return;
    }
    throw new ParseException("missing bean type" + location());
  }

  boolean directive(String action) {
    var mark = position;
    if (phrase() && name.equals(action)) {
      if (phrase()) {
        handler.directive(action,name);
        return true;
      }
    }
    position = mark; // restore position
    return false;
  }

  boolean phrase() {
    var c = nonWhitespace();
    if (isJavaIdentifierStart(c) || isIdentifierPart(c)) {
      special();
      name = new String(buf, offset, (position - offset));
      return true;
    }
    return false;
  }

  @Override
  boolean identifier(int c) {
    if (c == '(' && name != null) {
      entity(null);
      return true;
    }
    return super.identifier(c);
  }

}