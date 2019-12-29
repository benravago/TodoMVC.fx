package todomvc.fx.app;

import fx.mvc.View;
import fx.mvc.util.Lookup;

import javafx.event.Event;
import javafx.stage.WindowEvent;

import javafx.scene.Parent;

@View("todomvc.fx.app.Main")
class MainController {

    Parent root;

    void onLoad(Event e) {
        root = Lookup.$(e).root();
        System.out.println("Main root " + root);
    }

    void onShowing(WindowEvent e) {
        System.out.println("onShowing " + e.getSource());
    }
    
    void onShown(WindowEvent e) {
        System.out.println("onShown " + e.getSource());
    }

}
