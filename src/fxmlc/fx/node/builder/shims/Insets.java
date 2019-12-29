package fx.node.builder.shims;

import org.w3c.dom.Element;
import java.util.function.Function;

public class Insets implements fx.node.builder.Shim {

    @Override
    public String type() {
        return "javafx.geometry.Insets";
    }

    @Override
    public String toString(Element e, Function<String,String> r) {
        var t = false;
        double top = 0.0, right = 0.0, bottom = 0.0, left = 0.0;
        var m = e.getAttributes();
        for (int i = 0, n = m.getLength(); i < n; i++) {
            var a = m.item(i);
            var k = a.getNodeName();
            var v = a.getNodeValue();
            switch (k) {
                case "top": top = Double.parseDouble(v); break;
                case "right": right = Double.parseDouble(v); t = true; break;
                case "bottom": bottom = Double.parseDouble(v); t = true; break;
                case "left": left = Double.parseDouble(v); t = true; break;
            }
        }
        var rbl = t ? ","+right+','+bottom+','+left : "";
        return "new "+type()+'('+top+rbl+')' ;
    }

}
