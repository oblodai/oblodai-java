package com.oblodai.core;

/**
 * A binary answer from a {@code bare} route: a rendered PDF or CSV document.
 *
 * @param bytes the file contents
 * @param contentType MIME type the gateway reported
 * @param filename name from {@code Content-Disposition}, when the gateway sent one
 */
public record FileResult(byte[] bytes, String contentType, String filename) {

    /** Size of the file in bytes. */
    public int size() {
        return bytes.length;
    }
}
