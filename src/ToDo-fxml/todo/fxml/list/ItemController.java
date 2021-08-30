package todo.fxml.list;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import todo.fxml.Repository;
import todo.fxml.TodoItem;

public class ItemController {

  @FXML
  public CheckBox completed;
  @FXML
  public HBox contentBox;
  @FXML
  public Label contentLabel;
  @FXML
  public Button deleteButton;
  @FXML
  public TextField contentInput;
  @FXML
  public HBox root;

  final TodoItem todoItem;
  final Repository repository;

  public ItemController(TodoItem item, Repository repository) {
    this.todoItem = item;
    this.repository = repository;
  }

  public void initialize() {

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

  public void disableEditMode() {
    contentInput.setVisible(false);
    contentBox.setVisible(true);
  }

  public void enableEditMode() {
    contentBox.setVisible(false);
    contentInput.setVisible(true);
    contentInput.requestFocus();
  }

  public void delete() {
    repository.deleteItem(todoItem);
  }

}
