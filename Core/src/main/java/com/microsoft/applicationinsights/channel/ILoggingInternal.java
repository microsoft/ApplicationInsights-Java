package com.microsoft.applicationinsights.channel;

public interface ILoggingInternal {
    public static boolean enableDebugMode = true;
    public static String prefix = "com.microsoft.applicationinsights";

    public void warn(String tag, String message);

    public void throwInternal(String tag, String message) throws Exception;
}
