package fx.json;

import java.math.BigDecimal;

public class JsonParser<CH extends JsonListener> {

  CH handler;

  char[] buf;
  int position, limit;
  int offset, length;
  int lines, mark;
  String name;

  final static int EOF = -1;

  public JsonParser<CH> handler(CH handler) {
    this.handler = handler;
    return this;
  }

  public JsonParser<CH> reset(ParseInput in) {
    buf = in.getChars();
    limit = buf.length;
    position = 0;
    lines = mark = 0;
    name = null;
    return this;
  }

  public void parse() {
    try {
      prologue();
      var c = nonWhitespace();
      if (c == '{') {
        object();
      } else {
        if (c == '[') {
          array();
        } else {
          if (c != EOF) {
            throw new ParseException(invalid());
          }
        }
      }
    }
    catch (IndexOutOfBoundsException e) {
      handler.error(new ParseException("unexpected end of data", e));
    }
    catch (ParseException e) {
      handler.error(e);
    }
  }

  String invalid() {
    return "invalid character '" + buf[position - 1] + "'" + location();
  }

  String location() {
    return " at " + (lines + 1) + ':' + (position - mark) + " (" + position + ')';
  }

  int nonWhitespace() {
    while (position < limit) {
      var c = buf[position++];
      if (c == ' ' || c == '\r' || c == '\t') continue;
      if (c == '\n') {
        lines += 1;
        mark = position;
        continue;
      }
      return c; // c is at position-1
    }
    return EOF;
  }

  void object() {
    handler.objectStart(name);
    if (!empty('}')) {
      for (;;) {
        name();
        value();
        var c = nonWhitespace();
        if (c == ',') continue;
        if (c == '}') break;
        throw new ParseException(invalid());
      }
      name = null;
    }
    handler.objectEnd();
  }

  void array() {
    handler.arrayStart(name);
    if (!empty(']')) {
      name = null;
      for (;;) {
        value();
        var c = nonWhitespace();
        if (c == ',') continue;
        if (c == ']') break;
        throw new ParseException(invalid());
      }
    }
    handler.arrayEnd();
  }

  void name() {
    name = null;
    var c = nonWhitespace();
    if (c == '"') {
      string();
      if (nonWhitespace() == ':') {
        name = new String(buf, offset, length);
      }
    } else if (!specialName(c)) {
      throw new ParseException("missing name" + location());
    }
  }

  void value() {
    var c = nonWhitespace();
    if (c == '"') {
      string();
      handler.stringValue(name, new String(buf, offset, length));
    } else {
      if (numeric(c)) {
        number();
        handler.numberValue(name, new BigDecimal(buf, offset, (position - offset)));
      } else {
        if (c == '{') {
          object();
        } else {
          if (c == '[') {
            array();
          } else {
            if (c == 't') {
              token("true");
              handler.booleanValue(name, Boolean.TRUE);
            } else {
              if (c == 'f') {
                token("false");
                handler.booleanValue(name, Boolean.FALSE);
              } else {
                if (c == 'n') {
                  token("null");
                  handler.nullValue(name);
                } else {
                  if (!specialValue(c)) {
                    throw new ParseException(invalid());
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  void token(String s) {
    var n = s.length();
    for (var i = 1; i < n; i++) {
      if (s.charAt(i) != buf[position++]) {
        throw new ParseException(invalid());
      }
    }
  }

  void string() {
    offset = position;
    for (;;) {
      var c = buf[position++];
      if (c == '"') {
        length = (position - 1) - offset;
        return;
      }
      if (c == '\b' || c == '\f' || c == '\n' || c == '\r' || c == '\t') break;
      if (c == '\\') {
        c = buf[position++];
        if (c == '"' || c == '\\' || c == '/' || c == 'b' || c == 'f' || c == 'n' || c == 'r' || c == 't' || (c == 'u' && utf16())) continue;
        break;
      }
    }
    throw new ParseException("invalid character" + location());
  }

  boolean utf16() {
    for (var i = 0; i < 4; i++) {
      var c = buf[position++];
      if ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f')) continue;
      return false;
    }
    return true;
  }

  boolean numeric(int c) {
    if ('-' == c) {
      offset = position - 1;
      return true;
    }
    if ('0' <= c && c <= '9') {
      offset = --position;
      return true;
    }
    return false;
  }

  void number() {
    if (!digits()) {
      throw new ParseException(invalid());
    }
    var c = buf[position];
    if (c == '.') {
      position++;
      if (!digits()) {
        throw new ParseException(invalid());
      }
      c = buf[position];
    }
    if (c == 'e' || c == 'E') {
      c = buf[++position];
      if (c == '+' || c == '-') {
        position++;
      }
      if (!digits()) {
        throw new ParseException(invalid());
      }
    }
  }

  boolean digits() {
    var p = position;
    for (;;) {
      var c = buf[position];
      if (c < '0' || '9' < c) break;
      position++;
    }
    return p < position;
  }

  boolean empty(int c) {
    if (c == nonWhitespace()) {
      return true;
    } else {
      position--;
      return false;
    }
  }

  void prologue() {
    // any initial processing
  }

  boolean specialName(int c) {
    return false;
  }

  boolean specialValue(int c) {
    return false;
  }

}
