package fx.json;

public interface JsonListener {

  default void objectStart(String name) {} // -> name:{
  default void objectEnd() {} // -> }

  default void arrayStart(String name) {} // -> name:[
  default void arrayEnd() {} // -> ]

  default void stringValue(String name, String value) {} // -> name: ""
  default void numberValue(String name, Number value) {} // -> name: -01.2e+3
  default void booleanValue(String name, Boolean value) {} // -> name: true|false
  default void nullValue(String name) {} // -> name: null

  default void error(ParseException e) { throw e; }

}
