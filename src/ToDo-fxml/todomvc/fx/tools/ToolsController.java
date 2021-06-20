package todomvc.fx.tools;

import javafx.fxml.FXML;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleListProperty;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;

import todomvc.fx.data.Repository;

public class ToolsController {

  @FXML
  Label itemsLeftLabel;

  @FXML
  ToggleGroup stateGroup;

  final Repository repository;

  public ToolsController() {
    this.repository = Repository.getInstance();
  }

  public void initialize() {
    var openItemsProperty =
      new SimpleListProperty<>(repository.openItemsProperty());

    itemsLeftLabel.textProperty().bind(
      Bindings.createStringBinding(() -> {
        var size = openItemsProperty.getSize();
        var itemsText = size == 1 ? "item" : "items";
        return size + " " + itemsText + " left";
      },
      openItemsProperty.sizeProperty())
    );
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
