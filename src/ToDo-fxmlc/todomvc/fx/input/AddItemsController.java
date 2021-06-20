package todomvc.fx.input;

import fx.mvc.OnLoad;
import fx.mvc.View;
import static fx.mvc.util.Lookup.*;

import javafx.event.Event;
import javafx.collections.ListChangeListener;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

import todomvc.fx.Repository;
import todomvc.fx.TodoItem;

@View("todomvc.fx.input.AddItems")
class AddItemsController {

    CheckBox selectAll;
    TextField addInput;

    final Repository repository;

    AddItemsController() {
        this.repository = Repository.getInstance();
    }

    @OnLoad
    void initialize(Event e) {
        var root = $(e);
        selectAll = root.get("selectAll");
        addInput = root.get("addInput");

        addInput.setOnAction(event -> {
            var currentText = addInput.getText();

            // Check input
            if (currentText == null || currentText.trim().isEmpty()) {
                return;
            }

            // Create and add item
            var newItem = new TodoItem(currentText);
            repository.addItem(newItem);

            // Reset input
            addInput.setText("");
        });

        selectAll.setOnAction(event -> {
            var selected = selectAll.isSelected();
            repository.allItemsProperty().forEach(item -> {
                item.setDone(selected);
            });

            // While iterating through the items and setting them to done,
            // the selectAll-checkbox can switch it's state based on other constraints.
            // Therefore at the end the checkbox has to be set to the value again.
            selectAll.setSelected(selected);
        });

        repository.allItemsProperty().addListener((ListChangeListener<TodoItem>) c -> {
            c.next();

            selectAll.setVisible(!repository.allItemsProperty().isEmpty());

            // If the checkbox is marked...
            if (selectAll.isSelected()) {

                // We check if there is any item that is not "done" now.
                // If this is the case, we uncheck the checkbox
                selectAll.setSelected(!repository.allItemsProperty().stream().anyMatch(item -> !item.isDone()));
            } else {

                // If the checkbox is not marked yet
                // we check if there all items are done now.
                // If this is the case, we mark the checkbox.
                selectAll.setSelected(repository.allItemsProperty().stream().allMatch(item -> item.isDone()));
            }
        });

    }

}
