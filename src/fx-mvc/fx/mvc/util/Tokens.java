package fx.mvc.util;

import java.util.List;
import java.util.LinkedList;

/**
 * A list of supported constants and operators follows:
 *<pre>
 * "string" 'string'   A string constant
 * true false          A boolean constant
 * null                A constant representing the null value
 * 50.0 3e5 42         A numerical constant
 * - (unary operator)  Unary minus operator, applied on a number
 * ! (unary operator)  Unary negation of a boolean
 * + - * / %           Numerical binary operators
 * && ||               Boolean binary operators
 * > >= < <= == !=     Binary operators of comparison.
 *</pre>
 */
public class Tokens {

  List<String> list = new LinkedList<>();
  String s;
  int i, len;
  int begin, end;

  private Tokens(String text) {
    s = text;
    len = s.length();
    i = begin = end = 0;
  }

  void tokenize() {
    while (i < len) {
      skipWhitespace();
      nextToken();
      list.add(s.substring(begin, end));
    }
  }

  void skipWhitespace() {
    while (i < len) {
      if (Character.isWhitespace(s.charAt(i)))
        i++;
      else
        break;
    }
  }

  void nextToken() {
    begin = i;
    while (i < len) {
      var c = s.charAt(i);
      if (Character.isWhitespace(c)) {
        break;
      }
      i++;
      if (c == '"' || c == '\'') {
        quoted(c);
        return;
      }
    }
    end = i;
  }

  void quoted(char d) {
    begin = i;
    while (i < len) {
      var c = s.charAt(i++);
      if (c == d)
        break;
      if (c == '\\')
        i++;
    }
    end = i - 1;
  }

  public static List<String> from(String s) {
    if (s == null || s.isBlank()) {
      return new LinkedList<>();
    }
    var p = new Tokens(s);
    p.tokenize();
    return p.list;
  }

}
