package todo.fx.tools;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleListProperty;
import javafx.event.ActionEvent;
import javafx.scene.Parent;

import todo.fx.Repository;

public class Tools extends ToolsView {

  final Repository repository;

  public Tools() {
    super();

    repository = Repository.getInstance();

    var openItemsProperty = new SimpleListProperty<>(repository.openItemsProperty());

    itemsLeftLabel.textProperty().bind(Bindings.createStringBinding(() -> {
      var size = openItemsProperty.getSize();
      var itemsText = size == 1 ? "item" : "items";
      return size + " " + itemsText + " left";
    }, openItemsProperty.sizeProperty()));
  }

  @Override
  void all(ActionEvent e) {
    repository.showAllItems();
  }

  @Override
  void active(ActionEvent e) {
    repository.showOpenItems();
  }

  @Override
  void completed(ActionEvent e) {
    repository.showCompletedItems();
  }

  public Parent root() {
    return root;
  }
}
