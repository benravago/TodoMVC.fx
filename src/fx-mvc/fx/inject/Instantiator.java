package fx.inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.AnnotatedElement;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

class Instantiator {

  static Member of(Class<?> type) {
    assert type != null;
    return isAbstract(type) ? accessor(type) : constructor(type);
  }

  static Member constructor(Class<?> type) {
    var a = type.getDeclaredConstructors();
    if (a.length > 0) {
      var m = noArg(a);
      return m != null ? m : select(a);
    }
    return null;
  }

  static Member noArg(Constructor<?>[] constructors) {
    return Arrays
      .stream(constructors)
      .filter(m -> // Type()
         isAccessible(m) &&
         m.getParameterCount() == 0)
      .findAny()
      .orElse(null);
  }

  static Member select(Constructor<?>[] constructors) {
    var a = Arrays
      .stream(constructors)
      .filter(Instantiator::isAccessible) // public or package
      .toArray(Constructor[]::new);
    return switch (a.length) {
      case 0 -> null; // no usable constructor
      case 1 -> a[0]; // only usable constructor
      default -> factory(a); // multiple choice; one (and only one) must have @Factory
    };
  }

  static Member accessor(Class<?> type) {
    var m = accessorMethod(type);
    return m != null ? m : factoryField(type);
  }

  static Member accessorMethod(Class<?> type) {
    var m = instanceMethod(type);
    return m != null ? m : factoryMethod(type);
  }

  static Member instanceMethod(Class<?> type) {
    return Arrays
      .stream(type.getDeclaredMethods())
      .filter(m -> // static Type newInstance();
         isAccessible(m) &&
         isStatic(m) &&
         m.getReturnType().equals(type) &&
         m.getName().equals("newInstance") &&
         m.getParameterCount() == 0)
      .findAny()
      .orElse(null);
  }

  static Member factoryMethod(Class<?> type) {
    var from = factory(type);
    return from != null ? method(type,from) : null;
  }

  static Member method(Class<?> type, Class<?> from) {
    return select(
      from.getDeclaredMethods(),
      "get" + type.getSimpleName(), // getType();
      m -> ((Method)m).getReturnType().equals(type));
  }

  static Member factoryField(Class<?> type) {
    var from = factory(type);
    return from != null ? field(type,from) : null;
  }

  static Member field(Class<?> type, Class<?> from) {
    return select(
      from.getDeclaredFields(),
      "get" + type.getSimpleName(), // getType;
      f -> ((Field)f).getType().equals(type));
  }

  static Member select(Member[] list, String name, Predicate<Member> selector) {
    var a = Arrays
      .stream(list)
      .filter(m -> // static Type ...
         isAccessible(m) &&
         isStatic(m) &&
         selector.test(m))
      .toArray(Member[]::new);
    if (a.length > 0) {
      var m = getter(a,name);
      return m != null ? m : factory(a);
    }
    return null;
  }

  static Member getter(Member[] list, String name) {
    return Arrays
      .stream(list)
      .filter(m -> m.getName().equals(name)) // getType or getType(...) ];
      .findAny()
      .orElse(null);
  }

  static Member factory(Member[] list) {
    var a = Arrays
      .stream(list)
      .filter(Instantiator::isFactory) // has @Factory
      .toArray(Member[]::new);
    return switch (a.length) {
      case 0 -> NO_FACTORY; // no tagged choice
      case 1 -> a[1]; // single tagged choice
      default -> MANY_FACTORY; // more than one tagged choices
    };
  }

  static final int ABSTRACT = Modifier.ABSTRACT | Modifier.INTERFACE;
  static final int HIDDEN = Modifier.PRIVATE | Modifier.PROTECTED;

  static boolean isAbstract(Class<?> t) {
    return (t.getModifiers() & ABSTRACT) != 0;
  }
  static boolean isAccessible(Member m) {
    return (m.getModifiers() & HIDDEN) == 0;
  }
  static boolean isStatic(Member m) {
    return (m.getModifiers() & Modifier.STATIC) != 0;
  }

  // used to tag no- or multiple- @Factory condition
  static final Member NO_FACTORY, MANY_FACTORY;
  static {
    var f = Instantiator.class.getDeclaredFields();
    NO_FACTORY = f[0];
    MANY_FACTORY = f[1];
  }

  static boolean isFactory(Member m) {
    return ((AnnotatedElement)m).isAnnotationPresent(Factory.class);
  }

  static Class<?> factory(Class<?> type) {
    var name = factoryName(type);
    return classFor(name).orElse(type);
  }

  static String factoryName(Class<?> type) {
    var tag = type.getAnnotation(Factory.class);
    return tag != null ? tag.value() : type.getName() + "Factory";
  }

  static Optional<Class<?>> classFor(String name) {
    try { return Optional.of(Class.forName(name)); }
    catch (ClassNotFoundException e) { return Optional.empty(); }
  }

}