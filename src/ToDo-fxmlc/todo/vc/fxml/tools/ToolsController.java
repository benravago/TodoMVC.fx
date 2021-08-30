package todo.vc.fxml.tools;

import fx.mvc.OnLoad;
import fx.mvc.View;
import static fx.mvc.util.Lookup.*;

import javafx.event.Event;
import javafx.event.ActionEvent;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleListProperty;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;

import todo.vc.fxml.Repository;

@View("todo.vc.fxml.tools.Tools")
class ToolsController {

  Label itemsLeftLabel;
  ToggleGroup stateGroup;

  final Repository repository;

  ToolsController() {
    repository = Repository.getInstance();
  }

  @OnLoad
  void initialize(Event e) {
    itemsLeftLabel = $(e).get("itemsLeftLabel");

    var openItemsProperty = new SimpleListProperty<>(repository.openItemsProperty());

    itemsLeftLabel.textProperty().bind(Bindings.createStringBinding(() -> {
      var size = openItemsProperty.getSize();
      var itemsText = size == 1 ? "item" : "items";
      return size + " " + itemsText + " left";
    }, openItemsProperty.sizeProperty()));
  }

  void all(ActionEvent e) {
    repository.showAllItems();
  }

  void active(ActionEvent e) {
    repository.showOpenItems();
  }

  void completed(ActionEvent e) {
    repository.showCompletedItems();
  }

}
