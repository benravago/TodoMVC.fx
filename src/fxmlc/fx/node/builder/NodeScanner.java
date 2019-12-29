package fx.node.builder;

import java.io.InputStream;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;

abstract class NodeScanner {

    Forms forms;

    String packageName;
    String className, nodeType;
    String controllerName, controllerTag;
    String controllerId = "_0";
    String visibility = "";
    String includeAction;

    Doc doc;

    NodeScanner(Forms resolver) {
        forms = resolver;
        forms.loadShims(getClass().getPackageName()+".shims");
    }

    NodeScanner setView(String name, String type) {
        nodeType = type;
        packageName = "";
        className = String.valueOf(name);
        if (name != null && !name.isBlank()) {
            var p = name.lastIndexOf('.');
            if (p > -1) {
                packageName = name.substring(0,p);
                className = name.substring(p+1);
            }
        }
        return this;
    }

    NodeScanner setController(String name, String tag) {
        controllerTag = tag;
        controllerName = name;
        return this;
    }

    NodeScanner setInclude(String include) {
        includeAction = include;
        return this;
    }

    String transform(InputStream in) {
        setVisibility();
        doc = Doc.from(in);
        transform();
        return prologue() + body() + epilogue();
    }

    void transform() {
        doc.forEach(Node.TEXT_NODE,this::trimText)
           .forEach(Node.PROCESSING_INSTRUCTION_NODE,this::addImports)
           .forEach(Node.ELEMENT_NODE,this::getDefines)
           .forEach(Node.ELEMENT_NODE,this::setMetadata)
           .forEach(Node.ELEMENT_NODE,this::translateElement);
    }

    abstract String prologue();
    abstract String epilogue();

    String body() {
        return doc.text();
    }

    void setVisibility() {
        var p = controllerName.lastIndexOf('.');
        if (p < 0) p = 0;
        visibility = packageName.regionMatches​(0,controllerName,0,p) ? "" : "public ";
    }

    void trimText(Node n) {
        var c = n.getTextContent();
        if (c.isBlank()) {
            remove(n);
        } else {
            c = c.trim();
            n.setTextContent(c);
        }
    }

    void addImports(ProcessingInstruction pi) {
        if ("import".equals(pi.getTarget())) {
            forms.addImport(pi.getData().trim());
        }
        remove(pi);
    }

    void getDefines(Element e) {
        var tag = e.getTagName();
        if (tag.equals("fx:define")) {
            remove(e);
        }
    }    
    
    void setMetadata(Element e) {
        var tag = e.getTagName();
        var type = forms.fqcn(tag);
        if (type != null) {
            e.setAttribute("_type",type);
        }
        if (type == null) {
            var parent = forms.fqcn(e.getParentNode().getNodeName());
            type = forms.propertyType(parent,tag);
        }
        if (type != null) {
            if (forms.isA(type,"java.util.List")) {
                e.setAttribute("_list","1");
            }
        }
    }

    static String typeOf(Element e) {
        var type = e.getAttribute("_type");
        return type.isBlank() ? null : type;
    }

    static boolean isList(Element e) {
        return ! e.getAttribute("_list").isBlank();
    }

    abstract void translateElement(Element e);
    abstract void translateAttributes(Element e);

    static int level(Node n) {
        var i = 0;
        while (n.getNodeType() != Node.DOCUMENT_NODE) {
            i++; n = n.getParentNode();
        }
        return i;
    }

    static String field(Node n) {
        return field(n.getNodeName());
    }
    static String field(String v) {
        return Character.toUpperCase(v.charAt(0))+v.substring(1);
    }

    static void remove(Node n) {
        n.getParentNode().removeChild(n);
    }

    static void insert(Node parent, Node child, Node last) {
        if (last != null) {
            parent.insertBefore​(child,last);
        } else {
            parent.appendChild​(child);
        }
    }

    static Element parent(Node n) {
        var p = n.getParentNode();
        return (p.getNodeType() == Node.ELEMENT_NODE) ? (Element)p : null;
    }

    void insert(Element e, Node ref, String body) {
        e.insertBefore(text(body),ref);
    }

    void enclose(Element e, String head, String tail) {
        var h = text(head);
        var c = e.getFirstChild();
        if (c == null) {
            e.appendChild(h);
        } else {
            e.insertBefore(h,c);
        }
        e.appendChild(text(tail));
    }

    static String s(String format, Object ... args) {
        return String.format(format,args);
    }

    static boolean has(Node n, String name) {
        return (n instanceof Element) && ((Element)n).hasAttribute(name);
    }

    static void have(Node n, String name) {
        if (n instanceof Element) ((Element)n).setAttribute(name,"1");
    }

    Node text(String data) {
        return doc.document().createTextNode(data);
    }

    String shimFor(Element e) {
        var shim = forms.shimFor(typeOf(e));
        return shim != null ? shim.toString(e,this::resolve) : null;
    }

    static String enumOf(String p, String v) {
        return p+'.'+v.toUpperCase();
    }

    static boolean isPrimitive(String type) {
        switch (type) {
            default: {
                return false;
            }
            case "char": case "boolean":
            case "short": case "int": case "long":
            case "float": case "double": {
                return true;
            }
        }
    }

    String resolve(String v) {
        if (v.startsWith("new ")) {
            return v;
        } else if (v.charAt(0) == '@') {
            return className+".class.getResource(\""+v.substring(1)+"\").toString()";
        } else {
            return isNumber(v) ? v : quoted(v);
        }
    }

    static Matcher number = Pattern.compile("\\d+(?:\\.\\d+)?").matcher("");

    static boolean isNumber(String v) {
        return number.reset(v).matches();
    }

    static String quoted(String v) {
        var b = new StringBuilder("\"");
        var n = v.length();
        for (var i = 0; i < n; i++) {
            var c = v.charAt(i);
            switch (c) {
                case '\b': b.append("\\b"); break;
                case '\n': b.append("\\n"); break;
                case '\t': b.append("\\t"); break;
                case '\f': b.append("\\f"); break;
                case '\r': b.append("\\r"); break;
                case '\"': b.append("\\\""); break;
                case '\'': b.append("\\\'"); break;
                case '\\': b.append("\\\\"); break;
                default: b.append(c); break;
            }
        }
        return b.append('"').toString();
    }

}
