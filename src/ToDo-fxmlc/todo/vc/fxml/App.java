package todo.vc.fxml;

import fx.mvc.runtime.Runtime;

public class App {

  public static void main(String... args) throws Throwable {
    var name = "todo.vc.fxml.app.MainController";
    var rc = Runtime.getRuntime().launch(name);
    System.out.println("app " + name + " started");
    var e = rc.get();
    if (e != null) {
      throw e;
    }
    System.out.println("" + App.class + " done");
  }
}
