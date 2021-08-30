package todo.fx.app;

import javafx.scene.Parent;

public class Main extends MainView {

  public Main() {
    super();
    System.out.println("Main root " + root);
  }

  public Parent root() {
    return root;
  }
}