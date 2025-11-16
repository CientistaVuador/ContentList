package matinilad.contentlist.ui.cli;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import matinilad.contentlist.phantomfs.entry.FileEntry;
import matinilad.contentlist.phantomfs.PhantomCreator;
import matinilad.contentlist.phantomfs.PhantomValidator;
import static matinilad.contentlist.phantomfs.PhantomValidator.ValidatorReason.EXISTS;
import static matinilad.contentlist.phantomfs.PhantomValidator.ValidatorReason.HASH;
import static matinilad.contentlist.phantomfs.PhantomValidator.ValidatorReason.SAMPLE;
import static matinilad.contentlist.phantomfs.PhantomValidator.ValidatorReason.SIZE;
import static matinilad.contentlist.phantomfs.PhantomValidator.ValidatorReason.TYPE;
import matinilad.contentlist.phantomfs.PhantomPath;
import matinilad.contentlist.ui.UIUtils;

/**
 *
 * @author Cien
 */
public class CLInterface {

    private static void printHelp(PrintStream out) {
        out.println("Available commands:");
        out.println("-create [output csv file] [input file/directory] [input file/directory]...");
        out.println("-validate [input csv file] [base directory]");
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

        try {
            try (OutputStream o = Files.newOutputStream(outputFile)) {
                PhantomCreator.ContentListCallbacks callbacks = new PhantomCreator.ContentListCallbacks() {
                    @Override
                    public void onStart() throws IOException {
                        out.println("Initializing...");
                    }

                    @Override
                    public void onFileUnreadable(Path path) throws IOException {
                        out.println("Warning: '" + path.toString() + "' is unreadable.");
                    }

                    @Override
                    public void onFileDuplicated(Path path) throws IOException {
                        out.println("Warning: '" + path.toString() + "' is duplicated.");
                    }

                    private long nextUpdate = System.currentTimeMillis();

                    private PhantomPath currentPath = null;
                    private FileEntry lastEntry = null;
                    private long current = 0;
                    private long total = 0;
                    private int processedEntries = 0;

                    private void update() {
                        if (System.currentTimeMillis() < this.nextUpdate) {
                            return;
                        }
                        this.nextUpdate = System.currentTimeMillis() + 10000;

                        out.println("Total processed: " + this.processedEntries + " entries");
                        out.println(" Current Path: " + this.currentPath);
                        if (this.total == 0) {
                            out.println("  --% Done");
                        } else {
                            double p = ((this.current / ((double) this.total)) * 100);
                            out.println("  " + String.format("%.2f", p) + "% Done");
                        }
                    }

                    @Override
                    public void onEntryStart(PhantomPath path) throws IOException {
                        this.currentPath = path;
                        this.current = 0;
                        this.total = 0;

                        update();
                    }

                    @Override
                    public void onEntryFinish(FileEntry entry) throws IOException {
                        this.lastEntry = entry;
                        this.processedEntries++;

                        update();
                    }

                    @Override
                    public void onEntryProgressUpdate(long current, long total) throws IOException {
                        this.current = current;
                        this.total = total;

                        update();
                    }

                    @Override
                    public void onFinish() throws IOException {
                        out.println("Done! " + this.processedEntries + " Entries in total!");
                        out.println("Total Size: " + UIUtils.formatBytes(this.lastEntry.getSize()));
                        out.println(" " + this.lastEntry.getFiles() + " Files, " + this.lastEntry.getDirectories() + " Directories");
                    }
                };

                PhantomCreator.create(o, callbacks, inputFiles);
            }
        } catch (IOException | InterruptedException ex) {
            out.println("Operation failed!");
            out.println(ex.getLocalizedMessage());
            ex.printStackTrace(out);
            return;
        }
    }

