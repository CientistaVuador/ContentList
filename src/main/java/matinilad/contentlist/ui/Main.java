package matinilad.contentlist.ui;

import java.io.IOException;
import java.util.Arrays;
import matinilad.contentlist.ui.cli.CLInterface;
import matinilad.contentlist.ui.gui.GUInterface;
import matinilad.contentlist.ui.tui.TUInterface;

/**
 *
 * @author Cien
 */
public class Main {

    public static void main(String[] args) throws IOException {
        if (args == null || args.length == 0) {
            GUInterface.run();
            return;
        }
        String first = args[0];
        switch (first) {
            case "-cli" -> {
                CLInterface.run(System.out, Arrays.copyOfRange(args, 1, args.length));
            }
            case "-tui" -> {
                TUInterface.run(System.in, System.out, Arrays.copyOfRange(args, 1, args.length));
            }
            case "-gui" -> {
                GUInterface.run();
            }
            default -> {
                if (!first.equals("-help")) {
                    System.out.println("Invalid option: "+first);
                }
                System.out.println("Available interfaces:");
                System.out.println("-cli - The command line interface (create, validate)");
                System.out.println("-tui - The terminal user interface (open)");
                System.out.println("-gui - The graphical user interface (default) (create, validate, open)");
            }
        }
    }
    
}
