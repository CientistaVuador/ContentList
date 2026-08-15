/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <https://unlicense.org>
 */
package matinilad.contentlist.ui.cli;

import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;
import matinilad.contentlist.phantomfs.PhantomFileSystem;
import matinilad.contentlist.phantomfs.PhantomPath;
import matinilad.contentlist.phantomfs.entry.FileEntry;
import matinilad.contentlist.phantomfs.entry.FileEntryFactory;
import matinilad.contentlist.phantomfs.entry.FileEntryMetadata;
import matinilad.contentlist.phantomfs.entry.FileEntryWriter;
import matinilad.contentlist.phantomfs.utils.PasswordEncryption;
import matinilad.contentlist.phantomfs.utils.PathStream;
import matinilad.contentlist.phantomfs.utils.TempFileList;
import matinilad.contentlist.ui.UIUtils;

/**
 *
 * @author Cien
 */
public class CreateCommand {

    private static void printHelp(PrintStream out) {
        out.println("Arguments (Can be used in any order):");
        out.println("-out [file] - Sets the output file [REQUIRED!]");
        out.println("-in [file] - Adds a input file");
        out.println("-inDir [directory] - Adds the contents of a directory as input");
        out.println("-name [name] - Sets the name of the list");
        out.println("-author [author] - Sets the author of the list");
        out.println("-desc [description] - Sets the description of the list");
        out.println("-verbose - Enables verbose mode, otherwise only errors will be displayed");
        out.println("-encrypt - Encrypts the file with a password");
        out.println("-replace - Replaces the output file without asking, if it already exists");
        out.println("-hidden - Includes hidden files");
        out.println("-sampleSize [size] - Sets the sample size for files");
        out.println("-disable [type/timestamps/size/filesAndDirectories/sha256/sample/metadata/all]");
        out.println("  Blocks a file attribute from being written into the csv");
        out.println("  A comma can be used for multiple attributes in a single argument");
        out.println("  e.g.: -disable sha256,sample");
    }

