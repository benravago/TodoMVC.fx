package fx.node.builder;

import org.w3c.dom.Node;
import org.w3c.dom.Element;

class NodeBuilder extends NodeScanner {

    NodeBuilder(Forms resolver) {
        super(resolver);
    }

    @Override
    String prologue() {
        return "package " + packageName + ";\n"
             + "@"+ controllerTag + "(\"" + controllerName + "\")\n"
             + visibility + "class " + className + " {\n"
             + visibility + "static " + nodeType + " root(" + controllerName + ' ' + controllerId +")\n";
    }
    @Override
    String epilogue() {
        return "}\n";
    }

    @Override
    void translateElement(Element e) {
        var p = parent(e);
        if (p == null) {
            ReturnRoot(e);
        } else {
            var str = shimFor(e);
            if (str != null) {
                TextContent(p,e,str);
                return;
            } else {
                if (isList(e)) {
                    GetList(e);
                } else {
                    if (typeOf(e) != null) {
                        if (typeOf(p) != null || isList(p)) {
                            AddItem(e,p);
                        } else {
                            PutProperty(e);
                        }
                    } else {
                        if ("fx:include".equals(e.getTagName())) {
                            IncludeItem(e,p);
                        } else {
                            SetProperty(e);
                        }
                    }
                }
            }
        }
        translateAttributes(e);
    }

    @Override
    void translateAttributes(Element e) {
        var f = e.getFirstChild().getNextSibling();
        var m = e.getAttributes();
        var n = m.getLength();
        if (n < 1) return;
        for (var i = 0; i < n; i++) {
            var a = m.item(i);
            var name = a.getNodeName();
            if (ignore(name)) continue;
            var value = a.getNodeValue();
            if (!specialAttribute(name,value)) {
                translateAttribute(e,f,name,value);
            }
        }
    }

    static boolean ignore(String n) {
        return n.startsWith("_") || n.startsWith("xmlns:") || n.equals("xmlns");
    }

    boolean specialAttribute(String name, String value) {
        if ("fx:controller".equals(name)) {
            if (controllerName == null) {
                controllerName = value;
            }
            return true;
        }
        return false;
    }

    void translateAttribute(Element e, Node f, String cn, String cv) {
        var fx = "";
        var d = cn.indexOf(':');
        if (d > 0) {
            fx = cn;
            cn = cn.substring(d+1);
        }
        d = cn.indexOf('.');
        if (d > 0) {
            parentAttribute(e,f,cn.substring(0,d),cn.substring(d+1),cv);
        } else {
            var p = forms.propertyType(typeOf(e),cn);
            if (p != null) {
                var cf = field(cn);
                if (isPrimitive(p)){
                    SetPrimitive(e,f,cf,cv);
                } else if (p.equals("java.lang.String")) {
                    SetPlain(e,f,cf,resolve(cv));
                } else if (forms.isA(p,"java.lang.Enum")) {
                    SetEnum(e,f,cf,enumOf(p,cv));
                } else if (forms.isA(p,"java.util.List")) {
                    AddListItem(e,f,cf,resolve(cv));
                } else if (forms.isA(p,"javafx.event.EventHandler")) {
                    eventHandler(e,f,cf,cv);
                } else if (p.equals("java.lang.Object")) {
                    SetPlain(e,f,cf,resolve(cv));
                } else if (p.equals("javafx.scene.paint.Paint")) {
                    SetColor(e,f,cf,cv.toUpperCase());
                } else if (cv.startsWith("$")) {
                    SetExtern(e,f,cf,cv.substring(1));
                } else if (p.endsWith("[]")) {
                    SetPlain(e,f,cf,cv);
                } else {
                    Unused(e,f,cn,cv,p);
                }
            } else {
                Unused(e,f,cn,cv,p);
            }
        }
    }

    void parentAttribute(Element e, Node f, String cn, String cf, String cv) {
        var fqcn = forms.fqcn(cn);
        var ct = forms.get(fqcn).get(cf);
        if (ct.startsWith("javafx.")) cv = ct + '.' + cv; // optimistic
        SetStatic(e,f, fqcn, field(cf), cv );
    }

