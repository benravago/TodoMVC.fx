package todomvc.fx;

import javafx.fxml.FXMLLoader;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

  public static void main(String... args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) throws Exception {

    var pathToMainFxml = "app/Main.fxml";

    var mainFxmlUrl = getClass().getResource(pathToMainFxml);
    if (mainFxmlUrl == null) {
      throw new IllegalStateException("Can't find Main.fxml file with path: " + pathToMainFxml);
    }

    var fxmlLoader = new FXMLLoader(mainFxmlUrl);
    fxmlLoader.load();

    Parent root = fxmlLoader.getRoot();
    stage.setScene(new Scene(root));
    stage.show();
  }
}
