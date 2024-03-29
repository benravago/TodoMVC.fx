package fx.mvc.util;

import java.lang.reflect.Method;
import java.lang.annotation.Annotation;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;

import static fx.mvc.util.Functions.*;
import static fx.mvc.util.Reflections.*;

public final class Events {
  private Events() {}

  public static void setEventHandler(Object sender, Object receiver, Class<? extends Annotation> tag) {
    var event = defined(defaultValue(tag, "event"));
    var setOnEvent = "set" + tag.getSimpleName();
    var addHandler = method(sender, setOnEvent, EventHandler.class);
    if (addHandler != null) {
      var handler = handler(receiver, tag, event);
      if (handler != null) {
        var eventHandler = lambda(receiver, handler, EventHandler.class);
        if (eventHandler != null) {
          callVirtual(sender, addHandler, eventHandler);
        }
      }
    }
  }

  public static void addEventHandler(Object sender, Object receiver, Class<? extends Annotation> tag) {
    var event = defined(defaultValue(tag, "event"));
    var eventType = fieldValue(event, defaultValue(tag, "eventType"));
    var addHandler = method(sender, "addEventHandler", EventType.class, EventHandler.class);
    if (addHandler != null) {
      var handler = handler(receiver, tag, event);
      if (handler != null) {
        var eventHandler = lambda(receiver, handler, EventHandler.class);
        if (eventHandler != null) {
          callVirtual(sender, addHandler, eventType, eventHandler);
        }
      }
    }
  }

  public static void sendEvent(Object receiver, Class<? extends Annotation> tag, Event event) {
    var handler = handler(receiver, tag, event.getClass());
    if (handler != null)
      callVirtual(receiver, handler, event);
  }

  static Method handler(Object o, Class<? extends Annotation> a, Class<?> p) {
    var n = defaultValue(a, "method");
    return method(o, m -> (m.getName().equals(n) || m.isAnnotationPresent(a)) && accepts(m, p));
  }

}
