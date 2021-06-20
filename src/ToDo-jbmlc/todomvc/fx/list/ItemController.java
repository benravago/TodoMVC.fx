package todomvc.fx.list;

import fx.mvc.OnLoad;
import fx.mvc.View;
import static fx.mvc.util.Lookup.*;

import javafx.event.Event;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import todomvc.fx.data.Repository;
import todomvc.fx.data.TodoItem;

@View("todomvc.fx.list.Item")
class ItemController {

  CheckBox completed;
  HBox contentBox;
  Label contentLabel;
  Button deleteButton;
  TextField contentInput;
  HBox root;

  final TodoItem todoItem;
  final Repository repository;

  ItemController(TodoItem item, Repository repository) {
    this.todoItem = item;
    this.repository = repository;
  }

  @OnLoad
  void initialize(Event e) {
    var lookup = $(e);
    root = lookup.root();
    completed = lookup.get("completed");
    contentBox = lookup.get("contentBox");
    contentLabel = lookup.get("contentLabel");
    deleteButton = lookup.get("deleteButton");
    contentInput = lookup.get("contentInput");

    completed.selectedProperty().bindBidirectional(todoItem.doneProperty());

    contentLabel.textProperty().bindBidirectional(todoItem.textProperty());
    contentInput.textProperty().bindBidirectional(todoItem.textProperty());

    contentBox.setOnMouseClicked(event -> {
      if (event.getClickCount() > 1) {
        enableEditMode();
      }
    });

    contentInput.setOnAction(event -> disableEditMode());

    contentInput.focusedProperty().addListener((obs, oldV, newV) -> {
      if (!newV) {
        disableEditMode();
      }
    });

    deleteButton.visibleProperty().bind(root.hoverProperty());
  }

  void disableEditMode() {
    contentInput.setVisible(false);
    contentBox.setVisible(true);
  }

  void enableEditMode() {
    contentBox.setVisible(false);
    contentInput.setVisible(true);
    contentInput.requestFocus();
  }

  void delete(ActionEvent e) {
    repository.deleteItem(todoItem);
  }

}
