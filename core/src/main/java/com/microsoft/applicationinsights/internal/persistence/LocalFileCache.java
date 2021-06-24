/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.internal.persistence;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

public class LocalFileCache {

  /**
   * Track a list of active filenames persisted on disk. FIFO (First-In-First-Out) read will avoid
   * an additional sorting at every read. Caveat: data loss happens when the app crashes. filenames
   * stored in this queue will be lost forever. There isn't an unique way to identify each java app.
   * C# uses "User@processName" to identify each app, but Java can't rely on process name since it's
   * a system property that can be customized via the command line. TODO (heya) need to uniquely
   * identify each app and figure out how to retrieve data from the disk for each app.
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
