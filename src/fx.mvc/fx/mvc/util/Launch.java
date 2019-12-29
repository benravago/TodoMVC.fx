package fx.mvc.util;

import java.util.concurrent.Future;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import fx.mvc.stage.OnShown;
import fx.mvc.stage.OnShowing;
import static fx.mvc.util.Events.*;
import static fx.mvc.util.Views.*;

public final class Launch {
    private Launch() {}

    public static void main(String[] args) throws Throwable {
        if (args.length > 0) {
            fault.set(new Throwable[1]);
            Application.launch(App.class,args[0]);
            var t = fault.get()[0];
            if (t != null) {
                throw t;
            }
        }
    }

    private static final InheritableThreadLocal<Throwable[]> fault =
        new InheritableThreadLocal<>();

    public static Future<Throwable> application(String className) {
        fault.set(new Throwable[1]);
        var running = new Running();
        new Thread(() -> {
            try {
                Application.launch(App.class,className);
                // on return, the 'JavaFX Application Thread' has ended
                // signal fault in App.start(), if any
                running.put(fault.get()[0]);
            }
            catch (Throwable e) {
                // signal fault before App.start()
                running.put(e);
            }
        }).start();
        return running;
    }

    public static final class App extends Application {
        @Override
        public void start(Stage primaryStage) {
            try {
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
                fault.get()[0] = t;
                Platform.exit();
            }
        }
    }

}