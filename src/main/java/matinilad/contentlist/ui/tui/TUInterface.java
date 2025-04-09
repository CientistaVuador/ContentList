package matinilad.contentlist.ui.tui;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import matinilad.contentlist.ContentEntry;
import matinilad.contentlist.ContentFileSystem;
import matinilad.contentlist.ContentListUtils;
import matinilad.contentlist.ContentPath;
import matinilad.contentlist.ContentType;
import matinilad.contentlist.ui.UIUtils;

/**
 *
 * @author Cien
 */
public class TUInterface {

    private static void printHelp(PrintStream out) {
        out.println("Available commands:");
        out.println("-open [input csv file]");
    }

    public static void run(InputStream in, PrintStream out, String[] args) {
        Objects.requireNonNull(in, "in is null");
        Objects.requireNonNull(out, "out is null");
        if (args == null || args.length == 0) {
            out.println("No arguments!");
            printHelp(out);
            return;
        }
        switch (args[0]) {
            case "-open" -> {
                open(in, out, Arrays.copyOfRange(args, 1, args.length));
            }
            default -> {
                if (!args[0].equals("-help")) {
                    out.println("Invalid option: " + args[0]);
                }
                printHelp(out);
            }
        }
    }

    private static void open(InputStream in, PrintStream out, String[] args) {
        if (args.length == 0) {
            out.println("No arguments!");
            out.println("Usage:");
            out.println("[input csv file]");
            return;
        }

        Path inputPath;
        try {
            inputPath = Path.of(args[0]);
        } catch (InvalidPathException ex) {
            out.println("Invalid input file!");
            out.println(ex.getLocalizedMessage());
            ex.printStackTrace(out);
            return;
        }

        if (!Files.exists(inputPath)) {
            out.println("Input file does not exists!");
            return;
        }

        if (!Files.isRegularFile(inputPath)) {
            out.println("Input file is not a file!");
            return;
        }

        out.println("Loading...");

        ContentFileSystem fs;
        try {
            fs = ContentFileSystem.of(inputPath.toFile());
        } catch (IOException | InterruptedException ex) {
            out.println("Failed to load input file!");
            out.println(ex.getLocalizedMessage());
            ex.printStackTrace(out);
            return;
        }

        out.println("Done!");

        runTerminal(in, out, fs);
    }

    private static ContentEntry readEntry(PrintStream out, ContentFileSystem fs, ContentPath path) {
        ContentEntry entry = fs.readEntry(path);
        if (entry == null) {
            out.println("Entry not found in CSV file.");
            out.println(path);
        }
        return entry;
    }

    private static ContentPath parseContentPath(PrintStream out, String argument) {
        ContentPath path;
        try {
            path = ContentPath.of(argument);
        } catch (IllegalArgumentException ex) {
            out.println(argument + " is not a valid path!");
            out.println(ex.getLocalizedMessage());
            ex.printStackTrace(out);
            return null;
        }
        return path;
    }

    private static void printTerminalHelp(PrintStream out) {
        out.println("Available commands:");
        out.println("ls");
        out.println(" List the files in the current directory");
        out.println("cd [directory]");
        out.println(" Changes the current directory");
        out.println("about [file/directory]");
        out.println(" Shows information about a file/directory");
        out.println("search [name]");
        out.println(" Lists all files/directories in the current directory, including subdirectories, containing a name");
        out.println(" Variations:");
        out.println("  csearch - Case sensitive");
        out.println("  esearch - Exact name");
        out.println("  ecsearch - Exact name, Case sensitive");
    }

