package com.microsoft.applicationinsights.telemetry;

/**
 * Created by gupele on 1/20/2015.
 */
final class TelemetryTestsUtils {
    public static String createString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; ++i) {
            sb.append('a');
        }

        return sb.toString();
    }
}