    void eventHandler(Element e, Node f, String cf, String cv) {
        switch (cv.charAt(0)) {
            case '#': SetReference(e,f,cf,cv.substring(1)); break;
            case '$': SetReturned(e,f,cf,cv.substring(1)); break;
            default:  SetFunction(e,f,cf,cv); break;
        }
    }

    void TextContent(Element p, Element e, String str) {
        if (isList(p)) {
            str = s( "_%d.add(%s);\n", level(p), str );
        }
        e.setTextContent(str);
    }

    void GetList(Element e) {
        var l = level(e);
        enclose(e, s( "{ var _%d = _%d.get%s();\n", l, l-1, field(e) ), "}\n" );
    }

    void SetProperty(Element e) {
        var x = has(e,"_put") ? "" : ";\n";
        var l = level(e);
        enclose(e, s( "{ var _%d = ", l ), s( "%s_%d.set%s(_%d); }\n", x, l-1, field(e), l ) );
    }

    void PutProperty(Element e) {
        have(e.getParentNode(),"_put");
        var l = level(e);
        enclose(e, s( "new %s(); var _%d=_%d;\n", typeOf(e), l, l-1 ), "");
    }

    void AddItem(Element e, Element p) {
        var l = level(e);
        var g = isList(p) ? "" : ".getChildren()"; // optimistic!
        enclose(e, s( "{ var _%d = new %s();\n", l, typeOf(e) ), s( "_%d%s.add(_%d); }\n", l-1, g, l ) );
    }

    void IncludeItem(Element e, Element p) {
        var source = source(e);
        var l = level(e);
        var g = isList(p) ? "" : ".getChildren()"; // optimistic!
        enclose(e, s( "{ var _%d = %s(\"%s\",%s);\n", l, includeAction, packageName, source ),
                   s( "_%d%s.add(_%d); }\n", l-1, g, l ) );
    }

    void ReturnRoot(Element e) {
        var l = level(e);
        enclose(e, s( "{ var _%d = new %s();\n", l, typeOf(e) ), s( "return _%d; }\n", l ) );
    }

    void Unused(Element e, Node f, String cf, String cv, String p) {
        insert(e,f, s( "// %s = %s >> %s\n", cf, cv, p ));
    }

    void SetStatic(Element e, Node f, String cn, String cf, String cv) {
        insert(e,f, s( "%s.set%s(_%d,%s);\n", cn, cf, level(e), cv ));
    }

    void SetPrimitive(Element e, Node f, String cf, String cv) {
        insert(e,f, s( "_%d.set%s(%s);\n", level(e), cf, cv ));
    }

    void SetPlain(Element e, Node f, String cf, String cv) {
        insert(e,f, s( "_%d.set%s(%s);\n", level(e), cf, cv ));
    }

    void SetColor(Element e, Node f, String cf, String cv) {
        insert(e,f, s( "_%d.set%s(javafx.scene.paint.Color.%s);\n", level(e), cf, cv ));
    }

    void SetEnum(Element e, Node f, String cf, String cv) {
        insert(e,f, s( "_%d.set%s(%s);\n", level(e), cf, cv ));
    }

    void AddListItem(Element e, Node f, String cf, String cv) {
        insert(e,f, s( "_%d.get%s().add(%s);\n", level(e), cf, cv ));
    }

    void SetReference(Element e, Node f, String cf, String cv) {
        insert(e,f, s( "_%d.set%s(%s::%s);\n", level(e), cf, controllerId, cv ));
    }

    void SetReturned(Element e, Node f, String cf, String cv) {
        insert(e,f, s( "_%d.set%s(%s.%s());\n", level(e), cf, controllerId, cv ));
    }

    void SetFunction(Element e, Node f, String cf, String cv) {
        insert(e,f, s( "_%d.set%s(%s);\n", level(e), cf, cv ));
    }

    void SetExtern(Element e, Node f, String cf, String cv) {
        insert(e,f, s( "_%d.set%s(%s.%s);\n", level(e), cf, controllerId, cv ));
    }

    String source(Element e) {
        var source = e.getAttribute("source");
        e.removeAttribute("source");
        return resolve(source);
    }

}
