package fx.mvc.util;

import java.util.List;
import java.lang.invoke.MethodHandle;

import javafx.event.Event;
import javafx.stage.Window;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.beans.DefaultProperty;

public final class Lookup {

    final Object root;

    private Lookup(Object root) {
        this.root = root;
    }

    @SuppressWarnings("unchecked")
    public <T> T root() {
        return (T) root;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String name) {
        if (name != null && ! name.isBlank()) {
            try { return (T) lookup(root,name); }
            catch (Throwable ignore) {}
        }
        return null;
    }

    static Object lookup(Object n, String id) throws Throwable {
        var m = getId.get(n.getClass());
        return (m != null && id.equals(m.invoke(n))) ? n : itemize(n,id);
    }

    static Object itemize(Object n, String id) throws Throwable {
        var m = getItems.get(n.getClass());
        if (m != null) {
            var content = m.invoke(n);
            if (content instanceof List) {
                for (var item:(List<?>)content) {
                    var t = lookup(item,id);
                    if (t != null) return t;
                }
            } else if (content != null) {
                return lookup(content,id);
            }
        }
        return null;
    }

    public static Lookup $(Object n) {
        return n != null ? new Lookup(n) : null;
    }

    public static Lookup $(Event e) {
        return $(e.getSource());
    }
    public static Lookup $(Scene s) {
        return $(s.getRoot());
    }
    public static Lookup $(Window w) {
        return $(w.getScene());
    }
    public static Lookup $$(Node n) {
        return $(n.getScene());
    }

    private final static ClassValue<MethodHandle> getId =
        new ClassValue<MethodHandle>() {
        @Override
        protected MethodHandle computeValue(Class<?> refc) {
            return getter(refc,"getId");
        }
    };

    private final static ClassValue<MethodHandle> getItems =
        new ClassValue<MethodHandle>() {
        @Override
        protected MethodHandle computeValue(Class<?> refc) {
            var name = defaultGetter(refc);
            return getter(refc,name);
        }
    };

    static String defaultGetter(Class<?> refc) {
        var def = refc.getAnnotation(DefaultProperty.class);
        if (def != null) {
            var n = def.value();
            return "get" + Character.toUpperCase(n.charAt(0)) + n.substring(1);
        }
        return null;
    }

    static MethodHandle getter(Class<?> refc, String name) {
        if (name != null) {
            var method = Reflections.method(refc,name);
            if (method != null) {
                return Functions.methodHandle(refc,method);
            }
        }
        return null;
    }

}
