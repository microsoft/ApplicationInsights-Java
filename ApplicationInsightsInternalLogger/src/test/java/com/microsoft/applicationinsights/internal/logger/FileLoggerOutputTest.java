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

package com.microsoft.applicationinsights.internal.logger;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public final class FileLoggerOutputTest {
  private static final String TEMP_LOG_TEST_FOLDER = "JavaSDKLogTests";
  private static final String LOG_FILE_SUFFIX = "jsl";

  private final String workingFolder;

  public FileLoggerOutputTest() {
    workingFolder =
        new File(LocalFileSystemUtils.getTempDir(), TEMP_LOG_TEST_FOLDER).getAbsolutePath();
  }

  @Test
  public void testOneLine() throws IOException {
    testFileLoggerOutputWithRealFiles(new String[] {"line 1"});
  }

  @Test
  public void testTwoLines() throws IOException {
    testFileLoggerOutputWithRealFiles(new String[] {"line 1", "line 2"});
  }

  @Test
  public void testManyLines() throws IOException {
    String[] lines = new String[200];
    for (int i = 0; i < 200; ++i) {
      lines[i] = "line " + String.valueOf(i);
    }
    testFileLoggerOutputWithRealFiles(lines);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFileOutputWithoutPrefix() {
    createFileLoggerOutputWithoutPrefix();
  }

  @Test
  public void testChangeOfFiles() throws IOException {
    LogFileProxy mockProxy1 = Mockito.mock(LogFileProxy.class);
    Mockito.doReturn(true).when(mockProxy1).isFull();

    LogFileProxy mockProxy2 = Mockito.mock(LogFileProxy.class);
    Mockito.doReturn(true).when(mockProxy2).isFull();

    LogFileProxy mockProxy3 = Mockito.mock(LogFileProxy.class);

    final Queue<LogFileProxy> proxies = new LinkedList<LogFileProxy>();
    proxies.add(mockProxy1);
    proxies.add(mockProxy2);
    proxies.add(mockProxy3);

    LogFileProxyFactory mockFactory = Mockito.mock(LogFileProxyFactory.class);
    Mockito.doAnswer(
            new Answer() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                return proxies.remove();
              }
            })
        .when(mockFactory)
        .create((File) anyObject(), anyString(), anyInt());

    File folder = createFolderForTest();
    FileLoggerOutput tested = createFileLoggerOutput();
    tested.setLogProxyFactory(mockFactory);
    try {
      tested.log("line1");
      tested.log("line2");
      tested.log("line3");
    } finally {
      if (folder != null && folder.exists()) {
        FileUtils.deleteDirectory(folder);
      }
    }

    Mockito.verify(mockFactory, Mockito.times(3)).create((File) anyObject(), anyString(), anyInt());

    Mockito.verify(mockProxy1, Mockito.times(1)).writeLine(anyString());
    Mockito.verify(mockProxy1, Mockito.times(1)).writeLine("line1");
    Mockito.verify(mockProxy1, Mockito.times(1)).close();
    Mockito.verify(mockProxy1, Mockito.times(1)).delete();

    Mockito.verify(mockProxy2, Mockito.times(1)).writeLine(anyString());
    Mockito.verify(mockProxy2, Mockito.times(1)).writeLine("line2");
    Mockito.verify(mockProxy2, Mockito.times(1)).close();
    Mockito.verify(mockProxy2, Mockito.never()).delete();

    Mockito.verify(mockProxy3, Mockito.times(1)).writeLine(anyString());
    Mockito.verify(mockProxy3, Mockito.times(1)).writeLine("line3");
    Mockito.verify(mockProxy3, Mockito.never()).close();
    Mockito.verify(mockProxy3, Mockito.never()).delete();
  }

  private void testFileLoggerOutputWithRealFiles(String[] lines) throws IOException {
    File folder = createFolderForTest();
    FileLoggerOutput tested = createFileLoggerOutput();
    try {
      for (String line : lines) {
        tested.log(line);
      }

      tested.close();

      Collection<File> logs = FileUtils.listFiles(folder, new String[] {LOG_FILE_SUFFIX}, false);

      assertNotNull(logs);
      assertThat(logs, hasSize(1));

      BufferedReader br = null;
      File file = null;
      try {
        file = logs.iterator().next();
        br = new BufferedReader(new FileReader(file));
        String line;
        int i = 0;
        while ((line = br.readLine()) != null) {
          assertEquals(lines[i], line);
          ++i;
        }
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        if (file != null) {
          file.delete();
        }

        if (br != null) {
          br.close();
        }
      }
    } finally {
      if (folder != null && folder.exists()) {
        FileUtils.deleteDirectory(folder);
      }
    }
  }

  private FileLoggerOutput createFileLoggerOutputWithoutPrefix() {
    return createFileLoggerOutput(false);
  }

  private FileLoggerOutput createFileLoggerOutput() {
    return createFileLoggerOutput(true);
  }

  private FileLoggerOutput createFileLoggerOutput(boolean withPrefix) {
    Map<String, String> data = new HashMap<String, String>();

    data.put("NumberOfTotalSizeInMB", "2");
    data.put("BaseFolderPath", workingFolder);
    data.put("NumberOfFiles", "2");
    if (withPrefix) {
      data.put("UniquePrefix", "UniquePrefix");
    }
    FileLoggerOutput tested = new FileLoggerOutput(data);
    return tested;
  }

  private File createFolderForTest() throws IOException {
    File folder = new File(workingFolder);
    if (folder.exists()) {
      FileUtils.deleteDirectory(folder);
    }

    return folder;
  }
}
