package todo.fx.app;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import todo.fx.input.AddItems;
import todo.fx.list.ItemList;
import todo.fx.tools.Tools;

class MainView {

  VBox root;

  MainView() {

    root = new VBox();
    root.setAlignment(Pos.CENTER);

    var label = new Label();
    label.setId("title");
    label.setText("todos");

    var addItems = new AddItems().root();

    var itemList = new ItemList().root();
    VBox.setVgrow(itemList, Priority.ALWAYS);

    var tools = new Tools().root();

    root.getChildren().addAll(label, addItems, itemList, tools);

    root.getStylesheets().add(getClass().getResource("main.css").toString());
  }
}
