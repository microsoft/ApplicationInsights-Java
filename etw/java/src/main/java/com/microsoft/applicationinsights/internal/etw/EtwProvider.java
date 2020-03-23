package com.microsoft.applicationinsights.internal.etw;

import java.io.File;
import java.io.IOException;

import com.google.common.base.Preconditions;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EtwProvider {
    private static final String LIB_FILENAME_32_BIT = "applicationinsights-java-etw-provider-x86.dll";
    private static final String LIB_FILENAME_64_BIT = "applicationinsights-java-etw-provider-x86-64.dll";
    
    // From JniPCConnector in applicationinsights-core
    private static final String AI_BASE_FOLDER = "AISDK";
    private static final String AI_NATIVE_FOLDER = "native";

    private static Logger LOGGER;
    static {
        if (SystemUtils.IS_OS_WINDOWS) {
            LOGGER = LoggerFactory.getLogger(EtwProvider.class);
            File dllPath = null;
            try {
                dllPath = loadLibrary();
                LOGGER.info("EtwProvider initialized. Lib path={}", dllPath.getAbsolutePath());
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t) {
                try {
                    LOGGER.error("Error initializing EtwProvider", t);
                    if (dllPath != null) {
                        dllPath.deleteOnExit();
                    }
                } catch (ThreadDeath td) {
                    throw td;
                } catch (Throwable chomp) {
                    // ignore
                }
            }
        } else {
            LoggerFactory.getLogger(EtwProvider.class).info("Non-Windows OS. Loading ETW library skipped.");
        }
    }

    static void load() {
        // triggers static initializer
    }
    
    private static File loadLibrary() throws IOException {
        final String fileName = getDllFilenameForArch();

        final File targetDir = DllFileUtils.buildDllLocalPath();
        final File dllPath = new File(targetDir, fileName);

        if (!dllPath.exists()) {
            DllFileUtils.extractToLocalFolder(dllPath, fileName);
        }

        System.load(dllPath.getAbsolutePath());

        return dllPath;
    }

    static String getDllFilenameForArch() {
        final boolean is32bit = StringUtils.defaultIfEmpty(System.getProperty("os.arch"), "null").equalsIgnoreCase("x86");
        return is32bit ? LIB_FILENAME_32_BIT : LIB_FILENAME_64_BIT;
    }
    
    private static final int WINEVENT_LEVEL_INFO = 4;
    private static final int WINEVENT_LEVEL_ERROR = 2;
    private static final int WINEVENT_LEVEL_CRITICAL = 1;

    // TODO remove eventId, remove eventName
    private native void cppWriteEvent(int eventId, String eventName, int level, String extensionVersion, String subscriptionId, String appName, String resourceType, String logger, String message);

    // TODO delete me
    private native boolean cppIsProviderEnabled(int level/*, int keywordMask*/); // no keywords at the moment. Define if keywords are added.

    private static String extensionVersion = "ev";
    private static String subscriptionId = "sid";
    private static String appName = "app";
    private static String resourceType = "rt";

    public static void setExtensionVersion(String version) {
        Preconditions.checkNotNull(version, "version cannot be null");
        extensionVersion = version;
    }

    public static void setSubscriptionId(String subscriptionId) {
        Preconditions.checkNotNull(subscriptionId, "subscriptionId cannot be null");
        EtwProvider.subscriptionId = subscriptionId;
    }

    public static void setAppName(String appName) {
        Preconditions.checkNotNull(appName, "appName cannot be null");
        EtwProvider.appName = appName;
    }

    public static void setResourceType(String resourceType) {
        Preconditions.checkNotNull(resourceType, "resourceType cannot be null");
        EtwProvider.resourceType =  resourceType;
    }

    public void info(String logger, String messageFormat, Object... messageArgs) {
        cppWriteEvent(1, "Information", WINEVENT_LEVEL_INFO, extensionVersion, subscriptionId, appName, resourceType, logger, String.format(messageFormat, messageArgs));
    }

    public void error(String logger, String messageFormat, Object... messageArgs) {
        cppWriteEvent(2, "Error", WINEVENT_LEVEL_ERROR, extensionVersion, subscriptionId, appName, resourceType, logger, String.format(messageFormat, messageArgs));
    }

    public void critical(String logger, String messageFormat, Object... messageArgs) {
        cppWriteEvent(2, "Critical", WINEVENT_LEVEL_CRITICAL, extensionVersion, subscriptionId, appName, resourceType, logger, String.format(messageFormat, messageArgs));
    }
}