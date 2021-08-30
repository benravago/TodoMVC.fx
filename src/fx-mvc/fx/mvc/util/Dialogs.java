package fx.mvc.util;

import java.util.Optional;

import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

import fx.mvc.dialog.OnShowing;
import fx.mvc.dialog.OnShown;
import static fx.mvc.util.Events.*;

public final class Dialogs {
  private Dialogs() {}

  public static <T> Optional<T> open(Object controller) {
    Node view = Views.forController(controller);
    var dialog = new Dialog<T>();
    dialog.setResizable(true);
    setEventHandler(dialog, controller, OnShowing.class);
    setEventHandler(dialog, controller, OnShown.class);
    var pane = dialog.getDialogPane();
    pane.setStyle("-fx-focus-color:lightgray; -fx-faint-focus-color:transparent;");
    pane.getButtonTypes().add(ButtonType.OK);
    pane.setContent(view);
    return dialog.showAndWait();
  }

}
