package fx.json;

public interface JslListener extends JsonListener {

  // name: identifier(parameters) {}

  default void entityStart(String name, String ident) {} // -> name: ident(
  default void entityEnd() {} // -> )

}