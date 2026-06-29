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
package matinilad.contentlist.phantomfs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import matinilad.contentlist.phantomfs.entry.FileEntryValidator;
import matinilad.contentlist.phantomfs.entry.FileEntryValidatorResult;

/**
 *
 * @author Cien
 */
public class PhantomValidator {
    
    private final Path rootDirectory;
    private final PhantomFileSystem fileSystem;
    private FileEntryValidator.Factory validatorFactory = FileEntryValidator::new;

    public PhantomValidator(Path rootDirectory, PhantomFileSystem fileSystem) {
        this.rootDirectory = Objects.requireNonNull(rootDirectory, "root directory is null");
        this.fileSystem = Objects.requireNonNull(fileSystem, "file system is null");
    }

    public Path getRootDirectory() {
        return rootDirectory;
    }

    public PhantomFileSystem getFileSystem() {
        return fileSystem;
    }

    public FileEntryValidator.Factory getValidatorFactory() {
        return validatorFactory;
    }

    public void setValidatorFactory(FileEntryValidator.Factory validatorFactory) {
        if (validatorFactory == null) {
            validatorFactory = FileEntryValidator::new;
        }
        this.validatorFactory = validatorFactory;
    }

    private PhantomPath validatePath(PhantomPath validationPath) {
        validationPath = Objects.requireNonNull(validationPath, "validation path is null");
        if (validationPath.isRelative()) {
            throw new IllegalArgumentException("validation path is relative.");
        }
        validationPath = getFileSystem().toRealPath(validationPath);
        if (validationPath == null) {
            throw new IllegalArgumentException("validation path could not be resolved.");
        }
        return validationPath;
    }

    protected boolean onShouldInterrupt() throws IOException, InterruptedException {
        return Thread.interrupted();
    }

    protected void onEntryValidated(
            PhantomPath root, PhantomPath current,
            FileEntryValidatorResult result
    ) throws IOException, InterruptedException {

    }

    private int validate(PhantomPath root, PhantomPath path) throws IOException, InterruptedException {
        int failed = 0;

        if (onShouldInterrupt()) {
            throw new InterruptedException();
        }

        PhantomFileSystem fs = getFileSystem();

        FileEntryValidatorResult result = getValidatorFactory()
                .newFileEntryValidator(getRootDirectory(), fs.getEntry(path))
                .validate();

        if (!result.success()) {
            failed++;
        }

        onEntryValidated(root, path, result);

        if (fs.isDirectory(path)) {
            PhantomPath[] paths = fs.listFiles(path);

            for (PhantomPath p : paths) {
                failed += validate(root, p);
            }
        }

        return failed;
    }

    public int validate(PhantomPath root) throws IOException, InterruptedException {
        root = validatePath(root);
        return validate(root, root);
    }
}
