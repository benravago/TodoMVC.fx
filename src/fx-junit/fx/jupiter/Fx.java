package fx.jupiter;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.extension.ExtendWith;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(FxRunner.class)
public @interface Fx {
  // tags a method to be run in the JavaFX thread
}
