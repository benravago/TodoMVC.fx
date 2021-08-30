package todo.fx.list;

import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;

class ItemListView<T> {

  AnchorPane root;

  ListView<T> items;

  ItemListView() {

    root = new AnchorPane();

    var listView = new ListView<T>();
    items = listView;
    listView.setId("items");

    AnchorPane.setTopAnchor(listView, 0.0);
    AnchorPane.setRightAnchor(listView, 0.0);
    AnchorPane.setLeftAnchor(listView, 0.0);
    AnchorPane.setBottomAnchor(listView, 0.0);

    root.getChildren().add(listView);
  }

}
