package todomvc.fx.list;

import fx.mvc.OnLoad;
import fx.mvc.View;
import fx.mvc.util.Views;
import static fx.mvc.util.Lookup.*;

import java.net.URL;

import java.util.HashMap;
import java.util.Map;

import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import todomvc.fx.Repository;
import todomvc.fx.TodoItem;

@View("todomvc.fx.list.ItemList")
class ItemListController {

    ListView<TodoItem> items;

    final Map<TodoItem, Node> itemNodeCache = new HashMap<>();

    final Repository repository;

    ItemListController() {
        this.repository = Repository.getInstance();
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
        var itemController = new ItemController(item,repository);
        Parent itemView = Views.loadView(itemController);
        return itemView;
    }

}
