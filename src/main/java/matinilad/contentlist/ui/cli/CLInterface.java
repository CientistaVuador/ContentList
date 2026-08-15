package matinilad.contentlist.ui.cli;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Objects;

/**
 *
 * @author Cien
 */
public class CLInterface {
    
    private static void printHelp(PrintStream out) {
        out.println("Available commands:");
        out.println("-create - Creates a new list");
        out.println("-validate - Validates a directory");
    }

    public static void run(PrintStream out, String[] args) throws Exception {
        System.exit(runCLI(out, args));
    }
    
    private static int runCLI(PrintStream out, String[] args) throws Exception {
        Objects.requireNonNull(out, "out is null");
        if (args == null || args.length == 0) {
            out.println("No arguments!");
            printHelp(out);
            return -1;
        }
        switch (args[0].toLowerCase()) {
            case "-create" -> {
                return CreateCommand.run(System.in, out, Arrays.copyOfRange(args, 1, args.length));
            }
            case "-validate" -> {
                return ValidateCommand.run(System.in, out, Arrays.copyOfRange(args, 1, args.length));
            }
            default -> {
                if (!args[0].equalsIgnoreCase("-help")) {
                    out.println("Invalid option: " + args[0]);
                    printHelp(out);
                    return -1;
                }
                printHelp(out);
                return 0;
            }
        }
    }
    
    private CLInterface() {

    }

}
