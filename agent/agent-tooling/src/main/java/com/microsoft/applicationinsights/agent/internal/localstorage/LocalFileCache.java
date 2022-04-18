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

package com.microsoft.applicationinsights.agent.internal.localstorage;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;

public class LocalFileCache {

  /**
   * Track a list of active filenames persisted on disk. FIFO (First-In-First-Out) read will avoid
   * an additional sorting at every read. Caveat: data loss happens when the app crashes. filenames
   * stored in this queue will be lost forever. There isn't an unique way to identify each java app.
   * C# uses "User@processName" to identify each app, but Java can't rely on process name since it's
   * a system property that can be customized via the command line.
   */
  private final Queue<String> persistedFilesCache = new ConcurrentLinkedDeque<>();

  public LocalFileCache(File folder) {
    List<File> files = sortPersistedFiles(folder);
    // existing files are not older than 48 hours and need to get added to the queue to be
    // re-processed.
    // this will avoid data loss in the case of app crashes and restarts.
    for (File file : files) {
      persistedFilesCache.add(file.getName());
    }
  }

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

  @Nullable
  private static List<File> sortPersistedFiles(File folder) {
    return FileUtils.listFiles(folder, new String[] {"trn"}, false).stream()
        .sorted(Comparator.comparing(File::lastModified))
        .collect(Collectors.toList());
  }
}