    public static int run(InputStream in, PrintStream out, String[] args) throws Exception {
        if (args.length == 0) {
            printHelp(out);
            return 0;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("-help")) {
            printHelp(out);
            return 0;
        }

        Scanner scanner = new Scanner(in);

        Path outputFile = null;
        List<Path> inputFiles = new ArrayList<>();
        String name = null;
        String author = null;
        String description = null;
        boolean verbose = false;
        boolean encrypt = false;
        boolean replace = false;
        boolean hiddenFiles = false;
        int sampleSize = 32;
        FileEntryWriter.Flags flags = new FileEntryWriter.Flags();

        for (int i = 0; i < args.length; i++) {
            String argument = args[i].toLowerCase();
            String nextArgument = null;
            if ((i + 1) < args.length) {
                nextArgument = args[i + 1];
            }

            switch (argument) {
                case "-verbose" -> {
                    verbose = true;
                    continue;
                }
                case "-encrypt" -> {
                    encrypt = true;
                    continue;
                }
                case "-replace" -> {
                    replace = true;
                    continue;
                }
                case "-hidden" -> {
                    hiddenFiles = true;
                    continue;
                }
            }

            if (nextArgument == null) {
                out.println("A argument is required for " + argument);
                out.println("Type -help for a list of arguments");
                return -1;
            }

            i++;

            switch (argument) {
                case "-out" -> {
                    if (outputFile != null) {
                        out.println("Attempting to set output file twice!");
                        return -1;
                    }
                    try {
                        outputFile = Path.of(nextArgument).toAbsolutePath().normalize();
                    } catch (InvalidPathException ex) {
                        out.println("Invalid output path: " + nextArgument);
                        ex.printStackTrace(out);
                        return -1;
                    }
                }
                case "-in" -> {
                    try {
                        inputFiles.add(Path.of(nextArgument));
                    } catch (InvalidPathException ex) {
                        out.println("Invalid input path: " + nextArgument);
                        ex.printStackTrace(out);
                        return -1;
                    }
                }
                case "-indir" -> {
                    Path directory;
                    try {
                        directory = Path.of(nextArgument);
                    } catch (InvalidPathException ex) {
                        out.println("Invalid input directory path: " + nextArgument);
                        ex.printStackTrace(out);
                        return -1;
                    }
                    if (!Files.isDirectory(directory)) {
                        out.println("Not a directory: " + directory);
                        return -1;
                    }
                    try {
                        inputFiles.addAll(Files.list(directory).toList());
                    } catch (IOException | UncheckedIOException ex) {
                        out.println("Failed to add contents of " + directory);
                        ex.printStackTrace(out);
                        return -1;
                    }
                }
                case "-name", "-author", "-desc", "-description" -> {
                    switch (argument) {
                        case "-name" -> {
                            name = nextArgument;
                        }
                        case "-author" -> {
                            author = nextArgument;
                        }
                        case "-desc", "-description" -> {
                            description = nextArgument;
                        }
                    }
                }
                case "-samplesize" -> {
                    try {
                        sampleSize = Integer.parseInt(nextArgument);
                    } catch (NumberFormatException ex) {
                        out.println("Not a integer: " + nextArgument);
                        ex.printStackTrace(out);
                        return -1;
                    }
                    if (sampleSize < 0) {
                        out.println("Sample size is negative");
                        return -1;
                    }
                }
                case "-disable" -> {
                    String[] split = nextArgument.split(",");
                    if (split.length == 0) {
                        out.println("Disable argument is empty: " + nextArgument);
                        return -1;
                    }
                    for (String s : split) {
                        switch (s.toLowerCase()) {
                            case "type" -> {
                                flags.setTypeEnabled(false);
                            }
                            case "timestamps" -> {
                                flags.setTimestampsEnabled(false);
                            }
                            case "size" -> {
                                flags.setSizeEnabled(false);
                            }
                            case "filesanddirectories" -> {
                                flags.setFilesAndDirectoriesEnabled(false);
                            }
                            case "sha256" -> {
                                flags.setSha256Enabled(false);
                            }
                            case "sample" -> {
                                flags.setSampleEnabled(false);
                            }
                            case "metadata" -> {
                                flags.setMetadataEnabled(false);
                            }
                            case "all" -> {
                                flags.setAllDisabled(true);
                            }
                            default -> {
                                out.println("Unknown disable option: " + s);
                                return -1;
                            }
                        }
                    }
                }
            }
        }

        if (outputFile == null) {
            out.println("Output file must be set!");
            return -1;
        }

        Path filename = outputFile.getFileName();
        if (filename == null) {
            out.println("Output file has no name!");
            return -1;
        }

        if (!filename.toString().contains(".")) {
            String ext = (encrypt ? "bin" : "csv");
            String newName = filename.toString() + "." + ext;
            if (outputFile.getParent() == null) {
                outputFile = outputFile.getFileSystem().getPath(newName);
            } else {
                outputFile = outputFile.getParent().resolve(newName);
            }
        }

        if (Files.exists(outputFile)) {
            if (Files.isDirectory(outputFile)) {
                out.println("Output file is a directory!");
                return -1;
            }

            if (!replace) {
                out.println("Replace " + outputFile + " ?");
                out.print("[Y/N:]");
                String response = scanner.nextLine().toLowerCase();
                if (!response.equals("y") && !response.equals("yes")) {
                    out.println("Operation canceled");
                    return 0;
                }
            }
        }

        if (sampleSize == 0) {
            flags.setSampleEnabled(false);
        } else if (!flags.isSampleEnabled()) {
            sampleSize = 0;
        }

        TempFileList temp = new TempFileList();
        try {
            if (outputFile.getParent() != null) {
                temp.createDirectories(outputFile.getParent());
            }
            try (OutputStream fileOut = temp.newOutputStream(outputFile)) {
                byte[] userSalt = null;
                char[] password = null;
                try {
                    if (encrypt) {
                        Console console = System.console();
                        if (console == null) {
                            out.println("Console is not available for password reading");
                            return -1;
                        }
                        while (true) {
                            char[] pass = console.readPassword("[%s]", "Password:");
                            try {
                                if (pass == null || pass.length == 0) {
                                    out.println("Password is empty, try again");
                                    continue;
                                }
                                char[] confirmPass = console.readPassword("[%s]", "Confirm Password:");
                                try {
                                    if (!Arrays.equals(pass, confirmPass)) {
                                        out.println("Passwords are not equal, try again");
                                        continue;
                                    }
                                } finally {
                                    if (confirmPass != null) {
                                        Arrays.fill(confirmPass, '\0');
                                    }
                                }
                                password = pass.clone();
                            } finally {
                                if (pass != null) {
                                    Arrays.fill(pass, '\0');
                                }
                            }
                            break;
                        }
                        out.println("Type random characters below or leave empty to skip.");
                        out.print("[Salt:]");
                        String salt = scanner.nextLine();
                        if (salt != null && salt.length() > 0) {
                            userSalt = salt.getBytes(StandardCharsets.UTF_8);
                        }
                    }

                    boolean finalVerbose = verbose;

                    AtomicInteger errorCount = new AtomicInteger(0);

                    PhantomFileSystem fs = new PhantomFileSystem();
                    FileEntryFactory factory = new FileEntryFactory();
                    factory.setSampleSize(sampleSize);
                    factory.setSha256Enabled(flags.isSha256Enabled());
                    PathStream stream = new PathStream(inputFiles.toArray(Path[]::new), hiddenFiles);
                    stream.stream((e) -> {
                        Path file = e.getPath();
                        try {
                            if (e.getError() != null) {
                                throw e.getError();
                            }
                            if (finalVerbose) {
                                if (Files.isRegularFile(file)) {
                                    long size = Files.size(file);
                                    out.print("[" + UIUtils.formatBytesShort(size) + "] ");
                                }
                                out.println(file.toString());
                            }
                            FileEntry entry = factory.newFileEntry(e.getRoot(), file);
                            fs.writeEntry(entry);
                        } catch (Throwable t) {
                            errorCount.incrementAndGet();
                            out.println("File rejected: " + file);
                            t.printStackTrace(out);
                        }
                    });
                    fs.validate();

                    FileEntry rootEntry = fs.getEntry(PhantomPath.of("/"));
                    FileEntryMetadata meta = rootEntry.getMetadata();
                    if (name != null) {
                        meta.writeString(FileEntry.METADATA_NAME, name);
                    }
                    if (author != null) {
                        meta.writeString(FileEntry.METADATA_AUTHOR, author);
                    }
                    if (description != null) {
                        meta.writeString(FileEntry.METADATA_DESCRIPTION, description);
                    }

                    if (verbose) {
                        out.println("Total size: " + UIUtils.formatBytes(rootEntry.getSize()));
                        out.println("Files: " + rootEntry.getFiles());
                        out.println("Directories: " + rootEntry.getDirectories());
                        out.println("Errors: " + errorCount.get());
                    } else {
                        if (errorCount.get() != 0) {
                            out.println("Errors: " + errorCount.get());
                        }
                    }
                    
                    ByteArrayOutputStream arrayOut = new ByteArrayOutputStream();

                    OutputStream toOutput = fileOut;
                    if (password != null) {
                        toOutput = new GZIPOutputStream(arrayOut);
                    }

                    try (FileEntryWriter writer = new FileEntryWriter(new OutputStreamWriter(toOutput, StandardCharsets.UTF_8), flags)) {
                        FileEntry[] entries = fs.listEntries();
                        for (FileEntry e : entries) {
                            writer.writeFileEntry(e);
                        }
                    }

                    if (password != null) {
                        toOutput.close();

                        fileOut.write(PasswordEncryption.encrypt(arrayOut.toByteArray(), userSalt, password));
                    }

                    return errorCount.get();
                } finally {
                    if (password != null) {
                        Arrays.fill(password, '\0');
                    }
                }
            }
        } catch (Throwable t) {
            temp.deleteFiles();
            throw t;
        }
    }

}
