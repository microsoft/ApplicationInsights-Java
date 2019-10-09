package com.microsoft.applicationinsights.agentc.internal.diagnostics.status;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.microsoft.applicationinsights.agentc.internal.diagnostics.AgentExtensionVersionFinder;
import com.microsoft.applicationinsights.agentc.internal.diagnostics.DiagnosticsHelper;
import com.microsoft.applicationinsights.agentc.internal.diagnostics.DiagnosticsValueFinder;
import com.microsoft.applicationinsights.agentc.internal.diagnostics.InstrumentationKeyFinder;
import com.microsoft.applicationinsights.agentc.internal.diagnostics.MachineNameFinder;
import com.microsoft.applicationinsights.agentc.internal.diagnostics.PidFinder;
import com.microsoft.applicationinsights.agentc.internal.diagnostics.SdkVersionFinder;
import com.microsoft.applicationinsights.agentc.internal.diagnostics.SiteNameFinder;
import com.microsoft.applicationinsights.internal.system.SystemInformation;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import com.squareup.moshi.Moshi.Builder;
import okio.BufferedSink;
import okio.Okio;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class StatusFile {
    private StatusFile() {}

    private static final List<DiagnosticsValueFinder> VALUE_FINDERS = new ArrayList<>();

    @VisibleForTesting
    static final Map<String, Object> CONSTANT_VALUES = new ConcurrentHashMap<>();

    @VisibleForTesting
    static final String FILENAME_PREFIX = "status";

    @VisibleForTesting
    static final String FILE_EXTENSION = ".json";

    private static final String DEFAULT_STATUS_FILE_DIRECTORY = "/home/LogFiles/ApplicationInsights/status";

    @VisibleForTesting
    static String directory;

    private static final ThreadPoolExecutor WRITER_THREAD = new ThreadPoolExecutor(1, 1, 750L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), ThreadPoolUtils.createNamedDaemonThreadFactory("StatusFileWriter"));

    static {
        WRITER_THREAD.allowCoreThreadTimeOut(true);
        CONSTANT_VALUES.put("AppType", "java");
        VALUE_FINDERS.add(new MachineNameFinder());
        VALUE_FINDERS.add(new PidFinder());
        VALUE_FINDERS.add(new SdkVersionFinder());
        VALUE_FINDERS.add(new SiteNameFinder());
        VALUE_FINDERS.add(new InstrumentationKeyFinder());
        VALUE_FINDERS.add(new AgentExtensionVersionFinder());

        if (SystemInformation.INSTANCE.isWindows()) {
            directory = "D:" + DEFAULT_STATUS_FILE_DIRECTORY;
        } else {
            directory = DEFAULT_STATUS_FILE_DIRECTORY;
        }
    }

    public static <T> void putValueAndWrite(String key, T value) {
        if (shouldNotWrite()) {
            return;
        }
        CONSTANT_VALUES.put(key, value);
        write();
    }

    public static void write() {
        if (shouldNotWrite()) {
            return;
        }
        WRITER_THREAD.submit(new Runnable() {
            @Override
            public void run() {
                Map<String, Object> map = getJsonMap();

                String fileName = constructFileName(map);

                final File file = new File(directory, fileName);
                boolean dirsWereCreated = file.getParentFile().mkdirs();
                if (dirsWereCreated || file.getParentFile().exists()) {
                    try (BufferedSink buffer = Okio.buffer(Okio.sink(file))) {
                        new Builder().build().adapter(Map.class).indent(" ").nullSafe().toJson(buffer, map);
                    } catch (Exception e) {
                        LoggerFactory.getLogger(StatusFile.class).error("Error writing {}", file.getAbsolutePath(), e);
                    }
                } else {
                    LoggerFactory.getLogger(StatusFile.class).error("Parent directories for status file could not be created: {}", file.getAbsolutePath());
                }
            }
        }, "StatusFileJsonWrite");
    }

    @VisibleForTesting
    static Map<String, Object> getJsonMap() {
        Map<String, Object> map = new LinkedHashMap<>(CONSTANT_VALUES);
        for (DiagnosticsValueFinder finder : VALUE_FINDERS) {
            final String value = finder.getValue();
            if (!Strings.isNullOrEmpty(value)) {
                map.put(capitalize(finder.getName()), value);
            }
        }
        return map;
    }

    private static boolean shouldNotWrite() {
        return !DiagnosticsHelper.isAppServiceCodeless();
    }

    @VisibleForTesting
    static String constructFileName(Map<String, Object> map) {
        String result = FILENAME_PREFIX;
        final String separator = "_";
        if (map.containsKey(MachineNameFinder.PROPERTY_NAME)) {
            result = result + separator + map.get(MachineNameFinder.PROPERTY_NAME);
        }
        if (map.containsKey(PidFinder.PROPERTY_NAME)) {
            result = result + separator + map.get(PidFinder.PROPERTY_NAME);
        }
        return result + FILE_EXTENSION;
    }

    private static String capitalize(String input) {
        if (input == null) {
            return null;
        }
        return input.substring(0,1).toUpperCase() + input.substring(1);
    }
}
