package fx.mvc.util;

import java.nio.file.Paths;
import java.util.NoSuchElementException;

import javafx.event.Event;
import javafx.scene.Node;
import javafx.util.Pair;

import fx.mvc.View;
import fx.mvc.Controller;
import fx.mvc.OnLoad;
import fx.inject.Instances;
import static fx.mvc.util.Events.*;
import static fx.mvc.util.Functions.*;
import static fx.mvc.util.Reflections.*;

public final class Views {
  private Views() {}

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static <C, V> Pair<C, V> forName(String viewName) {
    var view = defined(viewName);
    if (view != null) {
      var controller = controllerFor(view);
      if (controller != null) {
        var main = Instances.newInstance(controller);
        var root = bind(main,view);
        System.out.println("views.forName "+main+':'+root);
        return new Pair(main,root);
      }
      throw new NoSuchElementException("no controller for "+viewName);
    }
    throw new NoSuchElementException("no view "+viewName);
  }
  
  static Class<?> controllerFor(Class<?> view) {
    var c = view.getAnnotation(Controller.class);
    return c != null ? defined(c.value()) : null;
  }
  
  public static Node include(Class<?> ref, String view) { // assumes no suffix
    var pkg = ref.getPackageName().replace('.', '/'); // make package path
    var path = Paths.get(pkg).resolve(view).normalize(); // combine package and source
    var fqcn = path.toString().replace('/', '.'); // convert back to fqcn format
    var pair = forName(fqcn);
    return pair != null ? (Node) pair.getValue() : null;
  }

  public static <V> V forController(Object controller) {
    var viewName = viewName(controller);
    return viewName.isEmpty() ? null : bind(controller, defined(viewName));
  }

  static String viewName(Object controller) {
    var ctrl = controller.getClass();
    var name = viewName(ctrl);
    if (name.isEmpty()) {
      for (var intf:ctrl.getInterfaces()) {
        name = viewName(intf);
        if (!name.isEmpty()) break;
      }
    }
    return name;
  }

  static String viewName(Class<?> type) {
    var tag = type.getAnnotation(View.class);
    if (tag != null) {
      var name = tag.value();
      return name.isBlank() ? "" : name;
    }
    var name = type.getName();
    if (name.endsWith("Controller")) {
      return name.substring(0, name.length() - 10) + "View";
    }
    return "";
  }

  @SuppressWarnings("unchecked")
  static <T> T bind(Object controller, Class<?> view) {
    var tag = controller.getClass().getAnnotation(View.class);
    var nodeClass = defined(tag.nodeType()); // usually javafx.scene.Parent
    var root = callStatic(view, "view", nodeClass, controller);
    sendEvent(controller, OnLoad.class, new Event(root, null, Event.ANY));
    return (T) root;
  }

}