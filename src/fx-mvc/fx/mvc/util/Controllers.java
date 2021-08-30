package fx.mvc.util;

import javafx.util.Pair;

import fx.inject.Instances;
import static fx.mvc.util.Reflections.*;

import java.util.NoSuchElementException;

public final class Controllers {
  private Controllers() {}

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static <C, V> Pair<C, V> forName(String controllerName) {
    var controller = defined(controllerName);
    if (controller != null) {    
      var main = Instances.newInstance(controller);
      var viewName = viewFor(controller,main);
      if (!viewName.isEmpty()) {
        var view = defined(viewName);
        if (view != null) {
          var root = Views.bind(main,view);
          return new Pair(main,root);
        }
      }
      throw new NoSuchElementException("no view for "+controllerName);
    }
    throw new NoSuchElementException("no controller "+controllerName);
  }

  static String viewFor(Class<?> type, Object obj) {
    var name = Views.viewName(type);
    if (name.isEmpty()) {
      var intf = obj.getClass();
      if (!type.equals(intf)) {
        name = Views.viewName(intf);
      }
    }
    return name;
  }

}
