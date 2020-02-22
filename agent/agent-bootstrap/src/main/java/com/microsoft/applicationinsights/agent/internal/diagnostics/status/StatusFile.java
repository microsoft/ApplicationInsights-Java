package com.microsoft.applicationinsights.agent.internal.diagnostics.status;

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
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.microsoft.applicationinsights.agent.internal.diagnostics.AgentExtensionVersionFinder;
import com.microsoft.applicationinsights.agent.internal.diagnostics.DiagnosticsHelper;
import com.microsoft.applicationinsights.agent.internal.diagnostics.DiagnosticsValueFinder;
import com.microsoft.applicationinsights.agent.internal.diagnostics.InstrumentationKeyFinder;
import com.microsoft.applicationinsights.agent.internal.diagnostics.MachineNameFinder;
import com.microsoft.applicationinsights.agent.internal.diagnostics.PidFinder;
import com.microsoft.applicationinsights.agent.internal.diagnostics.SdkVersionFinder;
import com.microsoft.applicationinsights.agent.internal.diagnostics.SiteNameFinder;
import com.squareup.moshi.Moshi;
import okio.BufferedSink;
import okio.Okio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusFile {

    private static final Logger logger = LoggerFactory.getLogger(StatusFile.class);

    private static final boolean isWindows;

    private static final List<DiagnosticsValueFinder> VALUE_FINDERS = new ArrayList<>();

    // visible for testing
    static final Map<String, Object> CONSTANT_VALUES = new ConcurrentHashMap<>();

    // visible for testing
    static final String FILENAME_PREFIX = "status";

    // visible for testing
    static final String FILE_EXTENSION = ".json";

    private static final String DEFAULT_STATUS_FILE_DIRECTORY = "/home/LogFiles/ApplicationInsights/status";

    // visible for testing
    static String directory;

    private static final Object lock = new Object();

    // guarded by lock
    private static String uniqueId;

    // guarded by lock
    private static BufferedSink buffer;

    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("StatusFileWriter");
            thread.setDaemon(true);
            return thread;
        }
    };

    private static final ThreadPoolExecutor WRITER_THREAD =
            new ThreadPoolExecutor(1, 1, 750L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
                    THREAD_FACTORY);

    static {
        WRITER_THREAD.allowCoreThreadTimeOut(true);
        CONSTANT_VALUES.put("AppType", "java");
        VALUE_FINDERS.add(new MachineNameFinder());
        VALUE_FINDERS.add(new PidFinder());
        VALUE_FINDERS.add(new SdkVersionFinder());
        VALUE_FINDERS.add(new SiteNameFinder());
        VALUE_FINDERS.add(new InstrumentationKeyFinder());
        VALUE_FINDERS.add(new AgentExtensionVersionFinder());

        String osName = System.getProperty("os.name");
        isWindows = osName != null && osName.startsWith("Windows");

        if (isWindows) {
            directory = "D:" + DEFAULT_STATUS_FILE_DIRECTORY;
        } else {
            directory = DEFAULT_STATUS_FILE_DIRECTORY;
        }
    }

    private StatusFile() {
    }

    public static <T> void putValueAndWrite(String key, T value) {
        if (!DiagnosticsHelper.shouldOutputDiagnostics()) {
            return;
        }
        CONSTANT_VALUES.put(key, value);
        write();
    }

    public static void write() {
        if (!DiagnosticsHelper.shouldOutputDiagnostics()) {
            return;
        }
        WRITER_THREAD.submit(new Runnable() {
            @Override
            public void run() {
                Map<String, Object> map = getJsonMap();

                String fileName = constructFileName(map);

                // the executor should prevent more than one thread from executing this block.
                // this is just a safeguard
                synchronized (lock) {
                    final File file = new File(directory, fileName);
                    boolean dirsWereCreated = file.getParentFile().mkdirs();
                    if (dirsWereCreated || file.getParentFile().exists()) {
                        BufferedSink b = null;
                        try {
                            b = getBuffer(file);
                            new Moshi.Builder().build().adapter(Map.class).indent(" ").nullSafe().toJson(b, map);
                            b.flush();
                            logger.info("Wrote status to file: {}", file.getAbsolutePath());
                        } catch (Exception e) {
                            logger.error("Error writing {}", file.getAbsolutePath(), e);
                            if (b != null) {
                                try {
                                    b.close();
                                } catch (IOException ex) {
                                    // ignore this
                                }
                            }
                        }
                    } else {
                        logger.error("Parent directories for status file could not be created: {}",
                                file.getAbsolutePath());
                    }
                }
            }
        }, "StatusFileJsonWrite");
    }

    private static BufferedSink getBuffer(File file) throws IOException {
        synchronized (lock) {
            if (buffer != null) {
                buffer.close();
            }
            if (isWindows) {
                buffer = Okio.buffer(
                        Okio.sink(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.DELETE_ON_CLOSE,
                                StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
            } else { // on linux, the file is deleted/unlinked immediately using DELETE_ON_CLOSE making it
                // unavailable to other processes. Using shutdown hook instead.
                buffer = Okio.buffer(Okio.sink(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE,
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
            final String value = finder.getValue();
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
        final String separator = "_";
        if (map.containsKey(MachineNameFinder.PROPERTY_NAME)) {
            result = result + separator + map.get(MachineNameFinder.PROPERTY_NAME);
        }
        return result + separator + getUniqueId(map.get(PidFinder.PROPERTY_NAME)) + FILE_EXTENSION;
    }

    /**
     * If pid is available, use pid. Otherwise, use process start time. If neither are available, use a random guid.
     *
     * @param pid The process' id.
     * @return A unique id for the current process.
     */
    private static String getUniqueId(Object pid) {
        synchronized (lock) {
            if (uniqueId != null) {
                return uniqueId;
            }

            if (pid != null) {
                uniqueId = pid.toString();
            } else {
                final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
                if (runtimeMXBean != null) {
                    uniqueId = String.valueOf(Math.abs(runtimeMXBean.getStartTime()));
                } else {
                    uniqueId = UUID.randomUUID().toString().replace("-", "");
                }
            }

            return uniqueId;
        }
    }

    private static String capitalize(String input) {
        if (input == null) {
            return null;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}
