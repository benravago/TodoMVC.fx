package fx.mvc.util;

import java.nio.file.Paths;
import java.util.function.Function;

import javafx.scene.Node;
import javafx.event.Event;
import javafx.util.Pair;

import fx.mvc.View;
import fx.mvc.Controller;
import fx.mvc.OnLoad;
import static fx.mvc.util.Events.*;
import static fx.mvc.util.Functions.*;
import static fx.mvc.util.Reflections.*;

public final class Views {
  private Views() {}

  public static Node include(Class<?> ref, String view) { // assumes no suffix
    var pkg = ref.getPackageName().replace('.', '/'); // make package path
    var path = Paths.get(pkg).resolve(view).normalize(); // combine package and source
    var fqcn = path.toString().replace('/', '.'); // convert back to fqcn format
    var pair = loadController(fqcn);
    return pair != null ? (Node) pair.getValue() : null;
  }

  public static <C, V> Pair<C, V> loadController(String className) {
    return loadController(defined(className));
  }

  public static <C, V> Pair<C, V> loadController(Object obj) {
    return obj == null ? null
      : getController(obj.getClass(),
          type -> type.isInstance(obj) ? obj : Load.instance(type, obj));
  }

  public static <C, V> Pair<C, V> loadController(Class<?> type) {
    return type != null ? getController(type, Load::instance) : null;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  static <C, V> Pair<C, V> getController(Class<?> type, Function<Class<?>, Object> ctor) {
    var pair = controllerTypes(type);
    if (pair != null) {
      var controller = ctor.apply(pair.getKey());
      if (controller != null) {
        var view = bind(controller, pair.getValue());
        return new Pair(controller, view);
      }
    }
    return null;
  }

  static Pair<Class<?>, Class<?>> controllerTypes(Class<?> type) {
    if (type == null) return null;
    var controller = type.getAnnotation(Controller.class);
    if (controller != null) { // type is a 'View' or 'Model'
      type = defined(controller.value());
      if (type == null) return null;
    }
    var view = type.getAnnotation(View.class);
    if (view != null) { // type is not a 'Controller'
      var root = defined(view.value());
      if (root != null) {
        return new Pair<>(type, root);
      }
    }
    return null;
  }

  public static <V> V loadView(Object controller) {
    var viewName = viewName(controller);
    return viewName.isBlank() ? null : bind(controller, defined(viewName));
  }

  static String viewName(Object controller) {
    if (controller != null) {
      var view = controller.getClass().getAnnotation(View.class);
      if (view != null) {
        return nonNull(view.value());
      } else {
        var n = controller.getClass().getName();
        if (n.endsWith("Controller")) {
          return n.substring(0, n.length() - 10) + "View";
        }
      }
    }
    return "";
  }

  @SuppressWarnings("unchecked")
  static <T> T bind(Object controller, Class<?> view) {
    var tag = controller.getClass().getAnnotation(View.class);
    var nodeClass = defined(tag.nodeType());
    var root = callStatic(view, "view", nodeClass, controller);
    sendEvent(controller, OnLoad.class, new Event(root, null, Event.ANY));
    return (T) root;
  }

  static String nonNull(String s) {
    return s != null ? s : "";
  }

}
