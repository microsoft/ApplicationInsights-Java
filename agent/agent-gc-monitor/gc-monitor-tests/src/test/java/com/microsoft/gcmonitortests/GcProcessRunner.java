// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.gcmonitortests;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.microsoft.gcmonitor.GcCollectionEvent;
import com.microsoft.gcmonitor.JmxMemoryManagement;
import com.microsoft.gcmonitor.UnableToMonitorMemoryException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

// TODO (trask) revisit Java8ApiChecker suppression
@SuppressWarnings({"Java8ApiChecker", "SystemOut", "CatchAndPrintStackTrace"})
public class GcProcessRunner {
  private InputStream errorStream;
  private InputStream stdOut;
  private OutputStream processInput;
  private String stdOutData;
  private Process process;
  private final String gcArg;
  private final int heapSizeInMb;

  public GcProcessRunner(String gcArg, int heapSizeInMb) {
    this.gcArg = gcArg;
    this.heapSizeInMb = heapSizeInMb;
  }

  /** Run the GcEventGenerator process and collect gc events. */
  public List<GcCollectionEvent> getGcCollectionEvents()
      throws IOException, UnableToMonitorMemoryException, InterruptedException,
          GcNotPresentException {
    int port = getRandomPort();
    Process process = startGcProcess(port, gcArg, heapSizeInMb);

    try {
      try {
        awaitStdOut("Hit return to start");

        List<GcCollectionEvent> events = new ArrayList<>();
        CountDownLatch cdl = new CountDownLatch(1);

        // Use watchdog timer to ensure process is shutdown at the end
        WatchDog watchDog =
            new WatchDog(
                () -> {
                  try {
                    printErrors();

                    // Prompt gc event generator to start
                    awaitStdOut("Hit return to exit");
                    sendMessage("Ending");
                  } catch (IOException | InterruptedException | GcNotPresentException e) {
                    e.printStackTrace();
                  }
                  cdl.countDown();
                },
                2000);

        JmxMemoryManagement.create(
            getConnector(port),
            Executors.newSingleThreadExecutor(),
            event -> {
              watchDog.reset();
              events.add(event);
            });

        watchDog.start();
        sendMessage("Start");

        cdl.await(15, TimeUnit.SECONDS);
        return events;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (RuntimeException e) {
        if (process.exitValue() != 0) {
          if (errorStream.available() > 0) {
            String error = new String(readBytes(errorStream), UTF_8);
            if (error.contains("Unrecognized VM option")) {
              throw new GcNotPresentException();
            }
            System.err.println(error);
          }
          throw e;
        }
      }
    } finally {
      printErrors();
      try {
        process.destroyForcibly();
      } catch (RuntimeException e) {
        e.printStackTrace();
      }
    }

    throw new RuntimeException("Failed to start process");
  }

  private static int getRandomPort() {
    for (int i = 0; i < 100; i++) {
      int port = (int) (10000 * Math.random() + 40000);
      try {
        ServerSocket serverSocket = new ServerSocket(port);
        serverSocket.close();
        return port;
      } catch (IOException ignored) {
        // loop and try again
      }
    }
    throw new IllegalStateException("Unable to find free port");
  }

  private static MBeanServerConnection getConnector(int port) throws IOException {
    String url =
        "service:jmx:rmi://127.0.0.1:" + port + "/jndi/rmi://127.0.0.1:" + port + "/jmxrmi";
    JMXServiceURL target = new JMXServiceURL(url);
    JMXConnector connector = JMXConnectorFactory.connect(target);
    return connector.getMBeanServerConnection();
  }

  private void sendMessage(String message) throws IOException {
    message += "\n";
    processInput.write(message.getBytes(UTF_8));
    processInput.flush();
  }

  private void printErrors() throws IOException {
    if (errorStream.available() > 0) {
      String error = new String(readBytes(errorStream), UTF_8);
      System.err.println(error);
    }
  }

  /** Wait for process to emit a string from stdout. Waits up to 10 seconds. */
  private void awaitStdOut(String waitFor)
      throws IOException, InterruptedException, GcNotPresentException {
    for (int i = 0; i < 100; i++) {

      if (!process.isAlive()) {
        if (errorStream.available() > 0) {
          String error = new String(readBytes(errorStream), UTF_8);
          if (error.contains("Unrecognized VM option")) {
            throw new GcNotPresentException();
          }
          System.err.println(error);
        }
        throw new RuntimeException("GC process exited");
      }

      if (stdOut.available() > 0) {
        stdOutData += new String(readBytes(stdOut), UTF_8);

        if (stdOutData.contains(waitFor)) {
          break;
        }
      }
      Thread.sleep(100);
    }
  }

  /** Forks a process running the GC event generator. */
  private Process startGcProcess(int port, String gcArg, int heapSizeInMb)
      throws IOException, InterruptedException {
    String javaCommand = detectJava();

    String classPath = detectClasspath();

    process =
        new ProcessBuilder()
            .command(
                javaCommand,
                "-Dcom.sun.management.jmxremote=true",
                "-Dcom.sun.management.jmxremote.port=" + port,
                "-Dcom.sun.management.jmxremote.rmi.port=" + port,
                "-Dcom.sun.management.jmxremote.local.only=true",
                "-Dcom.sun.management.jmxremote.authenticate=false",
                "-Dcom.sun.management.jmxremote.ssl=false",
                "-Djava.rmi.server.hostname=127.0.0.1",
                "-XX:+UnlockExperimentalVMOptions",
                gcArg,
                "-Xmx" + heapSizeInMb + "m",
                "-cp",
                classPath,
                "com.microsoft.gcmonitortests.GcEventGenerator")
            .start();

    // Fail save kill forked proces
    Runtime.getRuntime().addShutdownHook(new Thread(process::destroyForcibly));

    errorStream = process.getErrorStream();
    stdOut = process.getInputStream();
    processInput = process.getOutputStream();

    stdOutData = "";

    Thread.sleep(1000);
    return process;
  }

  private static String detectJava() {
    String javaHome = System.getProperty("java.home");
    String javaCommand = "java";
    if (javaHome != null) {
      javaCommand = javaHome + File.separator + "bin" + File.separator + "java";
    }
    return javaCommand;
  }

  private static String detectClasspath() {
    String classPath = "build/classes/java/test/";
    String modulePath = System.getProperty("jdk.module.path");
    if (modulePath != null) {
      // Should point to the main classpath
      classPath =
          new File(new File(modulePath).getParentFile(), "classes/java/test").getAbsolutePath();
    }
    return classPath;
  }

  private static byte[] readBytes(InputStream in) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    while (in.available() > 0) {
      int read = in.read(buffer);
      baos.write(buffer, 0, read);
    }
    return baos.toByteArray();
  }
}
