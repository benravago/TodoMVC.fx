package fx.mvc.runtime;

import java.util.concurrent.Future;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import fx.mvc.stage.OnShowing;
import fx.mvc.stage.OnShown;
import static fx.mvc.util.Events.addEventHandler;
import static fx.mvc.util.Views.loadController;

public final class Runtime {
    private Runtime() {}

    private static final Runtime INSTANCE = new Runtime();

    public static Runtime getRuntime() {
        return INSTANCE;
    }

    public Future<Throwable> launch(Class<?> appClass, String... args) {
        return launch(appClass.getName(),args);
    }

    public synchronized Future<Throwable> launch(String className, String... src) {
        var dest = new String[1+src.length];
        dest[0] = className;
        if (src.length > 0) System.arraycopy(src, 0, dest, 1, src.length);
        return launchApplication(dest);
    }

    private static final InheritableThreadLocal<Object[]> app =
        new InheritableThreadLocal<>(); // { primaryApplication, primaryStage, thrown }

    Future<Throwable> launchApplication(String... args) {
        app.set(new Object[3]);
        var running = new Running();
        new Thread(() -> {
            try {
                Application.launch(Primary.class,args);
                // on return, the 'JavaFX Application Thread' has ended
                // signal fault in App.start(), if any
                running.put((Throwable)app.get()[2]);
            }
            catch (Throwable e) {
                // signal fault before App.start()
                running.put(e);
            }
        }).start();
        return running;
    }

    public static final class Primary extends Application {
        @Override
        public void start(Stage primaryStage) {
            try {
                var info = app.get();
                info[0] = this;
                info[1] = primaryStage;
                var className = getParameters().getRaw().get(0);
                var cv = loadController(className);
                var controller = cv.getKey();
                Parent root = (Parent) cv.getValue();
                addEventHandler(primaryStage,controller,OnShowing.class);
                addEventHandler(primaryStage,controller,OnShown.class);
                Scene scene = new Scene(root);
                primaryStage.setScene(scene);
                primaryStage.show();
            }
            catch (Throwable t) {
                // pass fault to parent thread
                app.get()[2] = t;
                Platform.exit();
            }
        }
    }

    private Application getApplication() {
        var info = app.get();
        if (info[0] != null) return (Application)info[0];
        throw new IllegalStateException("no javafx.application.Application has been launched");
    }

    public Stage getPrimaryStage() {
        var info = app.get();
        return info[1] != null ? (Stage)info[1] : null;
    }

    public HostServices getHostServices() {
        return getApplication().getHostServices();
    }

    public Application.Parameters getParameters() {
        return getApplication().getParameters();
    }

    public static String getUserAgentStylesheet() {
        return Application.getUserAgentStylesheet();
    }

    public static void setUserAgentStylesheet(String url) {
        Application.setUserAgentStylesheet(url);
    }

}
