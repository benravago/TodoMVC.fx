package todomvc.fx.input;

import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

class AddItemsView {

  HBox root;

  TextField addInput;
  CheckBox selectAll;

  AddItemsView() {

    root = new HBox();
    root.setAlignment(Pos.CENTER_LEFT);
    root.getStyleClass().add("add_item_root");

    selectAll = new CheckBox();
    selectAll.setId("selectAll");
    selectAll.setVisible(false);

    addInput = new TextField();
    addInput.setId("addInput");
    addInput.setPromptText("What needs to be done?");

    HBox.setHgrow(addInput, Priority.ALWAYS);

    root.getChildren().addAll(selectAll, addInput);

    root.getStylesheets()
        .add(getClass().getResource("additems.css").toString());
  }

}
