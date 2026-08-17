package matinilad.contentlist.ui.tui;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import matinilad.contentlist.phantomfs.entry.FileEntry;
import matinilad.contentlist.phantomfs.PhantomFileSystem;
import matinilad.contentlist.phantomfs.PhantomPath;
import matinilad.contentlist.phantomfs.entry.FileEntryReader;
import matinilad.contentlist.phantomfs.utils.PasswordEncryption;
import matinilad.contentlist.ui.UIUtils;
import matinilad.contentlist.ui.tui.commands.AboutCommand;
import matinilad.contentlist.ui.tui.commands.ChangeDirectoryCommand;
import matinilad.contentlist.ui.tui.commands.HelpCommand;
import matinilad.contentlist.ui.tui.commands.InfoCommand;
import matinilad.contentlist.ui.tui.commands.LinesCommand;
import matinilad.contentlist.ui.tui.commands.ListCommand;
import matinilad.contentlist.ui.tui.commands.MetadataCommand;
import matinilad.contentlist.ui.tui.commands.PageCommand;
import matinilad.contentlist.ui.tui.commands.RootCommand;
import matinilad.contentlist.ui.tui.commands.SearchCommand;
import matinilad.contentlist.ui.tui.commands.SpaceUnitCommand;
import matinilad.contentlist.ui.tui.commands.SystemCommand;
import matinilad.contentlist.ui.tui.commands.ValidateSystemCommand;

/**
 *
 * @author Cien
 */
public class TUInterface {

    private static void printHelp(PrintStream out) {
        out.println("Available commands:");
        out.println("-open [csv file] - Opens a file");
        out.println("-decrypt [file] - Opens a encrypted file");
    }

    public static void run(InputStream in, PrintStream out, String[] args) {
        Objects.requireNonNull(in, "in is null");
        Objects.requireNonNull(out, "out is null");
        if (args == null || args.length == 0) {
            out.println("No arguments!");
            printHelp(out);
            return;
        }
        switch (args[0].toLowerCase()) {
            case "-open", "-decrypt" -> {
                open(in, out, Arrays.copyOfRange(args, 1, args.length), args[0].equalsIgnoreCase("-decrypt"));
            }
            default -> {
                if (!args[0].equals("-help")) {
                    out.println("Invalid option: " + args[0]);
                }
                printHelp(out);
            }
        }
    }

    private static void open(InputStream in, PrintStream out, String[] args, boolean decrypt) {
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
        
        Scanner scanner = new Scanner(in);
        if (!decrypt && inputPath.getFileName().toString().toLowerCase().endsWith(".bin")) {
            out.println("Is "+inputPath.toString()+" encrypted?");
            out.print("[Y/N:]");
            String r = scanner.nextLine();
            if (r.equalsIgnoreCase("y") || r.equalsIgnoreCase("yes")) {
                decrypt = true;
            }
        }
        
        out.println("Loading...");

        PhantomFileSystem fs = new PhantomFileSystem();
        try {
            InputStream input;
            if (decrypt) {
                Console console = System.console();
                if (console == null) {
                    out.println("Console is not available for password reading");
                    return;
                }
                byte[] data = Files.readAllBytes(inputPath);
                while (true) {
                    char[] password = console.readPassword("[%s]", "Password:");
                    if (password == null || password.length == 0) {
                        out.println("Password is empty, try again");
                        continue;
                    }
                    try {
                        input = new GZIPInputStream(new ByteArrayInputStream(PasswordEncryption.decrypt(data, password)));
                        break;
                    } catch (PasswordEncryption.IncorrectPasswordException ex) {
                        out.println("Incorrect password or corrupted file, try again");
                    }
                }
            } else {
                input = Files.newInputStream(inputPath);
            }
            
            try (FileEntryReader reader = new FileEntryReader(new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)))) {
                FileEntry entry;
                while ((entry = reader.readEntry()) != null) {
                    fs.writeEntry(entry);
                }
            }
        } catch (IOException | IllegalArgumentException ex) {
            out.println("Failed to load input file!");
            out.println(ex.getLocalizedMessage());
            ex.printStackTrace(out);
            return;
        }

        out.println("Done!");
        
        runTerminal(scanner, in, out, fs);
    }

    private static FileEntry readEntry(PrintStream out, PhantomFileSystem fs, PhantomPath path) {
        FileEntry entry = fs.getEntry(path);
        if (entry == null) {
            out.println("Entry not found in CSV file.");
            out.println(path);
        }
        return entry;
    }

    private static void runTerminal(Scanner scanner, InputStream in, PrintStream out, PhantomFileSystem fs) {
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
        state.setDirectOutput(out);
        
        Commands commands = new Commands();

        commands.addCommand(new HelpCommand());
        commands.addCommand(new PageCommand());
        commands.addCommand(new LinesCommand());

        commands.addCommand(new ListCommand());
        commands.addCommand(new ChangeDirectoryCommand());
        commands.addCommand(new AboutCommand());

        commands.addCommand(new SearchCommand());
        commands.addCommand(new SearchCommand.CaseSensitive());
        commands.addCommand(new SearchCommand.Exact());
        commands.addCommand(new SearchCommand.ExactCaseSensitive());

        commands.addCommand(new MetadataCommand());
        commands.addCommand(new InfoCommand.Name());
        commands.addCommand(new InfoCommand.Author());
        commands.addCommand(new InfoCommand.Description());
        commands.addCommand(new SpaceUnitCommand());

        commands.addCommand(new RootCommand());
        commands.addCommand(new SystemCommand.Exists());
        commands.addCommand(new SystemCommand.Open());
        commands.addCommand(new SystemCommand.OpenDirectory());
        commands.addCommand(new SystemCommand.Copy());
        commands.addCommand(new SystemCommand.Trash());
        commands.addCommand(new ValidateSystemCommand());

        state.setCommands(commands);

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

            Command newCommand = commands.getCommand(command);
            if (newCommand != null) {
                try {
                    String commandOutput = newCommand.execute(argument).trim();
                    if (!newCommand.isDirectOutputEnabled()) {
                        Command pageCommand = state.getCommands().getCommand("pg");
                        if (pageCommand != null) {
                            state.setCommandOutput(commandOutput);
                            if (state.getNumberOfPages() > 0) {
                                pageCommand.execute("1");
                            }
                        } else {
                            out.println(commandOutput);
                        }
                    }
                } catch (CommandException ex) {
                    out.println(ex.getLocalizedMessage().trim());
                    if (ex.getCause() != null) {
                        out.println("Exception:");
                        ex.getCause().printStackTrace(out);
                    }
                }
            } else {
                out.println("Unknown command: " + command + "\nType help for a list of commands.");
            }
        }
    }

    private TUInterface() {

    }
}
