package fx.node.builder;

import org.w3c.dom.Element;
import java.util.function.Function;

public interface Shim {
    String type();
    String toString(Element element, Function<String,String> resolver);
}
