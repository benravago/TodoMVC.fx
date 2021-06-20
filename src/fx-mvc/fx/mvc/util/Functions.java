package fx.mvc.util;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

final class Functions {

  static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

  private static final ClassValue<MethodHandles.Lookup> privateLookup = new ClassValue<>() {
    @Override
    protected MethodHandles.Lookup computeValue(Class<?> type) {
      try {
        return MethodHandles.privateLookupIn(type, LOOKUP);
      } catch (Exception e) {
        return LOOKUP;
      }
    }
  };

  static <T> T lambda(Object impl, Method method, Class<?> functionalInterface) {
    try {
      var implType = impl.getClass();
      var caller = privateLookup.get(implType);
      var invoked = abstractMethod(functionalInterface);
      var invokedType = MethodType.methodType(functionalInterface, implType);
      var samMethodType = methodType(invoked);
      var implMethod = caller.unreflect(method);
      var instantiatedMethodType = methodType(method);

      var callSite = LambdaMetafactory.metafactory(caller,
        invoked.getName(), invokedType, samMethodType, implMethod, instantiatedMethodType);

      return (T) callSite.getTarget().bindTo(impl).invoke();
    } catch (Throwable e) {
      return uncheck(e);
    }
  }

  static Method abstractMethod(Class<?> c) {
    if (c.isAnnotationPresent(FunctionalInterface.class)) {
      for (var m : c.getMethods()) {
        if ((m.getModifiers() & Modifier.ABSTRACT) != 0) {
          return m;
        }
      }
    }
    throw new IllegalArgumentException(String.valueOf(c));
  }

  static MethodType methodType(Method m) {
    return MethodType.methodType(m.getReturnType(), m.getParameterTypes());
  }

  static MethodHandle methodHandle(Class<?> type, Method method) {
    try {
      return privateLookup.get(type).unreflect(method);
    } catch (Throwable e) {
      return uncheck(e);
    }
  }

  static MethodHandle constructorHandle(Class<?> type, Constructor<?> constructor) {
    try {
      return privateLookup.get(type).unreflectConstructor(constructor);
    } catch (Throwable e) {
      return uncheck(e);
    }
  }

  @SuppressWarnings("unchecked")
  static <T> T callStatic(Class<?> refc, String name, Class<?> rtype, Object... args) {
    try {
      var methodType = MethodType.methodType(rtype, parameterTypes(args));
      return (T) privateLookup.get(refc).findStatic(refc, name, methodType).invokeWithArguments(args);
    } catch (Throwable e) {
      return uncheck(e);
    }
  }

  @SuppressWarnings("unchecked")
  static <T> T callVirtual(Object obj, Method method, Object... args) {
    try {
      return (T) privateLookup.get(obj.getClass()).unreflect(method).invokeWithArguments(arguments(obj, args));
    } catch (Throwable e) {
      return uncheck(e);
    }
  }

  static Object[] arguments(Object obj, Object... objs) {
    var args = new Object[objs.length + 1];
    System.arraycopy(objs, 0, args, 1, objs.length);
    args[0] = obj;
    return args;
  }

  static Class<?>[] parameterTypes(Object... args) {
    var ptypes = new Class<?>[args.length];
    for (var i = 0; i < ptypes.length; i++) {
      var arg = args[i];
      if (arg != null) ptypes[i] = arg.getClass();
    }
    return ptypes;
  }

  @SuppressWarnings("unchecked")
  static <R, T extends Throwable> R uncheck(Throwable e) throws T { throw (T) e; }

}