    private static void validate(PrintStream out, String[] args) {
        if (args.length == 0) {
            out.println("No arguments!");
            out.println("Usage:");
            out.println("[input csv file] [base directory]");
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

        Path baseDirectory;
        if (args.length == 1) {
            baseDirectory = Path.of("");
        } else {
            try {
                baseDirectory = Path.of(args[1]);
            } catch (InvalidPathException ex) {
                out.println("Invalid base directory!");
                out.println(ex.getLocalizedMessage());
                ex.printStackTrace(out);
                return;
            }
        }
        if (!Files.exists(baseDirectory)) {
            out.println("Base directory does not exists!");
            return;
        }
        if (!Files.isDirectory(baseDirectory)) {
            out.println("Base directory is not a directory!");
            return;
        }

        try {
            PhantomValidator.validate(inputFile, baseDirectory, new PhantomValidator.ContentListValidatorCallbacks() {
                @Override
                public void onStart() throws IOException {
                    out.println("Initializing...");
                }

                private long nextUpdate = System.currentTimeMillis();

                private FileEntry currentEntry = null;
                boolean refused = false;
                private long current = 0;
                private long total = 0;
                private boolean ignoreWarnings = false;

                private int acceptedEntries = 0;
                private int refusedEntries = 0;

                private void update() {
                    if (System.currentTimeMillis() < this.nextUpdate) {
                        return;
                    }
                    this.nextUpdate = System.currentTimeMillis() + 10000;

                    out.println(this.acceptedEntries + " Accepted, " + this.refusedEntries + " Refused, " + (this.acceptedEntries + this.refusedEntries) + " Total.");
                    out.println(" Current Path: " + this.currentEntry.getPath().toString());
                    if (this.total == 0) {
                        out.println("  --% Done");
                    } else {
                        double p = ((this.current / ((double) this.total)) * 100);
                        out.println("  " + String.format("%.2f", p) + "% Done");
                    }
                }

                @Override
                public void onEntryStart(FileEntry entry, Path fileToValidate) throws IOException, InterruptedException {
                    this.currentEntry = entry;
                    this.refused = false;

                    update();
                }

                @Override
                public void onEntryAccepted(PhantomValidator.ValidatorReason reason, Object expected, Object found) throws IOException, InterruptedException {
                    update();
                }

                @Override
                public void onEntryRefused(PhantomValidator.ValidatorReason reason, Object expected, Object found) throws IOException, InterruptedException {
                    this.refused = true;

                    if (this.refusedEntries > 1000 && !this.ignoreWarnings) {
                        out.println("Too many refused entries!");
                        out.println("Warnings will now be ignored.");
                        this.ignoreWarnings = true;
                    }
                    if (!this.ignoreWarnings) {
                        HexFormat hex = HexFormat.of();
                        
                        out.println("Warning:");
                        out.println(" Path: " + this.currentEntry.getPath().toString());
                        out.println(" Was refused due to:");
                        switch (reason) {
                            case EXISTS -> {
                                out.println("  File does not exists.");
                            }
                            case TYPE -> {
                                out.println("  Expected type " + expected.toString() + ", found " + found.toString());
                            }
                            case SIZE -> {
                                out.println("  Expected size " + UIUtils.formatBytes((long) expected) + "; found " + UIUtils.formatBytes((long) found));
                            }
                            case SAMPLE -> {
                                out.println("  Expected sample " + hex.formatHex((byte[]) expected));
                                out.println("            found " + hex.formatHex((byte[]) found));
                            }
                            case HASH -> {
                                out.println("  Expected hash " + hex.formatHex((byte[]) expected));
                                out.println("          found " + hex.formatHex((byte[]) found));
                            }
                        }
                    }

                    update();
                }

                @Override
                public void onEntryProgressUpdate(long current, long total) throws IOException, InterruptedException {
                    this.current = current;
                    this.total = total;

                    update();
                }

                @Override
                public void onEntryFinish() throws IOException, InterruptedException {
                    if (this.refused) {
                        this.refusedEntries++;
                    } else {
                        this.acceptedEntries++;
                    }

                    update();
                }

                @Override
                public void onFinish() throws IOException, InterruptedException {
                    out.println("Done!");
                    out.println(" " + this.acceptedEntries + " Accepted, " + this.refusedEntries + " Refused");
                    out.println("  " + (this.acceptedEntries + this.refusedEntries) + " Total");
                }
            });
        } catch (IOException | InterruptedException ex) {
            out.println("Operation failed!");
            out.println(ex.getLocalizedMessage());
            ex.printStackTrace(out);
            return;
        }
    }

    private CLInterface() {

    }

}
