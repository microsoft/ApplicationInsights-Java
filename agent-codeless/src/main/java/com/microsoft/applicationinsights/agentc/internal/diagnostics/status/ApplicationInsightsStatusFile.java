package com.microsoft.applicationinsights.agentc.internal.diagnostics.status;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.microsoft.applicationinsights.agentc.internal.diagnostics.DiagnosticsValueFinder;
import com.microsoft.applicationinsights.agentc.internal.diagnostics.MachineNameFinder;
import com.microsoft.applicationinsights.agentc.internal.diagnostics.PidFinder;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Moshi.Builder;
import okio.BufferedSink;
import okio.Okio;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ApplicationInsightsStatusFile {
    @VisibleForTesting
    static List<DiagnosticsValueFinder> values = new ArrayList<>();
    @VisibleForTesting
    static Map<String, Object> constants = new LinkedHashMap<>();

    static final String FILENAME_PREFIX = "status";
    static final String FILE_EXTENSION = ".json";


    static {
        constants.put("AppType", "java");
    }

    public static <T> void putValueAndWrite(String key, T value) {
        constants.put(key, value);
        write();
    }

    public static void write() {
        Map<String, Object> map = new LinkedHashMap<>(constants);
        for (DiagnosticsValueFinder finder : values) {
            final String value = finder.getValue();
            if (!Strings.isNullOrEmpty(value)) {
                map.put(capitalize(finder.getName()), value);
            }
        }

        String fileName = constructFileName(map);

        try (BufferedSink buffer = Okio.buffer(Okio.sink(new File(getDirectory(), fileName)))) {
            Moshi moshi = new Builder().build();
            moshi.adapter(Map.class).nullSafe().toJson(buffer, map);
        } catch (IOException e) {
            // TODO
        }
    }

    private static String constructFileName(Map<String, Object> map) {
        String result = FILENAME_PREFIX;
        if (map.containsKey(MachineNameFinder.PROPERTY_NAME)) {
            result = result + map.get(MachineNameFinder.PROPERTY_NAME);
        }
        if (map.containsKey(PidFinder.PROPERTY_NAME)) {
            result = result + map.get(PidFinder.PROPERTY_NAME);
        }
        return result + FILE_EXTENSION;
    }

    private static String getDirectory() {
        return ".";
    }

    private static String capitalize(String input) {
        if (input == null) {
            return null;
        }
        return input.substring(0,1).toUpperCase() + input.substring(1);
    }
}
