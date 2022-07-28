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

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics.status;

import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.ApplicationMetadataFactory;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsHelper;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsValueFinder;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.MachineNameFinder;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.MsgId;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.PidFinder;
import com.squareup.moshi.Moshi;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import okio.BufferedSink;
import okio.Okio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class StatusFile {

  private static final List<DiagnosticsValueFinder> VALUE_FINDERS = new ArrayList<>();

  // visible for testing
  static final Map<String, Object> CONSTANT_VALUES = new ConcurrentHashMap<>();

  // visible for testing
  static final String FILENAME_PREFIX = "status";

  // visible for testing
  static final String FILE_EXTENSION = ".json";

  // visible for testing
  static final String HOME_ENV_VAR = "HOME";

  // visible for testing
  static final String DEFAULT_HOME_DIR = "/home";

  // visible for testing
  static final String DEFAULT_LOGDIR = "/LogFiles";

  // visible for testing
  static final String DEFAULT_APPLICATIONINSIGHTS_LOGDIR = "/ApplicationInsights";

  // visible for testing
  static String logDir;

  //  private static final AtomicBoolean alreadyLogged = new AtomicBoolean();

  private static final Object lock = new Object();

  // guarded by lock
  private static String uniqueId;

  // guarded by lock
  private static BufferedSink buffer;

  @Nullable public static Logger startupLogger;

  private static final Logger statusFileLogger = LoggerFactory.getLogger(StatusFile.class);

  private static final ThreadPoolExecutor WRITER_THREAD =
      new ThreadPoolExecutor(
          1, 1, 750L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), StatusFile::newThread);

  static {
    WRITER_THREAD.allowCoreThreadTimeOut(true);
    CONSTANT_VALUES.put("AppType", "java");
    ApplicationMetadataFactory mf = DiagnosticsHelper.getMetadataFactory();
    VALUE_FINDERS.add(mf.getMachineName());
    VALUE_FINDERS.add(mf.getPid());
    VALUE_FINDERS.add(mf.getSdkVersion());
    VALUE_FINDERS.add(mf.getSiteName());
    VALUE_FINDERS.add(mf.getInstrumentationKey());
    VALUE_FINDERS.add(mf.getExtensionVersion());

    logDir = initLogDir();
  }

  private static Thread newThread(Runnable r) {
    Thread thread = new Thread(r);
    thread.setName("StatusFileWriter");
    thread.setDaemon(true);
    return thread;
  }

  // visible for testing
  static String initLogDir() {
    if (DiagnosticsHelper.isOsWindows()) {
      return DEFAULT_HOME_DIR + DEFAULT_LOGDIR + DEFAULT_APPLICATIONINSIGHTS_LOGDIR;
    }
    return DiagnosticsHelper.LINUX_DEFAULT;
  }

  public static String getLogDir() {
    return logDir;
  }

  private StatusFile() {}

  public static <T> void putValueAndWrite(String key, T value) {
    putValueAndWrite(key, value, true);
  }

  public static <T> void putValueAndWrite(String key, T value, boolean loggingInitialized) {
    CONSTANT_VALUES.put(key, value);
    write(loggingInitialized);
  }

  public static <T> void putValue(String key, T value) {
    CONSTANT_VALUES.put(key, value);
  }

  public static void write() {
    write(false);
  }

  @SuppressWarnings("SystemOut")
  private static void write(boolean loggingInitialized) {
    WRITER_THREAD.submit(
        new Runnable() {
          @Override
          @SuppressFBWarnings(
              value = "SECPTI", // Potential Path Traversal
              justification =
                  "The constructed file path cannot be controlled by an end user of the instrumented application")
          public void run() {
            Map<String, Object> map = getJsonMap();

            String fileName = constructFileName(map);
            statusFileLogger.debug("#### status filename: " + fileName);

            // the executor should prevent more than one thread from executing this block.
            // this is just a safeguard
            synchronized (lock) {
              File file = new File(logDir, fileName);
              boolean dirsWereCreated = file.getParentFile().mkdirs();

              Logger logger = loggingInitialized ? LoggerFactory.getLogger(StatusFile.class) : null;

              if (dirsWereCreated || file.getParentFile().exists()) {
                BufferedSink b = null;
                try {
                  b = getBuffer(file);
                  new Moshi.Builder()
                      .build()
                      .adapter(Map.class)
                      .indent(" ")
                      .nullSafe()
                      .toJson(b, map);
                  b.flush();
                } catch (Exception e) {
                  if (logger != null) {
                    try (MDC.MDCCloseable ignored = MsgId.STATUS_FILE_RELATED_ERROR.makeActive()) {
                      logger.error("Error writing {}", file.getAbsolutePath(), e);
                    }
                  } else {
                    e.printStackTrace();
                  }
                  if (b != null) {
                    try {
                      b.close();
                    } catch (IOException ex) {
                      // ignore this
                    }
                  }
                }
              } else {
                if (logger != null) {
                  try (MDC.MDCCloseable ignored = MsgId.STATUS_FILE_RELATED_ERROR.makeActive()) {
                    logger.error(
                        "Parent directories for status file could not be created: {}",
                        file.getAbsolutePath());
                  }
                } else {
                  System.err.println(
                      "Parent directories for status file could not be created: "
                          + file.getAbsolutePath());
                }
              }
            }
          }
        },
        "StatusFileJsonWrite");
  }

  private static BufferedSink getBuffer(File file) throws IOException {
    synchronized (lock) {
      if (buffer != null) {
        buffer.close();
      }
      if (DiagnosticsHelper.isOsWindows()) {
        buffer =
            Okio.buffer(
                Okio.sink(
                    file.toPath(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.DELETE_ON_CLOSE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING));
      } else { // on linux, the file is deleted/unlinked immediately using DELETE_ON_CLOSE making it
        // unavailable to other processes. Using shutdown hook instead.
        buffer =
            Okio.buffer(
                Okio.sink(
                    file.toPath(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING));
        file.deleteOnExit();
      }
      return buffer;
    }
  }

  // visible for testing
  static Map<String, Object> getJsonMap() {
    Map<String, Object> map = new LinkedHashMap<>(CONSTANT_VALUES);
    for (DiagnosticsValueFinder finder : VALUE_FINDERS) {
      String value = finder.getValue();
      if (value != null && !value.isEmpty()) {
        map.put(capitalize(finder.getName()), value);
      }
    }
    return map;
  }

  /**
   * This MUST return the same filename each time. This should be unique for each process.
   *
   * @param map Json map to be written (contains some values incorporated into the filename)
   * @return The filename
   */
  // visible for testing
  static String constructFileName(Map<String, Object> map) {
    String result = FILENAME_PREFIX;
    String separator = "_";
    if (map.containsKey(MachineNameFinder.PROPERTY_NAME)) {
      result = result + separator + map.get(MachineNameFinder.PROPERTY_NAME);
    }
    return result + separator + getUniqueId(map.get(PidFinder.PROPERTY_NAME)) + FILE_EXTENSION;
  }

  /**
   * If pid is available, use pid. Otherwise, use process start time. If neither are available, use
   * a random guid.
   *
   * @param pid The process' id.
   * @return A unique id for the current process.
   */
  private static String getUniqueId(@Nullable Object pid) {
    synchronized (lock) {
      if (uniqueId != null) {
        return uniqueId;
      }

      if (pid != null) {
        uniqueId = pid.toString();
      } else {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        if (runtimeMxBean != null) {
          uniqueId = String.valueOf(Math.abs(runtimeMxBean.getStartTime()));
        } else {
          uniqueId = UUID.randomUUID().toString().replace("-", "");
        }
      }

      return uniqueId;
    }
  }

  @Nullable
  private static String capitalize(@Nullable String input) {
    if (input == null) {
      return null;
    }
    return input.substring(0, 1).toUpperCase() + input.substring(1);
  }
}
