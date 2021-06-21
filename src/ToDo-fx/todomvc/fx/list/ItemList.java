package todomvc.fx.list;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ListCell;

import todomvc.fx.data.Repository;
import todomvc.fx.data.TodoItem;

public class ItemList extends ItemListView<TodoItem> {

  final Map<TodoItem, Node> itemNodeCache = new HashMap<>();

  final Repository repository;

  public ItemList() {
    super();

    this.repository = Repository.getInstance();

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
            var itemNode = new Item(item, repository);
            itemNodeCache.put(item, itemNode.root);
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

  public Parent root() {
    return root;
  }
}
