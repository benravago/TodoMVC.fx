package fx.node.builder.shims;

import org.w3c.dom.Element;
import java.util.function.Function;

public class KeyCodeCombination implements fx.node.builder.Shim {

    @Override
    public String type() {
        return "javafx.scene.input.KeyCodeCombination";
    }

    @Override
    public String toString(Element e, Function<String,String> r) {
        var b = new StringBuilder();
        setCode(b,e);
        setModifier(b,e,"shift");
        setModifier(b,e,"control");
        setModifier(b,e,"alt");
        setModifier(b,e,"meta");
        setModifier(b,e,"shortcut");
        return "new "+type()+'('+b+')';
    }

    void setCode(StringBuilder b, Element e) {
        var code = e.getAttribute("code");
        if (!code.isBlank()) {
            b.append("javafx.scene.input.KeyCode.").append(code.toUpperCase());
        }
    }

    void setModifier(StringBuilder b, Element e, String name) {
        var value = e.getAttribute(name);
        if (!value.isBlank()) {
            b.append(',').append("javafx.scene.input.KeyCombination.")
             .append(name.toUpperCase()).append('_').append(value.toUpperCase());
        }
    }

}
