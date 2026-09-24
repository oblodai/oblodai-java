package com.oblodai.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A binary answer from a {@code bare} route: a rendered PDF or CSV document.
 *
 * @param bytes the file contents
 * @param contentType MIME type the gateway reported
 * @param filename name from {@code Content-Disposition}, when the gateway sent one
 */
public record FileResult(byte[] bytes, String contentType, String filename) {

    /** @return size of the file in bytes */
    public int size() {
        return bytes.length;
    }

    /**
     * Saves the document.
     *
     * @param path where to write it
     * @return the path
     * @throws IOException when it cannot be written
     */
    public Path writeTo(Path path) throws IOException {
        return Files.write(path, bytes);
    }

    @Override
    public String toString() {
        return "FileResult(contentType=" + contentType + ", filename=" + filename + ", size=" + size() + ")";
    }
}
