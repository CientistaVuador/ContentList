package matinilad.contentlist.ui.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

/**
 *
 * @author Cien
 */
public class CLInterface {

    private static void printHelp(PrintStream out) {
        out.println("Available commands:");
        out.println("-create [output csv file] [input file/directory] [input file/directory]...");
        out.println("-validate [input csv file] [root directory]");
    }

    public static void run(PrintStream out, String[] args) {
        Objects.requireNonNull(out, "out is null");
        if (args == null || args.length == 0) {
            out.println("No arguments!");
            printHelp(out);
            return;
        }
        switch (args[0]) {
            case "-create" -> {
                create(out, Arrays.copyOfRange(args, 1, args.length));
            }
            case "-validate" -> {
                validate(out, Arrays.copyOfRange(args, 1, args.length));
            }
            default -> {
                if (!args[0].equals("-help")) {
                    out.println("Invalid option: " + args[0]);
                }
                printHelp(out);
            }
        }
    }

    private static void create(PrintStream out, String[] args) {
        if (args.length == 0) {
            out.println("No arguments!");
            out.println("Usage:");
            out.println("[output csv file] [input file/directory] [input file/directory]...");
            return;
        }

        Path outputFile;
        try {
            outputFile = Path.of(args[0]);
        } catch (InvalidPathException ex) {
            out.println("Invalid output file!");
            out.println(ex.getLocalizedMessage());
            ex.printStackTrace(out);
            return;
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
            return;
        }

        if (Files.isDirectory(outputFile)) {
            out.println("Output file is a directory!");
            return;
        }

        Path[] inputFiles = new Path[args.length - 1];
        for (int i = 1; i < args.length; i++) {
            try {
                inputFiles[i - 1] = Path.of(args[i]);
            } catch (InvalidPathException ex) {
                out.println("Invalid input file! (index " + (i - 1) + ")");
                out.println(ex.getLocalizedMessage());
                ex.printStackTrace(out);
                return;
            }
        }

        CreateCommand create = new CreateCommand(out, inputFiles, outputFile);
        create.run();
    }

    private static void validate(PrintStream out, String[] args) {
        if (args.length == 0) {
            out.println("No arguments!");
            out.println("Usage:");
            out.println("[input csv file] [root directory]");
            return;
        }

        Path inputFile;
        try {
            inputFile = Path.of(args[0]);
        } catch (InvalidPathException ex) {
            out.println("Invalid input file!");
            out.println(ex.getLocalizedMessage());
            ex.printStackTrace(out);
            return;
        }
        if (!Files.exists(inputFile)) {
            out.println("Input file does not exists!");
            return;
        }
        if (!Files.isRegularFile(inputFile)) {
            out.println("Input file is not a file!");
            return;
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
                return;
            }
        }
        if (!Files.exists(rootDirectory)) {
            out.println("Root directory does not exists!");
            return;
        }
        if (!Files.isDirectory(rootDirectory)) {
            out.println("Root directory is not a directory!");
            return;
        }

        ValidateCommand validate = new ValidateCommand(out, inputFile, rootDirectory);
        validate.run();
    }

    private CLInterface() {

    }

}
