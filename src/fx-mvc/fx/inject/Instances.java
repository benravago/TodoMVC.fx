package fx.inject;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import java.util.function.Supplier;
import java.util.NoSuchElementException;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class Instances {
  private Instances() {}

  private final static ClassValue<Supplier<?>> producer = new ClassValue<>() {
    @Override protected Supplier<?> computeValue(Class<?> type) { return supplier(type); }
  };

  @SuppressWarnings("unchecked")
  public static <T> T newInstance(Class<T> type) {
    return (T) producer.get(type).get();
  }

  @SuppressWarnings("unchecked")
  public static <T> T newInstance(String className) {
    return (T) newInstance(Instantiator.classFor(className)
      .orElseThrow(() -> new NoSuchElementException("class for " + className + " not available")) );
  }

  static final Instantiator itor = new Instantiator();

  static Supplier<?> supplier(Class<?> type) {
    var m = Instantiator.of(type);
    if (m == null) {
      throw new NoSuchElementException("no instance of "+type+" available");
    } else if (m == Instantiator.NO_FACTORY) {
      throw new NoSuchElementException("no @Factory identified for "+type);
    } else if (m == Instantiator.MANY_FACTORY) {
      throw new NoSuchElementException("multiple @Factory's found for "+type);
    }
    var s = supplier(accessible(m));
    return isSingleton(type) ? singleton(s) : s;
  }
  
  static boolean isSingleton(Class<?> type) {
    return type.isAnnotationPresent(Singleton.class);
  }
  
  static Supplier<?> singleton(Supplier<?> s) {
    var instance = s.get();
    return () -> instance;
  }
  
  static Member accessible(Member m) {
    if (m instanceof AccessibleObject ao) {
      ao.setAccessible(true);
    }
    return m;
  }

  static final MethodHandles.Lookup lookup = MethodHandles.lookup();

  static Supplier<?> supplier(Member r) {
    try {
      if (r instanceof Constructor<?> c) {
        return constructor(c);
      } else if (r instanceof Method m) {
        return method(m);
      } else if (r instanceof Field f) {
        return field(f);
      }
    } catch (Throwable e) {
      uncheck(e);
    }
    return null;
  }

  static Supplier<?> constructor(Constructor<?> c) throws Throwable {
    return executable(lookup.unreflectConstructor(c));
  }

  static Supplier<?> method(Method m) throws Throwable {
    return executable(lookup.unreflect(m));
  }

  static Supplier<?> field(Field f) throws Throwable {
    return supplier(lookup.unreflectGetter(f));
  }

  static Supplier<?> supplier(MethodHandle implMethod) throws Throwable {
    var targetClass = implMethod.type().returnType();
    var caller = MethodHandles.privateLookupIn(targetClass, lookup);
    var samMethodType = MethodType.methodType(Object.class);
    var invokedType = MethodType.methodType(Supplier.class);
    var invokedName = "get";
    var callSite = LambdaMetafactory.metafactory(
      caller,
      invokedName, invokedType,
      samMethodType, implMethod,
      samMethodType); // instantiatedMethodType
    return (Supplier<?>) callSite.getTarget().invoke();
  }

  static Supplier<?> executable(MethodHandle mh) throws Throwable {
    return mh.type().parameterCount() > 0 ? withArgs(mh) : noArgs(mh);
  }

  static Supplier<?> noArgs(MethodHandle mh) throws Throwable {
    return supplier(mh); // usable as is
  }

  static Supplier<?> withArgs(MethodHandle mh) throws Throwable {
    return new SAM<>(mh);
  }

  static private final class SAM<T> implements Supplier<T> {
    private SAM(MethodHandle h) { mh = h; }
    private final MethodHandle mh;
    public T get() { return call(mh); }
  }

  @SuppressWarnings("unchecked")
  static private <T> T call(MethodHandle mh) {
    try {
      var param = mh.type().parameterArray();
      var args = new Object[param.length];
      for (var i = 0; i < args.length; i++) {
        // TODO: maybe add cyclic dependency check here
        args[i] = producer.get(param[i]).get();
      }
      return (T) mh.invokeWithArguments(args);
    }
    catch (Throwable t) { return uncheck(t); }
  }

  @SuppressWarnings("unchecked")
  static <T extends Throwable,V> V uncheck(Throwable t) throws T { throw (T)t; }
}
