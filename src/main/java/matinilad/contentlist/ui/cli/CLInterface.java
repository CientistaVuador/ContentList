package matinilad.contentlist.ui.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import matinilad.contentlist.ui.UIUtils;

/**
 *
 * @author Cien
 */
public class CLInterface {
    
    public static final Logger LOGGER = Logger.getLogger(CLInterface.class.getName());
    
    public static boolean readBooleanProperty(String property, boolean defaultValue) {
        String value = System.getProperty(property);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
    
    public static int readIntegerProperty(String property, int defaultValue, int min, int max) {
        String value = System.getProperty(property);
        if (value == null) {
            return defaultValue;
        }
        try {
            int intValue = Integer.parseInt(value);
            if (intValue < min) {
                throw new NumberFormatException("Minimum Value Is: "+min);
            }
            if (intValue > max) {
                throw new NumberFormatException("Maximum Value Is: "+max);
            }
            return intValue;
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.WARNING, "Failed to Read Property: "+property, ex);
        }
        return defaultValue;
    }
    
    public static final boolean ENABLE_VERBOSE_LOGGING = readBooleanProperty(UIUtils.internalName()+".cli.verbose", true);
    
    static {
        if (!ENABLE_VERBOSE_LOGGING) {
            LOGGER.setLevel(Level.WARNING);
        }
    }
    
    private static void printHelp(PrintStream out) {
        out.println("Available commands:");
        out.println("-create [output csv file] [input file/directory] [input file/directory]...");
        out.println("-validate [input csv file] [root directory]");
    }

    public static void run(PrintStream out, String[] args) {
        System.exit(runCLI(out, args));
    }
    
    private static int runCLI(PrintStream out, String[] args) {
        Objects.requireNonNull(out, "out is null");
        if (args == null || args.length == 0) {
            out.println("No arguments!");
            printHelp(out);
            return -1;
        }
        switch (args[0]) {
            case "-create" -> {
                return create(out, Arrays.copyOfRange(args, 1, args.length));
            }
            case "-validate" -> {
                return validate(out, Arrays.copyOfRange(args, 1, args.length));
            }
            default -> {
                if (!args[0].equals("-help")) {
                    out.println("Invalid option: " + args[0]);
                }
                printHelp(out);
                return -1;
            }
        }
    }

    private static int create(PrintStream out, String[] args) {
        if (args.length == 0) {
            out.println("No arguments!");
            out.println("Usage:");
            out.println("[output csv file] [input file/directory] [input file/directory]...");
            return -1;
        }

        Path outputFile;
        try {
            outputFile = Path.of(args[0]);
        } catch (InvalidPathException ex) {
            out.println("Invalid output file!");
            out.println(ex.getLocalizedMessage());
            ex.printStackTrace(out);
            return -1;
        }

        try {
            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);

                if (!Files.exists(parent)) {
                    throw new IOException("Reason unknown");
                }
            }
        } catch (IOException ex) {
            out.println("Failed to create output file directories!");
            out.println(ex.getLocalizedMessage());
            ex.printStackTrace(out);
            return -1;
        }

        if (Files.isDirectory(outputFile)) {
            out.println("Output file is a directory!");
            return -1;
        }

        Path[] inputFiles = new Path[args.length - 1];
        for (int i = 1; i < args.length; i++) {
            try {
                inputFiles[i - 1] = Path.of(args[i]);
            } catch (InvalidPathException ex) {
                out.println("Invalid input file! (index " + (i - 1) + ")");
                out.println(ex.getLocalizedMessage());
                ex.printStackTrace(out);
                return -1;
            }
        }

        CreateCommand create = new CreateCommand(inputFiles, outputFile);
        return create.run();
    }

    private static int validate(PrintStream out, String[] args) {
        if (args.length == 0) {
            out.println("No arguments!");
            out.println("Usage:");
            out.println("[input csv file] [root directory]");
            return -1;
        }

        Path inputFile;
        try {
            inputFile = Path.of(args[0]);
        } catch (InvalidPathException ex) {
            out.println("Invalid input file!");
            out.println(ex.getLocalizedMessage());
            ex.printStackTrace(out);
            return -1;
        }
        if (!Files.exists(inputFile)) {
            out.println("Input file does not exists!");
            return -1;
        }
        if (!Files.isRegularFile(inputFile)) {
            out.println("Input file is not a file!");
            return -1;
        }

        Path rootDirectory;
        if (args.length == 1) {
            rootDirectory = Path.of("");
        } else {
            try {
                rootDirectory = Path.of(args[1]);
            } catch (InvalidPathException ex) {
                out.println("Invalid root directory!");
                out.println(ex.getLocalizedMessage());
                ex.printStackTrace(out);
                return -1;
            }
        }
        if (!Files.exists(rootDirectory)) {
            out.println("Root directory does not exists!");
            return -1;
        }
        if (!Files.isDirectory(rootDirectory)) {
            out.println("Root directory is not a directory!");
            return -1;
        }
        
        ValidateCommand validate = new ValidateCommand(inputFile, rootDirectory);
        return validate.run();
    }

    private CLInterface() {

    }

}
