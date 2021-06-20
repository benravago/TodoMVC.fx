package fx.json;

public interface JbmlListener extends JslListener {

  // package name
  // import package.*
  // import package.class

  default void directive(String action, String data) {}

}
