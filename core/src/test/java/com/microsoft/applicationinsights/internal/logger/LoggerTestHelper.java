package com.microsoft.applicationinsights.internal.logger;

public class LoggerTestHelper {
    public static void resetInternalLogger() {
        InternalLogger.INSTANCE.reset();
    }
}
