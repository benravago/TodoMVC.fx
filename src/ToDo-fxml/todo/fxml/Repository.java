package todo.fxml;

import javafx.beans.Observable;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

public class Repository {

  private Repository() {}

  private static class Singleton {
    static final Repository INSTANCE = new Repository();
  }

  public static Repository getInstance() {
    return Singleton.INSTANCE;
  }

  private final ObservableList<TodoItem> allItems =
    FXCollections.observableArrayList(item -> new Observable[] { item.doneProperty() });

  private final FilteredList<TodoItem> completedItems =
    new FilteredList<>(allItems, TodoItem::isDone);

  private final FilteredList<TodoItem> openItems =
    new FilteredList<>(allItems, (item) -> !item.isDone());

  private final ListProperty<TodoItem> itemsProperty =
    new SimpleListProperty<>(allItems);

  public ObservableList<TodoItem> itemsProperty() {
    return itemsProperty;
  }

  public ObservableList<TodoItem> allItemsProperty() {
    return allItems;
  }

  public ObservableList<TodoItem> completedItemsProperty() {
    return completedItems;
  }

  public ObservableList<TodoItem> openItemsProperty() {
    return openItems;
  }

  public void showAllItems() {
    itemsProperty.set(allItems);
  }

  public void showCompletedItems() {
    itemsProperty.set(completedItems);
  }

  public void showOpenItems() {
    itemsProperty.set(openItems);
  }

  public void addItem(TodoItem item) {
    allItems.add(item);
  }

  public void deleteItem(TodoItem item) {
    allItems.remove(item);
  }

}
