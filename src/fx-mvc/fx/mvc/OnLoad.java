package fx.mvc;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnLoad {
  String method() default "onLoad";
  String event() default "javafx.event.Event";
  String eventType() default "ANY";
}
