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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class LocalFileCacheTests {

  private static final Queue<String> sortedFileNames = new ConcurrentLinkedDeque<>();
  private static final Queue<Long> sortedMilliseconds = new ConcurrentLinkedDeque<>();
  @TempDir File tempFolder;

  @BeforeEach
  public void setup() throws Exception {
    List<String> unsortedFileNames = new ArrayList<>();
    List<Long> unsortedMilliseconds = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      File tempFile = createTempFile(tempFolder);
      File trnFile = new File(tempFolder, FilenameUtils.getBaseName(tempFile.getName()) + ".trn");
      tempFile.renameTo(trnFile);
      unsortedFileNames.add(trnFile.getName());
      unsortedMilliseconds.add(Long.parseLong(trnFile.getName().substring(0, trnFile.getName().lastIndexOf('-'))));
    }

    List<String> tmpList = new ArrayList<>(unsortedFileNames);
    Collections.sort(tmpList);
    for (String filename: tmpList) {
      sortedFileNames.add(filename);
    }

    List<Long> tmpMillisecondList = new ArrayList<>(unsortedMilliseconds);
    Collections.sort(tmpMillisecondList);
    for (Long l : tmpMillisecondList) {
      sortedMilliseconds.add(l);
    }

    Collection<File> files = FileUtils.listFiles(tempFolder, new String[] {"trn"}, false);
    assertThat(files.size()).isEqualTo(100);
    assertThat(sortedFileNames.size()).isEqualTo(sortedMilliseconds.size());
  }

  @Test
  public void testSortPersistedFiles() {
    LocalFileCache cache = new LocalFileCache(tempFolder);
    Queue<String> sortedPersistedFile = cache.getPersistedFilesCache();

    assertThat(sortedPersistedFile.size()).isEqualTo(sortedFileNames.size());
    assertThat(sortedPersistedFile.size()).isEqualTo(sortedMilliseconds.size());

    while (sortedPersistedFile.peek() != null && sortedFileNames.peek() != null && sortedMilliseconds.peek() != null) {
      String actualFilename = sortedPersistedFile.poll();
      String expectedFilename = sortedFileNames.poll();
      assertThat(actualFilename).isEqualTo(expectedFilename);

      Long actualMilliseconds = Long.parseLong(actualFilename.substring(0, actualFilename.lastIndexOf('-')));
      Long expectedMilliseconds = sortedMilliseconds.poll();
      assertThat(actualMilliseconds).isEqualTo(expectedMilliseconds);
    }
  }

  private static File createTempFile(File folder) throws IOException {
    String prefix = System.currentTimeMillis() + "-";
    return File.createTempFile(prefix, null, folder);
  }
}
