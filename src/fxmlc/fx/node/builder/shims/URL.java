package fx.node.builder.shims;

import org.w3c.dom.Element;
import java.util.function.Function;

public class URL implements fx.node.builder.Shim {

    @Override
    public String type() {
        return "java.net.URL";
    }

    @Override
    public String toString(Element e, Function<String,String> r) {
        var spec = e.getAttribute("value");
        return spec.startsWith("@") ? r.apply(spec) : "new "+type()+"(\""+spec+"\")";
    }

}
