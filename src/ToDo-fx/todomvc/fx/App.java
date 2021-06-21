package todomvc.fx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import todomvc.fx.app.Main;

public class App extends Application {

  public static void main(String... args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) {
    stage.setTitle("ToDo-fx");
    var root = new Main().root();
    stage.setScene(new Scene(root));
    stage.show();
  }
}

