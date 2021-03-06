package todomvc.fx.tools;

import fx.mvc.OnLoad;
import fx.mvc.View;
import static fx.mvc.util.Lookup.*;

import javafx.event.Event;
import javafx.event.ActionEvent;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleListProperty;
import javafx.scene.control.Label;

import todomvc.fx.data.Repository;

@View("todomvc.fx.tools.Tools")
class ToolsController {

  Label itemsLeftLabel;

  final Repository repository;

  ToolsController() {
    this.repository = Repository.getInstance();
  }

  @OnLoad
  void initialize(Event e) {
    itemsLeftLabel = $(e).get("itemsLeftLabel");

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
