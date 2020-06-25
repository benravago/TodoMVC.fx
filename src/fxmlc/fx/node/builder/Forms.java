package fx.node.builder;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

abstract class Forms {

    final List<Map<String,String>> imports; // <shortName,className>
    final Map<String,String> shims;         // <className,shim.className>
    final Map<String,Form> forms;           // <className,properties>

    Forms() {
        forms = new HashMap<>();
        shims = new HashMap<>();
        imports = new ArrayList<>();
        imports.add(new HashMap<>());
    }

    abstract Form makeForm(String className);

    void addImport(String name) {
        if (name.endsWith(".*")) {
            addPackage(name.substring(0,name.length()-2));
        } else {
            addName(imports.get(0),name);
        }
    }

    void addPackage(String packageName) {
        var map = new HashMap<String,String>();
        for (var name : classesIn(packageName)) {
            addName(map,name);
        }
        imports.add(map);
    }

    static void addName(Map<String,String> map, String name) {
        map.put(name.substring(1+name.lastIndexOf('.')),name);
    }

    void loadShims(String packageName) {
        shims.clear();
        for (var shimName : classesIn(packageName)) {
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

    @SuppressWarnings("unchecked")
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

    static String property(String name) {
        if (name.startsWith("get") && name.length() > 3) {
            return Character.toLowerCase(name.charAt(3)) + name.substring(4);
        } else if (name.startsWith("is") && name.length() > 2) {
            return Character.toLowerCase(name.charAt(2)) + name.substring(3);
        }
        return null;
    }

    String propertyType(String className, String propertyName) {
        var form = get(className);
        return form != null ? form.get(propertyName) : null;
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

    List<String> classesIn(String packageName) {
        return classesInJar(packageName.replace('.','/'));
    }

    List<String> classesInJar(String packagePath) {
        var url = getClass().getClassLoader().getResource(packagePath);
        var pl = PathList.get(url,packagePath);
        return pl != null ?
            pl.map(p -> p.toString())
              .filter(s -> s.endsWith(".class"))
              .map(s -> s.substring(0,s.length()-6).replace('/','.'))
              .collect(Collectors.toList())
            : Collections.emptyList();
    }

}
