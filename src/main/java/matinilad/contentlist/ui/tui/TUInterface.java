package matinilad.contentlist.ui.tui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;
import matinilad.contentlist.phantomfs.entry.FileEntry;
import matinilad.contentlist.phantomfs.PhantomFileSystem;
import matinilad.contentlist.phantomfs.PhantomPath;
import matinilad.contentlist.phantomfs.entry.FileEntryReader;
import matinilad.contentlist.phantomfs.entry.FileEntryType;
import matinilad.contentlist.ui.UIUtils;
import matinilad.contentlist.ui.tui.commands.AboutCommand;
import matinilad.contentlist.ui.tui.commands.ChangeDirectoryCommand;
import matinilad.contentlist.ui.tui.commands.ListCommand;
import matinilad.contentlist.ui.tui.commands.SearchCommand;

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

        PhantomFileSystem fs = new PhantomFileSystem();
        try {
            try (FileEntryReader reader = new FileEntryReader(new BufferedReader(new InputStreamReader(Files.newInputStream(inputPath), StandardCharsets.UTF_8)))) {
                FileEntry entry;
                while ((entry = reader.readEntry()) != null) {
                    fs.writeEntry(entry);
                }
            }
        } catch (IOException ex) {
            out.println("Failed to load input file!");
            out.println(ex.getLocalizedMessage());
            ex.printStackTrace(out);
            return;
        }

        out.println("Done!");

        runTerminal(in, out, fs);
    }

    private static FileEntry readEntry(PrintStream out, PhantomFileSystem fs, PhantomPath path) {
        FileEntry entry = fs.getEntry(path);
        if (entry == null) {
            out.println("Entry not found in CSV file.");
            out.println(path);
        }
        return entry;
    }

    private static PhantomPath parseContentPath(PrintStream out, String argument) {
        PhantomPath path;
        try {
            path = PhantomPath.of(argument);
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

    private static void runTerminal(InputStream in, PrintStream out, PhantomFileSystem fs) {
        Scanner scanner = new Scanner(in);

        out.println(UIUtils.name() + " Terminal v" + UIUtils.version());
        FileEntry rootEntry = readEntry(out, fs, PhantomPath.of("/"));
        if (rootEntry != null) {
            out.println(UIUtils.formatBytes(rootEntry.getSize()));
            out.println(rootEntry.getFiles() + " Files, " + rootEntry.getDirectories() + " Directories");
            out.println("Created on " + UIUtils.asShortLocalizedDateTime(rootEntry.getCreated()));
        }
        out.println("Welcome!");

        TUIState state = new TUIState();
        state.setFileSystem(fs);

        Commands commands = new Commands();
        
        commands.addCommand(new ListCommand());
        commands.addCommand(new ChangeDirectoryCommand());
        commands.addCommand(new AboutCommand());
        
        commands.addCommand(new SearchCommand());
        commands.addCommand(new SearchCommand.CaseSensitive());
        commands.addCommand(new SearchCommand.Exact());
        commands.addCommand(new SearchCommand.ExactCaseSensitive());

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
                default -> {
                    Command newCommand = commands.getCommand(command);
                    if (newCommand != null) {
                        try {
                            out.println(newCommand.execute(state, argument).trim());
                        } catch (CommandException ex) {
                            out.println(ex.getLocalizedMessage().trim());
                            if (ex.getCause() != null) {
                                out.println("Exception:");
                                ex.getCause().printStackTrace(out);
                            }
                        }
                    } else {
                        if (!command.equals("help")) {
                            out.println("Invalid command: " + command);
                        }
                        printTerminalHelp(out);
                    }
                }
            }
        }
    }

    private TUInterface() {

    }
}
