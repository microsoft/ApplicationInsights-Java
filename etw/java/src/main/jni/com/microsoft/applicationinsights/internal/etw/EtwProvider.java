package com.microsoft.applicationinsights.internal.etw;

import java.io.File;
import java.io.IOException;

import com.microsoft.applicationinsights.internal.etw.events.model.IpaEtwEventBase;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EtwProvider {
    private static final String LIB_FILENAME_32_BIT = "applicationinsights-java-etw-provider-x86.dll";
    private static final String LIB_FILENAME_64_BIT = "applicationinsights-java-etw-provider-x86-64.dll";

    // TODO make sure this logger does not append to EtwAppender
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

    private native void cppWriteEvent(IpaEtwEventBase event) throws ApplicationInsightsEtwException;

    public void writeEvent(IpaEtwEventBase event) throws ApplicationInsightsEtwException {
        cppWriteEvent(event);
    }
}