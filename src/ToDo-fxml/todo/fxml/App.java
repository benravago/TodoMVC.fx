package todo.fxml;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class App extends Application {

  public static void main(String... args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) throws Exception {
    stage.setTitle("ToDo-fxml");
    var root = getRoot("app/Main.fxml");
    stage.setScene(new Scene(root));
    stage.show();
  }

  Parent getRoot(String fxmlPath) throws Exception {
    var fxmlUrl = getClass().getResource(fxmlPath);
    if (fxmlUrl == null) {
      throw new IllegalArgumentException("Can't find FXML file at " + fxmlPath);
    }

    var fxmlLoader = new FXMLLoader(fxmlUrl);
    fxmlLoader.load();

    return (Parent) fxmlLoader.getRoot();
  }
}
