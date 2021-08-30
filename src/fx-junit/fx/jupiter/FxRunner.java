package fx.jupiter;

import javafx.application.Platform;

import java.util.concurrent.locks.LockSupport;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.ExtensionContext;

public class FxRunner implements InvocationInterceptor {

  private static volatile boolean ready = false;

  public static void startup() {
    if (!ready) {
      Platform.startup(() -> {
        ready = true;
        System.out.println("JavaFX Platform ready");
      });
    }
  }
  
  public static void shutdown() {
    if (ready) {
      Platform.exit();
    }
  }

  @SuppressWarnings("unchecked")
  static <T> T proceed(Invocation<T> invocation) throws Throwable {
    var t = Thread.currentThread();
    var r = new Object[1];
    Platform.runLater(() -> {
      try { r[0] = invocation.proceed(); }
      catch (Throwable e) { r[0] = e; }
      finally { LockSupport.unpark(t); }
    });
    LockSupport.park();
    if (r[0] instanceof Throwable x) throw x;
    return (T) r[0];
  }

  static <T> T intercept(Invocation<T> invocation, ReflectiveInvocationContext<?> context) throws Throwable {
    if (context.getExecutable().isAnnotationPresent(Fx.class)) { // never null
      startup(); // ensure toolkit is initialized
      return proceed(invocation); // run in FX thread
    } else { 
      return invocation.proceed();
    }
  }
  
  @Override 
  public <T> T interceptTestClassConstructor(Invocation<T> invocation, ReflectiveInvocationContext<Constructor<T>> invocationContext, ExtensionContext extensionContext) throws Throwable {
    return intercept(invocation,invocationContext);
  }
  @Override 
  public void interceptBeforeAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
    intercept(invocation,invocationContext);
  }
  @Override 
  public void interceptBeforeEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
    intercept(invocation,invocationContext);
  }
  @Override 
  public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
    intercept(invocation,invocationContext);
  }
  @Override 
  public <T> T interceptTestFactoryMethod(Invocation<T> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
    return intercept(invocation,invocationContext);
  }
  @Override 
  public void interceptTestTemplateMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
    intercept(invocation,invocationContext);
  }
  @Override 
  public void interceptAfterEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
    intercept(invocation,invocationContext);
  }

  @Override 
  public void interceptAfterAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
    intercept(invocation,invocationContext);
  }

  // 5.8.x
  // void interceptDynamicTest(Invocation<Void> invocation, DynamicTestInvocationContext invocationContext, ExtensionContext extensionContext)
}
