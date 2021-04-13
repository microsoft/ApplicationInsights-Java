package com.microsoft.gcmonitortests;

import com.microsoft.gcmonitor.GCCollectionEvent;
import com.microsoft.gcmonitor.JMXMemoryManagement;
import com.microsoft.gcmonitor.UnableToMonitorMemoryException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class GcProcessRunner {
    private InputStream errorStream;
    private InputStream stdOut;
    private OutputStream processInput;
    private String stdOutData;
    private Process process;
    private String gcArg;
    private int heapSizeInMb;

    public GcProcessRunner(String gcArg, int heapSizeInMb) {
        this.gcArg = gcArg;
        this.heapSizeInMb = heapSizeInMb;
    }

    /**
     * Run the GcEventGenerator process and collect gc events
     */
    public List<GCCollectionEvent> getGcCollectionEvents() throws IOException, AttachNotSupportedException, UnableToMonitorMemoryException, InterruptedException, GCNotPresentException {
        Process process = startGcProcess(gcArg, heapSizeInMb);

        try {
            try {
                awaitStdOut("Hit return to start");

                List<GCCollectionEvent> events = new ArrayList<>();
                CountDownLatch cdl = new CountDownLatch(1);

                // Use watchdog timer to ensure process is shutdown at the end
                WatchDog watchDog = new WatchDog(() -> {
                    try {
                        printErrors();

                        //Prompt gc event generator to start
                        awaitStdOut("Hit return to exit");
                        sendMessage("Ending");
                    } catch (IOException | InterruptedException | GCNotPresentException e) {
                        e.printStackTrace();
                    }
                    cdl.countDown();
                }, 2000);

                JMXMemoryManagement.create(
                        getConnector(process),
                        Executors.newSingleThreadExecutor(),
                        event -> {
                            watchDog.reset();
                            events.add(event);
                        });

                watchDog.start();
                sendMessage("Start");

                cdl.await(15, TimeUnit.SECONDS);
                return events;
            } catch (Exception e) {
                try {
                    if (process.exitValue() != 0) {
                        if (errorStream.available() > 0) {
                            String error = new String(errorStream.readAllBytes());
                            if (error.contains("Unrecognized VM option")) {
                                throw new GCNotPresentException();
                            }
                            System.err.println(error);
                        }
                        throw e;
                    }
                } catch (IllegalStateException e2) {
                    throw e;
                }
            }
        } finally {
            printErrors();
            try {
                process.destroyForcibly();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        throw new RuntimeException("Failed to start process");
    }

    private MBeanServerConnection getConnector(Process process) throws AttachNotSupportedException, IOException {
        VirtualMachine vm = VirtualMachine.attach(Long.toString(process.pid()));
        JMXServiceURL target = new JMXServiceURL(vm.startLocalManagementAgent());
        JMXConnector connector = JMXConnectorFactory.connect(target);
        return connector.getMBeanServerConnection();
    }

    private void sendMessage(String message) throws IOException {
        message += "\n";
        processInput.write(message.getBytes(StandardCharsets.UTF_8));
        processInput.flush();
    }

    private void printErrors() throws IOException {
        if (errorStream.available() > 0) {
            String error = new String(errorStream.readNBytes(errorStream.available()));
            System.err.println(error);
        }
    }

    /**
     * Wait for process to emit a string from stdout. Waits up to 10 seconds.
     */
    private void awaitStdOut(String waitFor) throws IOException, InterruptedException, GCNotPresentException {
        for (int i = 0; i < 100; i++) {

            if (!process.isAlive()) {
                if (errorStream.available() > 0) {
                    String error = new String(errorStream.readAllBytes());
                    if (error.contains("Unrecognized VM option")) {
                        throw new GCNotPresentException();
                    }
                    System.err.println(error);
                }
                throw new RuntimeException("GC process exited");
            }

            if (stdOut.available() > 0) {
                stdOutData += new String(stdOut.readNBytes(stdOut.available()));

                if (stdOutData.contains(waitFor)) {
                    break;
                }
            }
            Thread.sleep(100);
        }
    }

    /**
     * Forks a process running the GC event generator
     */
    private Process startGcProcess(String gcArg, int heapSizeInMb) throws IOException {
        String javaCommand = detectJava();

        String classPath = detectClasspath();

        process = new ProcessBuilder()
                .command(javaCommand, "-XX:+UnlockExperimentalVMOptions", gcArg, "-Xmx" + heapSizeInMb + "m", "-cp", classPath, "com.microsoft.gcmonitortests.GcEventGenerator")
                .start();

        //Fail save kill forked proces
        Runtime.getRuntime().addShutdownHook(new Thread(process::destroyForcibly));

        errorStream = process.getErrorStream();
        stdOut = process.getInputStream();
        processInput = process.getOutputStream();

        stdOutData = "";

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return process;
    }

    private String detectJava() {
        String javaHome = System.getProperty("java.home");
        String javaCommand = "java";
        if (javaHome != null) {
            javaCommand = javaHome + File.separator + "bin" + File.separator + "java";
        }
        return javaCommand;
    }

    private String detectClasspath() {
        String classPath = "build/classes/java/test/";
        String modulePath = System.getProperty("jdk.module.path");
        if (modulePath != null) {
            //Should point to the main classpath
            classPath = new File(new File(modulePath).getParentFile(), "classes/java/test").getAbsolutePath();
        }
        return classPath;
    }
}
