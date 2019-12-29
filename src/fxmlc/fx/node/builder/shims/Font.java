package fx.node.builder.shims;

import org.w3c.dom.Element;
import java.util.function.Function;

public class Font implements fx.node.builder.Shim {

    @Override
    public String type() {
        return "javafx.scene.text.Font";
    }

    @Override
    public String toString(Element e, Function<String,String> r) {
        var name = e.getAttribute("name");
        var size = e.getAttribute("size");
        return "new "+type()+"(\""+name+"\","+size+')';
    }

}