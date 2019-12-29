package fx.node.builder.shims;

import org.w3c.dom.Element;
import java.util.function.Function;

public class Image implements fx.node.builder.Shim {

    @Override
    public String type() {
        return "javafx.scene.image.Image";
    }

    @Override
    public String toString(Element e, Function<String,String> r) {
        var url = e.getAttribute("url");
        return url.isBlank() ? "" : "new "+type()+'('+r.apply(url)+')';
    }

}