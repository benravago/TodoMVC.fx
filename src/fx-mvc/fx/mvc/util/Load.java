package fx.mvc.util;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import fx.mvc.Factory;
import static fx.mvc.util.Reflections.*;
import static fx.mvc.util.Functions.*;

public final class Load {

  private static final ClassValue<MethodHandle> factory = new ClassValue<>() {
    @Override
    protected MethodHandle computeValue(Class<?> type) {

      var name = defaultValue(Factory.class, "method");
      for (var m : type.getDeclaredMethods()) {
        if ((m.getModifiers() & Modifier.STATIC) != 0) {
          if (m.isAnnotationPresent(Factory.class) || m.getName().equals(name)) {
            return methodHandle(type, m);
          }
        }
      }
      Constructor<?> d = null;
      for (var c : type.getDeclaredConstructors()) {
        if (c.isAnnotationPresent(Factory.class)) {
          return constructorHandle(type, c);
        }
        if (c.getParameterCount() == 0) d = c;
      }
      return d != null ? constructorHandle(type, d) : null;
    }
  };

  static MethodHandle factoryOf(Class<?> type) {
    return factory.get(type);
  }

  // TODO: maybe integrate with Factory annotation?

  @SuppressWarnings("unchecked")
  static <T> T instance(Class<T> type, Object... args) {
    var ctor = constructor(type, args);
    if (ctor != null) {
      try {
        return (T) constructorHandle(type, ctor).invokeWithArguments(args);
      } catch (Throwable e) {
        uncheck(e);
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public static <T> T instance(String className) {
    return (T) instance(defined(className));
  }

  public static <T> T instance(Class<T> type) {
    if (type != null) {
      verifyFactory(type);
      return invokeFactory(type);
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  static <T> T invokeFactory(Class<T> type) {
    var method = factory.get(type);
    if (method == null) return null;
    var params = method.type().parameterArray();
    var args = new Object[params.length];
    for (var i = 0; i < args.length; i++) {
      args[i] = invokeFactory(params[i]);
    }
    try {
      return (T) method.invokeWithArguments(args);
    } catch (Throwable e) {
      return uncheck(e);
    }
  }

  static void verifyFactory(Class<?> type) {
    checkForward(type, null);
  }

  static void checkForward(Object... args) {
    var type = checkBackward(args);
    var method = factory.get(type);
    if (method != null) {
      for (var param : method.type().parameterArray()) {
        checkForward(param, args);
      }
    }
  }

  static Class<?> checkBackward(Object... args) {
    var type = (Class<?>) args[0];
    var backtrack = (Object[]) args[1];
    while (backtrack != null) {
      if (type.equals(backtrack[0])) {
        throw new IllegalArgumentException("cyclic dependency on " + type);
      }
      backtrack = (Object[]) backtrack[1];
    }
    return type;
  }

}
