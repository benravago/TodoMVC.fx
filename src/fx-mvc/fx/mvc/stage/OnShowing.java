package fx.mvc.stage;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnShowing {
  String method() default "onShowing";
  String event() default "javafx.stage.WindowEvent";
  String eventType() default "WINDOW_SHOWING";
}
