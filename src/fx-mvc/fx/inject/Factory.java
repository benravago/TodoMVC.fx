package fx.inject;

import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.FIELD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ TYPE, CONSTRUCTOR, METHOD, FIELD })
@Retention(RUNTIME)
public @interface Factory {
  String value() default ""; // only used when in TYPE
}
