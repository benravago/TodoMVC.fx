package fx.node.builder;

import java.util.Map;
import java.util.Set;

class Form {

    final Map<String,String> map;
    final Set<String> interfaces;
    final String className;
    final Form superclass;

    Form(String className, Map<String,String> map, Set<String> interfaces, Form superclass) {
        this.map = map;
        this.className = className;
        this.interfaces = interfaces;
        this.superclass = superclass;
    }

    boolean isA(String type) {
        return className.equals(type)
            || interfaces.contains(type)
            || (superclass != null && superclass.isA(type));
    }

    boolean contains(String propertyName) {
        return map.containsKey(propertyName)
            || (superclass != null && superclass.contains(propertyName));
    }

    String get(String propertyName) {
        var propertyType = map.get(propertyName);
        return propertyType != null ? propertyType
             : superclass != null ? superclass.get(propertyName)
             : null;
    }

    void put(String propertyName, String propertyType) {
        map.put(propertyName,propertyType);
    }

}
