package todomvc.fx.app;

import fx.mvc.util.Launch;

public class ReferenceApp {

    public static void main(String... args) throws Throwable {
        var name = "todomvc.fx.app.MainController";
        var rc = Launch.application(name);
        System.out.println("app " + name + " started");
        var e = rc.get();
        if (e != null) {
            throw e;
        }
        System.out.println("" + ReferenceApp.class + " done");
    }
}
