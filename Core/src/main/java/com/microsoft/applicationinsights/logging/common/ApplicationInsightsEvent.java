package com.microsoft.applicationinsights.logging.common;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * Interface for
 */
public abstract class ApplicationInsightsEvent {

    public abstract String getMessage();

    public abstract boolean isException();

    public abstract Exception getException();

    public abstract Map<String, String> getCustomParameters();

    protected static void addLogEventProperty(String key, String value, Map<String, String> metaData) {
        if (value != null) {
            metaData.put(key, value);
        }
    }

    protected static String getFormattedDate(long dateInMilliseconds) {
        return new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US).format(new Date(dateInMilliseconds));
    }
}
