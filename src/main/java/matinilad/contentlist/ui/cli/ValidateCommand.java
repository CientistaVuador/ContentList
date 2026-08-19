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

import java.io.ByteArrayInputStream;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import matinilad.contentlist.phantomfs.entry.FileEntry;
import matinilad.contentlist.phantomfs.entry.FileEntryReader;
import matinilad.contentlist.phantomfs.entry.FileEntryValidator;
import matinilad.contentlist.phantomfs.entry.FileEntryValidatorResult;
import matinilad.contentlist.phantomfs.utils.EncryptedInputStream;
import matinilad.contentlist.ui.UIUtils;

/**
 *
 * @author Cien
 */
public class ValidateCommand {

    private static void printHelp(PrintStream out) {
        out.println("Arguments (Can be used in any order):");
        out.println("-in [input file] - Sets the input file [REQUIRED!]");
        out.println("-root [root directory] - Sets the root directory [REQUIRED!]");
        out.println("-verbose - Enables verbose mode, otherwise only errors will be displayed");
        out.println("-decrypt - Use this if the file is encrypted");
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

        Path inputFile = null;
        Path rootDirectory = null;
        boolean verbose = false;
        boolean decrypt = false;

        Scanner scanner = new Scanner(in);

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
                case "-decrypt" -> {
                    decrypt = true;
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
                case "-in" -> {
                    try {
                        inputFile = Path.of(nextArgument).toRealPath();
                        if (!Files.isRegularFile(inputFile)) {
                            throw new IOException("not a valid file: " + inputFile);
                        }
                    } catch (IOException | InvalidPathException ex) {
                        out.println("Invalid input file: " + nextArgument);
                        ex.printStackTrace(out);
                        return -1;
                    }
                }
                case "-root" -> {
                    try {
                        rootDirectory = Path.of(nextArgument).toRealPath();
                        if (!Files.isDirectory(rootDirectory)) {
                            throw new IOException("not a valid directory: " + rootDirectory);
                        }
                    } catch (IOException | InvalidPathException ex) {
                        out.println("Invalid root directory: " + nextArgument);
                        ex.printStackTrace(out);
                        return -1;
                    }
                }
            }
        }

        if (inputFile == null) {
            out.println("Input file not set!");
            return -1;
        }

        if (rootDirectory == null) {
            out.println("Root directory not set!");
            return -1;
        }

        if (!decrypt && inputFile.getFileName().toString().toLowerCase().endsWith(".bin")) {
            out.println("Is " + inputFile.toString() + " encrypted?");
            out.print("[Y/N:]");
            String response = scanner.nextLine();
            if (response.equalsIgnoreCase("y") || response.equalsIgnoreCase("yes")) {
                decrypt = true;
            }
        }

        InputStream input = Files.newInputStream(inputFile);
        if (decrypt) {
            Console console = System.console();
            if (console == null) {
                out.println("Console is not available for password reading");
                return -1;
            }
            
            PushbackInputStream pushback = new PushbackInputStream(input, 512);
            byte[] sample = pushback.readNBytes(512);
            pushback.unread(sample);
            input = pushback;
            
            while (true) {
                char[] password = console.readPassword("[%s]", "Password:");
                try {
                    if (password == null || password.length == 0) {
                        out.println("Password is empty");
                        continue;
                    }

                    try {
                        EncryptedInputStream test = new EncryptedInputStream(new ByteArrayInputStream(sample), password);
                        test.readAllBytes();
                    } catch (EncryptedInputStream.IncorrectPasswordException ex) {
                        out.println("Incorrect password or corrupted file, try again");
                        continue;
                    } catch (IOException t) {
                        //ignore
                    }
                    
                    input = new GZIPInputStream(new EncryptedInputStream(input, password));
                    break;
                } finally {
                    if (password != null) {
                        Arrays.fill(password, '\0');
                    }
                }
            }
        }

        int errors = 0;

        try (FileEntryReader reader = new FileEntryReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            FileEntry entry;
            while ((entry = reader.readEntry()) != null) {
                FileEntryValidator validator = new FileEntryValidator(rootDirectory, entry);
                Path file = validator.getPath();
                try {
                    if (verbose) {
                        if (Files.isRegularFile(file)) {
                            out.println("[" + UIUtils.formatBytesShort(Files.size(file)) + "] " + file.toString());
                        } else {
                            out.println(file.toString());
                        }
                    }
                    FileEntryValidatorResult result = validator.validate();
                    if (!result.success()) {
                        throw new IOException(UIUtils.getFailureReason(result));
                    }
                } catch (IOException ex) {
                    errors++;
                    out.println("Failed: " + file.toString());
                    ex.printStackTrace(out);
                }
            }
        }

        if (verbose || errors != 0) {
            out.println("Errors: " + errors);
        }

        return errors;
    }

    private ValidateCommand() {

    }
}
