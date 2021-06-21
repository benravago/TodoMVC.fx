package todomvc.fx.list;

import javafx.event.ActionEvent;
import todomvc.fx.data.Repository;
import todomvc.fx.data.TodoItem;

class Item extends ItemView {

  final TodoItem todoItem;
  final Repository repository;

  Item(TodoItem item, Repository repository) {
    super();

    this.todoItem = item;
    this.repository = repository;

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

  @Override
  void delete(ActionEvent e) {
    repository.deleteItem(todoItem);
  }

}
