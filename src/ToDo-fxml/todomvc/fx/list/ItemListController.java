package todomvc.fx.list;

import java.io.IOException;
import java.io.UncheckedIOException;

import java.net.URL;

import java.util.HashMap;
import java.util.Map;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import todomvc.fx.data.Repository;
import todomvc.fx.data.TodoItem;

public class ItemListController {

  @FXML
  ListView<TodoItem> items;

  final Map<TodoItem, Node> itemNodeCache = new HashMap<>();

  final URL itemFxmlUrl;

  final Repository repository;

  public ItemListController() {
    this.repository = Repository.getInstance();

    var pathToItemFxml = "Item.fxml";

    itemFxmlUrl = getClass().getResource(pathToItemFxml);

    if (itemFxmlUrl == null) {
      throw new IllegalStateException("Can't find Item.fxml file with path: " + pathToItemFxml);
    }
  }

  public void initialize() {

    items.setItems(repository.itemsProperty());

    items.setCellFactory(value -> new ListCell<TodoItem>() {
      @Override
      protected void updateItem(TodoItem item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
          setText(null);
          setGraphic(null);
        } else {
          setText(null);

          if (!itemNodeCache.containsKey(item)) {
            var parent = loadItemView(item);
            itemNodeCache.put(item, parent);
          }

          var node = itemNodeCache.get(item);

          var currentNode = getGraphic();
          if (currentNode == null || !currentNode.equals(node)) {
            setGraphic(node);
          }
        }
      }
    });

  }

  Parent loadItemView(TodoItem item) {
    var fxmlLoader = new FXMLLoader(itemFxmlUrl);
    try {
      var itemController = new ItemController(item, repository);
      fxmlLoader.setController(itemController);
      fxmlLoader.load();
      return fxmlLoader.getRoot();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

}
