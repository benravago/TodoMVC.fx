package fx.mvc.util;

public interface Bindable<T> {

    void bind(T object);

    static <T> void bind(Object ref, T arg) {
        if (ref instanceof Bindable) {
            ((Bindable)ref).bind(arg);
        }
    }

    static <T> void call(Object ref, T arg) {
        var m = Reflections.method(ref,"bind",arg.getClass());
        if (m != null) {
            Functions.callVirtual(ref,m,arg);
        }
    }

}
