package fx.util;

import java.util.regex.Pattern;
import javax.xml.namespace.QName;

public interface XML {

  static String name(QName q) {
    var p = q.getPrefix();
    var l = q.getLocalPart();
    return p.isBlank() ? l : p+'.'+l;
  }

  static Pattern numeric = Pattern.compile("-?\\d*(\\.\\d*)?");
  static Pattern constant = Pattern.compile("[A-Z_0-9]*");
  static Pattern method = Pattern.compile("#\\p{Alpha}\\w*");
  static Pattern token = Pattern.compile("\\w*\\.\\w*");
  static Pattern bool = Pattern.compile("true|false",Pattern.CASE_INSENSITIVE);

  static String value(String v) {
    if (v == null || v.equalsIgnoreCase("null")) return v;
    var m = numeric.matcher(v);
    if (m.matches()) return v;
    if (m.usePattern(constant).matches()) return v;
    if (m.usePattern(method).matches()) return v;
    if (m.usePattern(token).matches()) return v;
    if (m.usePattern(bool).matches()) return v;
    return '"'+String.valueOf(v)+'"';
  }


}
