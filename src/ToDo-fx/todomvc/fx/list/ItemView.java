package todomvc.fx.list;

import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;

abstract class ItemView {

  HBox root;

  CheckBox completed;
  HBox contentBox;
  Label contentLabel;
  Button deleteButton;
  TextField contentInput;

  abstract void delete(ActionEvent e);

  ItemView() {

    root = new HBox();
    root.setId("root");
    root.getStyleClass().add("item_root");
    root.setAlignment(Pos.CENTER_LEFT);

    completed = new CheckBox();
    completed.setId("completed");

    var stackPane = new StackPane();
    stackPane.setAlignment(Pos.CENTER_LEFT);
    HBox.setHgrow(stackPane, Priority.ALWAYS);

    contentBox = new HBox();
    contentBox.setId("contentBox");
    contentBox.getStyleClass().add("content_box");

    contentLabel = new Label();
    contentLabel.setId("contentLabel");
    contentLabel.setText("Label");
    HBox.setHgrow(contentLabel, Priority.ALWAYS);

    deleteButton = new Button();
    deleteButton.setId("deleteButton");
    deleteButton.setOnAction(this::delete);
    deleteButton.setVisible(false);
    deleteButton.setMnemonicParsing(false);

    var image = new Image(getClass().getResource("close.png").toString());
    deleteButton.setGraphic(new ImageView(image));

    contentBox.getChildren().addAll(contentLabel, deleteButton);

    contentInput = new TextField();
    contentInput.setId("contentInput");
    contentInput.setVisible(false);
    contentInput.setPromptText("What needs to be done?");

    stackPane.getChildren().addAll(contentBox, contentInput);

    root.getChildren().addAll(completed, stackPane);

    root.getStylesheets().add(getClass().getResource("item.css").toString());
  }
}
