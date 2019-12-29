package fx.mvc;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface View {
    String value();
    String nodeType() default "javafx.scene.Parent";
    String includeType() default "fx.mvc.util.Views.include";
}