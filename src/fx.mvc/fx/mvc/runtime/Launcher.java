package fx.mvc.runtime;

public class Launcher {
    public static void main(String...args) throws Throwable {
        if (args.length > 0) {
            var running = Runtime.getRuntime().launchApplication(args);
            Thread.sleep(500); // .5 sec
            var fault = running.get();
            if (fault != null) {
                throw fault;
            }
        } else {
            System.out.println("usage: Launcher <application.className> <application.parameters ...>");
        }
    } 
}
