package todo.fxml.tools;

import javafx.fxml.FXML;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleListProperty;
import javafx.scene.control.Label;

import todo.fxml.Repository;

public class ToolsController {

  @FXML
  public Label itemsLeftLabel;

  final Repository repository;

  public ToolsController() {
    repository = Repository.getInstance();
  }

  public void initialize() {
    var openItemsProperty = new SimpleListProperty<>(repository.openItemsProperty());

    itemsLeftLabel.textProperty().bind(Bindings.createStringBinding(() -> {
      var size = openItemsProperty.getSize();
      var itemsText = size == 1 ? "item" : "items";
      return size + " " + itemsText + " left";
    }, openItemsProperty.sizeProperty()));
  }

  public void all() {
    repository.showAllItems();
  }

  public void active() {
    repository.showOpenItems();
  }

  public void completed() {
    repository.showCompletedItems();
  }

}
