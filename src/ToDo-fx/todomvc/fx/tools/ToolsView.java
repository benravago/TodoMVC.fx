package todomvc.fx.tools;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

abstract class ToolsView {

  HBox root;

  Label itemsLeftLabel;

  ToggleGroup stateGroup;

  ToggleButton showAll;
  ToggleButton showActive;
  ToggleButton showCompleted;

  abstract void all(ActionEvent e);
  abstract void active(ActionEvent e);
  abstract void completed(ActionEvent e);

  ToolsView() {

    root = new HBox();
    root.setSpacing(20.0);
    root.setAlignment(Pos.CENTER);

    itemsLeftLabel = new Label();
    itemsLeftLabel.setId("itemsLeftLabel");
    itemsLeftLabel.setText("X items left");

    var hbox = new HBox();
    hbox.setSpacing(10.0);

    stateGroup = new ToggleGroup();

    showAll = new ToggleButton();
    showAll.setId("showAll");
    showAll.setOnAction(this::all);
    showAll.setToggleGroup(stateGroup);
    showAll.setText("All");
    showAll.setMnemonicParsing(false);
    showAll.setSelected(true);

    showActive = new ToggleButton();
    showActive.setId("showActive");
    showActive.setOnAction(this::active);
    showActive.setToggleGroup(stateGroup);
    showActive.setText("Active");
    showActive.setMnemonicParsing(false);

    showCompleted = new ToggleButton();
    showCompleted.setId("showCompleted");
    showCompleted.setOnAction(this::completed);
    showCompleted.setToggleGroup(stateGroup);
    showCompleted.setText("Completed");
    showCompleted.setMnemonicParsing(false);

    hbox.getChildren().addAll(showAll, showActive, showCompleted);
    hbox.setPadding(new Insets(5.0, 5.0, 5.0, 5.0));

    root.getChildren().addAll(itemsLeftLabel, hbox);
    root.setPadding(new Insets(5.0, 5.0, 5.0, 5.0));
  }

}
