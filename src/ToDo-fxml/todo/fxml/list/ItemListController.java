package todo.fxml.list;

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

import todo.fxml.Repository;
import todo.fxml.TodoItem;

public class ItemListController {

  @FXML
  public ListView<TodoItem> items;

  final Map<TodoItem, Node> itemNodeCache = new HashMap<>();
  final Repository repository;

  final URL itemFxmlUrl;

  public ItemListController() {
    repository = Repository.getInstance();
    itemFxmlUrl = findItemFxml("Item.fxml");
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
            var parent = loadItemFxml(item,itemFxmlUrl);
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

  URL findItemFxml(String fxmlPath) {
    var fxmlUrl = getClass().getResource(fxmlPath);
    if (fxmlUrl == null) {
      throw new IllegalArgumentException("Can't find FXML at " + fxmlPath);
    }
    return fxmlUrl;
  }
  
  Parent loadItemFxml(TodoItem item, URL fxmlUrl) {
    try {
      var fxmlLoader = new FXMLLoader(fxmlUrl);
      var itemController = new ItemController(item, repository);

      fxmlLoader.setController(itemController);
      fxmlLoader.load();

      return fxmlLoader.getRoot();
    }
    catch (IOException e) { throw new UncheckedIOException(e); }
  }

}