    private static void runTerminal(InputStream in, PrintStream out, ContentFileSystem fs) {
        Scanner scanner = new Scanner(in);

        out.println("Content List File System Terminal v1.0");
        ContentEntry rootEntry = readEntry(out, fs, ContentPath.of("/"));
        if (rootEntry != null) {
            out.println(UIUtils.formatBytes(rootEntry.getSize()));
            out.println(rootEntry.getFiles() + " Files, " + rootEntry.getDirectories() + " Directories");
            out.println("Created on " + UIUtils.asShortLocalizedDateTime(rootEntry.getCreated()));
        }
        out.println("Welcome!");

        ContentPath workDirectory = ContentPath.of("/");

        while (true) {
            out.print("]");
            String input;
            try {
                input = scanner.nextLine();
            } catch (NoSuchElementException ex) {
                break;
            }
            String[] split = input.split(" ", 2);

            String command = split[0];
            String argument;
            if (split.length > 1) {
                argument = split[1].trim();
                if (argument.isBlank()) {
                    argument = null;
                }
            } else {
                argument = null;
            }

            switch (command) {
                case "ls" -> {
                    if (argument != null) {
                        out.println("Tip: ls has no arguments, use cd for navigation.");
                    }
                    ContentPath[] files = fs.listFiles(workDirectory, true);
                    for (ContentPath file : files) {
                        out.print(file.relative(workDirectory).toString());
                        if (!file.getName().equals(".")
                                && !file.getName().equals("..")
                                && fs.isDirectory(file)) {
                            out.print("/.");
                        }
                        out.println();
                    }
                }
                case "cd" -> {
                    if (argument == null) {
                        out.println("Usage: cd [directory]");
                        continue;
                    }
                    ContentPath newWorkDirectory = parseContentPath(out, argument);
                    if (newWorkDirectory == null) {
                        continue;
                    }
                    if (newWorkDirectory.isRelative()) {
                        newWorkDirectory = workDirectory.resolve(newWorkDirectory);
                    }
                    if (!fs.exists(newWorkDirectory)) {
                        out.println(argument + " does not exists!");
                        continue;
                    }
                    if (!fs.isDirectory(newWorkDirectory)) {
                        out.println(argument + " is not a directory!");
                        continue;
                    }
                    workDirectory = fs.toRealPath(newWorkDirectory);
                    out.println(workDirectory);
                }
                case "about" -> {
                    if (argument == null) {
                        out.println("Usage: about [file]");
                        continue;
                    }
                    ContentPath p = parseContentPath(out, argument);
                    if (p == null) {
                        continue;
                    }
                    if (p.isRelative()) {
                        p = workDirectory.resolve(p);
                    }
                    if (!fs.exists(p)) {
                        out.println(argument + " does not exists!");
                        continue;
                    }
                    ContentEntry entry = readEntry(out, fs, p);
                    if (entry == null) {
                        continue;
                    }
                    out.println("Properties of:");
                    out.println(entry.getPath().toString());
                    out.println(" Type: " + entry.getType());
                    out.println(" Created: " + UIUtils.asShortLocalizedDateTime(entry.getCreated()));
                    out.println(" Modified: " + UIUtils.asShortLocalizedDateTime(entry.getModified()));
                    out.println(" Size: " + UIUtils.formatBytes(entry.getSize()));
                    if (entry.getType().equals(ContentType.DIRECTORY)) {
                        out.println("  " + entry.getFiles() + " Files, " + entry.getDirectories() + " Directories");
                    }
                    byte[] sha256 = entry.getSha256();
                    byte[] sample = entry.getSample();
                    if (sha256 != null) {
                        out.println(" SHA256: " + ContentListUtils.toHexString(sha256));
                    }
                    if (sample != null) {
                        out.println(" Sample: " + ContentListUtils.toHexString(sample));
                    }
                }
                case "search", "csearch", "ecsearch", "esearch" -> {
                    if (argument == null) {
                        out.println("Usage: " + command + " [name]");
                        continue;
                    }
                    boolean caseSensitive = command.equals("csearch") || command.equals("ecsearch");
                    boolean exact = command.equals("esearch") || command.equals("ecsearch");
                    ContentPath[] p;
                    try {
                        p = fs.search(workDirectory, argument, caseSensitive, exact, true);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                    if (p.length == 0) {
                        out.println("No files found for '" + argument + "'!");
                        continue;
                    }
                    for (ContentPath e : p) {
                        out.print(e.relative(workDirectory));
                        if (fs.isDirectory(e)) {
                            out.println("/.");
                        } else {
                            out.println();
                        }
                    }
                    out.println(p.length + " Files found for '" + argument + "'!");
                }

                default -> {
                    if (!command.equals("help")) {
                        out.println("Invalid command: " + command);
                    }
                    printTerminalHelp(out);
                }
            }
        }
    }

    private TUInterface() {

    }
}
