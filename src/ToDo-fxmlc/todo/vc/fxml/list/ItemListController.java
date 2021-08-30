package todo.vc.fxml.list;

import fx.mvc.OnLoad;
import fx.mvc.View;
import fx.mvc.util.Views;
import static fx.mvc.util.Lookup.*;

import java.util.HashMap;
import java.util.Map;

import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import todo.vc.fxml.Repository;
import todo.vc.fxml.TodoItem;

@View("todo.vc.fxml.list.ItemList")
class ItemListController {

  ListView<TodoItem> items;

  final Map<TodoItem, Node> itemNodeCache = new HashMap<>();
  final Repository repository;

  ItemListController() {
    repository = Repository.getInstance();
  }

  @OnLoad
  void initialize(Event e) {
    items = $(e).get("items");

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
    var itemController = new ItemController(item, repository);
    Parent itemView = Views.forController(itemController);
    return itemView;
  }

}
