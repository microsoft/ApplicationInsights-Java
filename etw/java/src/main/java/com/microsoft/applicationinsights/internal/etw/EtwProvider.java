package com.microsoft.applicationinsights.internal.etw;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EtwProvider {
    private static final String LIB_FILENAME_32_BIT = "applicationinsights-java-etw-provider-x86.dll";
    private static final String LIB_FILENAME_64_BIT = "applicationinsights-java-etw-provider-x86_64.dll";
    
    // From JniPCConnector in applicationinsights-core
    private static final String AI_BASE_FOLDER = "AISDK";
    private static final String AI_NATIVE_FOLDER = "native";

    private static Logger LOGGER;
    static {
        if (SystemUtils.IS_OS_WINDOWS) {
            LOGGER = LoggerFactory.getLogger(EtwProvider.class);
            extractLibrary();
        } else {
            LoggerFactory.getLogger(EtwProvider.class).info("Non-Windows OS. Loading ETW library skipped.");
        }
    }
    
    private static void extractLibrary() {
        final boolean is32bit = StringUtils.defaultIfEmpty(System.getProperty("os.arch"), "null").equalsIgnoreCase("x86");
        final String fileName = is32bit ? LIB_FILENAME_32_BIT : LIB_FILENAME_64_BIT;

        // TODO finish loading DLL
    }
    
    private static final int WINEVENT_LEVEL_INFO = 4;
    private static final int WINEVENT_LEVEL_ERROR = 2;
    private static final int WINEVENT_LEVEL_CRITICAL = 1;

    private native void cppWriteEvent(int eventId, String eventName, int level, String extensionVersion, String subscriptionId, String appName, String logger, String message);

    private native boolean cppIsProviderEnabled(int level/*, int keywordMask*/); // no keywords at the moment. Define if keywords are added.

    private static String extensionVersion = "TODO-extVersion"; // TODO set jar version
    private static String subscriptionId = "TODO-subID"; //TODO set sub id
    private static String appName = "TODO-appName"; // TODO set app name

    public void info(String logger, String messageFormat, Object... messageArgs) {
        if (cppIsProviderEnabled(WINEVENT_LEVEL_INFO)) {
            cppWriteEvent(1, "Information", WINEVENT_LEVEL_INFO, extensionVersion, subscriptionId, appName, logger, String.format(messageFormat, messageArgs));
        }
    }

    public void error(String logger, String messageFormat, Object... messageArgs) {
        if (cppIsProviderEnabled(WINEVENT_LEVEL_ERROR)) {
            cppWriteEvent(2, "Error", WINEVENT_LEVEL_ERROR, extensionVersion, subscriptionId, appName, logger, String.format(messageFormat, messageArgs));
        }
    }

    public void critical(String logger, String messageFormat, Object... messageArgs) {
        if (cppIsProviderEnabled(WINEVENT_LEVEL_CRITICAL)) {
            cppWriteEvent(2, "Critical", WINEVENT_LEVEL_CRITICAL, extensionVersion, subscriptionId, appName, logger, String.format(messageFormat, messageArgs));
        }
    }
}