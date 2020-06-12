package fx.node.builder;

import java.util.function.Consumer;

import java.io.InputStream;
import java.io.OutputStream;

import org.w3c.dom.Node;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

class Doc {

    private final Document doc;

    private Doc(Document doc) {
        this.doc = doc;
    }

    static Doc from(InputStream in) {
        return new Doc(read(in));
    }

    void print(OutputStream out) {
        write(doc,out);
    }

    Document document() {
        return doc;
    }

    String text() {
        return doc.getDocumentElement().getTextContent();
    }

    @SuppressWarnings("unchecked")
    <T extends Node> T rename(Node node, String newName) {
        return (T) doc.renameNode(node,null,newName);
    }

    @SuppressWarnings("unchecked")
    <T extends Node> Doc forEach(String tagname, Consumer<T> consumer) {
        var list = doc.getElementsByTagName(tagname);
        for (int i = 0, n = list.getLength(); i < n; i++) {
            consumer.accept((T)list.item(i));
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    <T extends Node> Doc forEach(short nodeType, Consumer<T> consumer) {
        return forEach( n -> {
            if (n.getNodeType() == nodeType) consumer.accept((T)n);
        });
    }

    Doc forEach(Consumer<Node> consumer) {
        traverse(doc,consumer);
        return this;
    }

    // bottom-up traversal so that child nodes can be altered by consumer

    static void traverse(Node parent, Consumer<Node> consumer) {
        for (var child : childNodes(parent)) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                traverse(child,consumer);
            }
            consumer.accept(child);
        }
    }

    static Node[] childNodes(Node node) {
        var list = node.getChildNodes();
        var nodes = new Node[list.getLength()];
        for (var i = 0; i < nodes.length; i++) {
            nodes[i] = list.item(i);
        }
        return nodes;
    }

    static Document read(InputStream in) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            var builder = factory.newDocumentBuilder();
            return builder.parse(in);
        }
        catch (Exception e) { return uncheck(e); }
    }

    static void write(Document in, OutputStream out) {
        try {
            var factory = TransformerFactory.newInstance();
            var transformer = factory.newTransformer();
            transformer.transform( new DOMSource(in), new StreamResult(out) );
        }
        catch (Exception e) { uncheck(e); }
    }

    @SuppressWarnings("unchecked")
    static <R,T extends Throwable> R uncheck(Throwable t) throws T { throw (T)t; }

}
