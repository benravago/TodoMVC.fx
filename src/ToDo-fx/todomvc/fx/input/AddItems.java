package todomvc.fx.input;

import javafx.collections.ListChangeListener;
import javafx.scene.Parent;
import todomvc.fx.data.Repository;
import todomvc.fx.data.TodoItem;

public class AddItems extends AddItemsView {

  final Repository repository;

  public AddItems() {
    super();

    this.repository = Repository.getInstance();

    addInput.setOnAction(event -> {
      var currentText = addInput.getText();

      // Check input
      if (currentText == null || currentText.trim().isEmpty()) {
        return;
      }

      // Create and add item
      var newItem = new TodoItem(currentText);
      repository.addItem(newItem);

      // Reset input
      addInput.setText("");
    });

    selectAll.setOnAction(event -> {
      var selected = selectAll.isSelected();

      repository.allItemsProperty().forEach(item -> {
        item.setDone(selected);
      });

      // While iterating through the items and setting them to done,
      // the selectAll-checkbox can switch it's state based on other constraints.
      // Therefore at the end the checkbox has to be set to the value again.
      selectAll.setSelected(selected);
    });

    repository.allItemsProperty().addListener((ListChangeListener<TodoItem>) c -> {
      c.next();

      selectAll.setVisible(!repository.allItemsProperty().isEmpty());

      // If the checkbox is marked...
      if (selectAll.isSelected()) {

        // We check if there is any item that is not "done" now.
        // If this is the case, we uncheck the checkbox
        selectAll.setSelected(!repository.allItemsProperty().stream().anyMatch(item -> !item.isDone()));
      } else {

        // If the checkbox is not marked yet
        // we check if there all items are done now.
        // If this is the case, we mark the checkbox.
        selectAll.setSelected(repository.allItemsProperty().stream().allMatch(item -> item.isDone()));
      }
    });

  }

  public Parent root() {
    return root;
  }
}
