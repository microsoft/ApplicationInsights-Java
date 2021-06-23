package com.microsoft.applicationinsights.internal.persistence;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

public class LocalFileCache {

    /**
     * Track a list of active filenames persisted on disk.
     * FIFO (First-In-First-Out) read will avoid an additional sorting at every read.
     * Caveat: data loss happens when the app crashes.  filenames stored in this queue will be lost forever.
     * There isn't an unique way to identify each java app.  C# uses "User@processName" to identify each app, but
     * Java can't rely on process name since it's a system property that can be customized via the command line.
     * TODO (heya) need to uniquely identify each app and figure out how to retrieve data from the disk for each app.
     */
    private static final Queue<String> persistedFilesCache = new ConcurrentLinkedDeque<>();

    // Track the newly persisted filename to the concurrent hashmap.
    void addPersistedFilenameToMap(String filename) {
        persistedFilesCache.add(filename);
    }

    String poll() {
        return persistedFilesCache.poll();
    }

    // only used by tests
    Queue<String> getPersistedFilesCache() {
        return persistedFilesCache;
    }
}
