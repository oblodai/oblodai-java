package com.oblodai.core;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;

/**
 * A binary answer from a {@code bare} route: a rendered PDF or CSV document.
 *
 * @param bytes the file contents
 * @param contentType MIME type the gateway reported
 * @param filename name from {@code Content-Disposition}, when the gateway sent one: a bare file
 *     name only (no directories, no control characters, never {@code .} or {@code ..})
 */
public record FileResult(byte[] bytes, String contentType, String filename) {

    /** @return size of the file in bytes */
    public int size() {
        return bytes.length;
    }

    /**
     * Saves the document to a NEW file, readable by the owner only (0600 where the file system has
     * POSIX permissions). An existing file is never overwritten: that is a {@link
     * java.nio.file.FileAlreadyExistsException}; see {@link #writeTo(Path, boolean)}.
     *
     * @param path where to write it
     * @return the path
     * @throws IOException when it cannot be written, or the file exists
     */
    public Path writeTo(Path path) throws IOException {
        return writeTo(path, false);
    }

    /**
     * Saves the document, created owner-only (0600 where supported).
     *
     * @param path where to write it
     * @param replace whether an existing file may be replaced
     * @return the path
     * @throws IOException when it cannot be written, or the file exists and {@code replace} is false
     */
    public Path writeTo(Path path, boolean replace) throws IOException {
        if (replace) Files.deleteIfExists(path);
        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            Files.createFile(
                    path, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        } else {
            Files.createFile(path);
        }
        return Files.write(path, bytes, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @Override
    public String toString() {
        return "FileResult(contentType=" + contentType + ", filename=" + filename + ", size=" + size() + ")";
    }
}
