package fx.json;

import static java.lang.Character.*;

public class JslParser<T extends JslListener> extends JsonParser<T>{

  @Override
  boolean specialName(int c) {
    if (isJavaIdentifierStart(c) || isIdentifierPart(c)) {
      special();
      length = position - offset;
      if (nonWhitespace() == ':') {
        name = new String(buf,offset,length);
        return true;
      }
    }
    return false;
  }

  @Override
  boolean specialValue(int c) {
    return identifier(c) || block(c);
  }

  boolean identifier(int c) {
    if (isJavaIdentifierStart(c) || isIdentifierPart(c)) {
      special();
      var ident = new String(buf,offset,(position-offset));
      entity(ident);
      return true;
    }
    return false;
  }

  void entity(String ident) {
    var c = nonWhitespace();
    if (c == '(') {
      handler.entityStart(name,ident);
      parameters();
      handler.entityEnd();
      c = nonWhitespace();
    } else {
      if (c == '{') {
        handler.entityStart(name,ident);
        handler.entityEnd();
      } else {
        handler.stringValue(name,ident);
      }
    }
    if (c == '{') {
      name = null;
      object();
    } else {
      position--;
    }
  }

  void parameters() {
    if (!empty(')')) {
      for (;;) {
        tag();
        value();
        var c = nonWhitespace();
        if (c == ',') continue;
        if (c == ')') break;
        throw new ParseException(invalid());
      }
      name = null;
    }
  }

  // TODO: accept "-quoted names

  void tag() {
    name = null;
    var mark = position;
    var c = nonWhitespace();
    if (specialName(c)) return;
    position = mark;
  }

  void special() {
    int c;
    offset = position - 1;
    do { c = buf[position++]; }
    while (isJavaIdentifierPart(c) || isIdentifierPart(c));
    position--;
  }

  boolean isIdentifierPart(int c) {
    return 0 < c && c < 128 && SYM[c] == 1;
  }

  final static byte[] SYM = new byte[128];
  static {  // excludes ' ( ) + , - : ; [ ] ` { }
    char[] c = {'!','#','$','%','&','*','.','/','<','=','>','?','@','^','_','|','~'};
    for (var i:c) SYM[i] = 1;
  }

  static final int K = '`';

  boolean backticks(int c) {
    return K == c && position < limit - 2 && K == buf[position+0] && K == buf[position+1];
  }

  boolean block(int c) {
    if (backticks(c)) {
      position += 2;
      offset = position;
      while (c != EOF) {
        c = buf[position++];
        if (backticks(c)) {
          length = (position - 1) - offset;
          handler.stringValue(name, new String(buf, offset, length));
          position += 2;
          return true;
        }
      }
      throw new ParseException("non-terminated block" + location());
    }
    return false;
  }

}
