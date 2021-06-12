package com.microsoft.applicationinsights.smoketestapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

@RestController
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @GetMapping("/")
    public String root() {
        return "OK";
    }

    @GetMapping("/spawn-another-java-process")
    public String spawnAnotherJavaProcess() throws Exception {
        Class<?> clazz = Class.forName("org.springframework.boot.loader.JarLauncher", false, ClassLoader.getSystemClassLoader());
        File appJar = new File(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
        File agentJar = getAgentJarFile();
        List<String> command = Arrays.asList("java",
                "-javaagent:" + agentJar.getAbsolutePath(),
                "-jar", appJar.toString(), "okhttp3");
        ProcessBuilder builder = new ProcessBuilder(command);
        Process process = builder.start();
        ConsoleOutputPipe consoleOutputPipe =
                new ConsoleOutputPipe(process.getInputStream(), System.out);
        Executors.newSingleThreadExecutor().submit(consoleOutputPipe);
        ConsoleOutputPipe consoleErrorPipe =
                new ConsoleOutputPipe(process.getErrorStream(), System.err);
        Executors.newSingleThreadExecutor().submit(consoleErrorPipe);
        process.waitFor();
        return "OK!";
    }

    private static File getAgentJarFile() {
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (String jvmArg : jvmArgs) {
            if (jvmArg.startsWith("-javaagent:") && jvmArg.contains("applicationinsights-agent")) {
                return new File(jvmArg.substring("-javaagent:".length()));
            }
        }
        throw new AssertionError("Agent jar not found on command line: " + String.join(" ", jvmArgs));
    }

    private static class ConsoleOutputPipe implements Runnable {

        private final InputStream in;
        private final OutputStream out;

        private ConsoleOutputPipe(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[100];
            try {
                while (true) {
                    int n = in.read(buffer);
                    if (n == -1) {
                        break;
                    }
                    out.write(buffer, 0, n);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
