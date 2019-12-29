package fx.node.builder;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

class Forms {

    final List<Map<String,String>> imports; // <shortName,className>
    final Map<String,String> shims;         // <className,shim.className>
    final Map<String,Form> forms;           // <className,properties>

    final Elements eu;

    Forms(Elements eu) {
        this.eu = eu;
        forms = new HashMap<>();
        shims = new HashMap<>();
        imports = new LinkedList<>();
        imports.add(new HashMap<>());
    }

    void addImport(String name) {
        if (name.endsWith(".*")) {
            addPackage(name.substring(0,name.length()-2));
        } else {
            addName(imports.get(0),name);
        }
    }

    void addPackage(String packageName) {
        var map = new HashMap<String,String>();
        for (var name : classesInPackage(packageName)) {
            addName(map,name);
        }
        imports.add(map);
    }

    static void addName(Map<String,String> map, String name) {
        map.put(name.substring(1+name.lastIndexOf('.')),name);
    }

    void loadShims(String packageName) {
        shims.clear();
        for (var shimName : classesInPackage(packageName)) {
            Shim shim = newInstance(shimName);
            if (shim != null) {
                var valueType = shim.type();
                shims.put(valueType,shimName);
            }
        }
    }

    Shim shimFor(String className) {
        if (className == null) return null;
        var shimName = shims.get(className);
        return shimName != null ? newInstance(shimName) : null;
    }

    static <T> T newInstance(String className) {
        try {
            return (T) Class.forName(className).getConstructor().newInstance();
        }
        catch (Exception ignore) { return null; }
    }

    boolean isA(String className, String superClass) {
        var r = get(className);
        return r != null ? r.isA(superClass) : false;
    }

    String fqcn(String shortName) {
        for (var map:imports) {
            var value = map.get(shortName);
            if (value != null) return value;
        }
        return null; // shortName;
    }

    String propertyType(String className, String propertyName) {
        var form = get(className);
        return form != null ? form.get(propertyName) : null;
    }

    List<String> classesInPackage(String packageName) {
        var list = new LinkedList<String>();
        var pe = eu.getPackageElement(packageName);
        if (pe != null) {
            for (var e : pe.getEnclosedElements()) {
                if (e.getKind() == ElementKind.CLASS) {
                    list.add(e.toString());
                }
            }
        }
        return list;
    }

    Form get(String className) {
        if (className == null || className.equals("java.lang.Object") ) {
            return null;
        }
        var form = forms.get(className);
        if (form == null) {
            form = makeForm(className);
            forms.put(className,form);
        }
        return form;
    }

    Form makeForm(String className) {
        var te = eu.getTypeElement(className);
        if (te == null) return null;
        var map = new HashMap<String,String>();
        for (var e : te.getEnclosedElements()) {
            if (e.getKind() == ElementKind.METHOD) {
                var m = (ExecutableElement)e;
                var name = simpleName(m);
                if (name != null) {
                    map.put(name,elementName(m.getReturnType()));
                }
            }
        }
        var interfaces = getInterfaces(te);
        var superclass = getSuperclass(te);
        return new Form(map,className,interfaces,superclass);
    }

    static String simpleName(ExecutableElement m) {
        var name = m.getSimpleName().toString();
        if (name.startsWith("get")) {
            return Character.toLowerCase(name.charAt(3)) + name.substring(4);
        } else if (name.startsWith("is")) {
            return Character.toLowerCase(name.charAt(2)) + name.substring(3);
        }
        return null;
    }

    static String elementName(TypeMirror typeMirror) {
        var s = typeMirror.toString();
        var p = s.indexOf('<');
        return p > 0 ? s.substring(0,p) : s;
    }

    Set<String> getInterfaces(TypeElement te) {
        var set = new HashSet<String>();
        for (var tm : te.getInterfaces()) {
            set.add(elementName(tm));
        }
        return set;
    }

    Form getSuperclass(TypeElement te) {
        var sc = te.getSuperclass();
        return sc != null ? get(elementName(sc)) : null;
    }

}
