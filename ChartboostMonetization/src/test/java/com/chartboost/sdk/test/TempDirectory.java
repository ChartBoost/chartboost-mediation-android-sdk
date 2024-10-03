package com.chartboost.sdk.test;

import java.io.File;
import java.io.IOException;

/*
    TempDirectory creates a temporary directory on instantiation and deletes it on close.

    Deletion does not follow directory symlinks.

    Please explicitly delete anything you've created in your temp directory that would
    require special measures to delete (permissions, say), or else close() will throw.
 */
public class TempDirectory implements AutoCloseable {
    public final File directory;

    public TempDirectory() {
        try {
            this.directory = new File(pickDirectoryName());
            init();
        } catch (IOException ex) {
            throw new Error(ex);
        }
    }

    private void init() throws IOException {
        if (directory.exists())
            throw new IOException("Temp directory already exists: " + directory);
        if (!directory.mkdirs())
            throw new IOException("failed to create " + directory);
    }

    @Override
    public void close() {
        directory.delete();
    }

    private static String pickDirectoryName() throws IOException {
        File tempFile = File.createTempFile("unittest", null);

        String canonicalPath = tempFile.getCanonicalPath();
        String dirName = canonicalPath + ".d";

        if (!tempFile.delete())
            throw new IOException("Unable to delete " + canonicalPath);

        return dirName;
    }
}
