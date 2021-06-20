package fx.mvc.util;

import java.lang.annotation.Annotation;
import java.util.function.Predicate;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;

final class Reflections {

    static Class<?> defined(String className) {
        try { return Class.forName(className); }
        catch (Exception e) { return null; }
    }

    @SuppressWarnings("unchecked")
    static <T> T fieldValue(Class<?> type, String name) {
        try { return (T) type.getField(name).get(null); }
        catch (Exception e) { return null; }
    }

    @SuppressWarnings("unchecked")
    static <T> T defaultValue(Class<? extends Annotation> annotation, String name) {
        try { return (T) annotation.getMethod(name).getDefaultValue(); }
        catch (Exception e) { return null; }
    }

    static Constructor<?> constructor(Class<?> type, Object... args) {
        if (type != null) {
            for (var ctor : type.getDeclaredConstructors()) {
                if (accepts(ctor,args)) return ctor;
            }
        }
        return null;
    }

    static Method method(Object obj, String name, Class<?>... parameters) {
        return (name == null || name.isBlank()) ? null
             : method(obj, m -> m.getName().equals(name) && accepts(m,parameters) );
    }

    static Method method(Object obj, Class<? extends Annotation> annotation, Class<?>... parameters) {
        return (obj == null) ? null
             : method(obj, m -> m.isAnnotationPresent(annotation) && accepts(m,parameters) );
    }

    static Method method(Object obj, Predicate<Method> selector) {
        if (obj != null && selector != null) {
            var type = (obj instanceof Class<?>) ? (Class<?>)obj : obj.getClass();
            while (type != java.lang.Object.class) {
                for (var method : type.getDeclaredMethods()) {
                    if (selector.test(method)) {
                        return method;
                    }
                }
                type = type.getSuperclass();
            }
        }
        return null;
    }

    static boolean accepts(Executable exec, Class<?>... params) {
        var pt = exec.getParameterTypes();
        var n = pt.length;
        if (n != params.length) return false;
        for (var i = 0; i < n; i++) {
            if ( ! pt[i].isAssignableFrom(params[i])) return false;
        }
        return true;
    }

    static boolean accepts(Executable exec, Object... args) {
        var pt = exec.getParameterTypes();
        var n = pt.length;
        if (pt.length != args.length) return false;
        for (var i = 0; i < n; i++) {
            if ( ! pt[i].isInstance(args[i])) return false;
        }
        return true;
    }

}
