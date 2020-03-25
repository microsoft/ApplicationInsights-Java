package com.microsoft.applicationinsights.internal.etw;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.util.LocalFileSystemUtils;
import com.microsoft.applicationinsights.internal.util.PropertyHelper;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

class DllFileUtils {
    private DllFileUtils() {}

    // From JniPCConnector in applicationinsights-core
    public static final String AI_BASE_FOLDER = "AISDK";
    public static final String AI_NATIVE_FOLDER = "native";

    // from :core:JniPCConnector.java
    public static File buildDllLocalPath() {
        File dllPath = LocalFileSystemUtils.getTempDir();

        dllPath = new File(dllPath.toString(), AI_BASE_FOLDER);
        dllPath = new File(dllPath.toString(), AI_NATIVE_FOLDER);
        dllPath = new File(dllPath.toString(), PropertyHelper.getSdkVersionNumber());

        if (!dllPath.exists()) {
            dllPath.mkdirs();
        }

        if (!dllPath.exists() || !dllPath.canRead() || !dllPath.canWrite()) {
            throw new RuntimeException("Failed to create a read/write folder for the native dll.");
        }

        InternalLogger.INSTANCE.trace("%s folder exists", dllPath.toString());

        return dllPath;
    }

    public static void extractToLocalFolder(File dllOnDisk, String libraryToLoad) throws IOException {
        ClassLoader classLoader = DllFileUtils.class.getClassLoader();
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        InputStream in = classLoader.getResourceAsStream(libraryToLoad);
        if (in == null) {
            throw new RuntimeException(String.format("Failed to find '%s' in jar", libraryToLoad));
        }

        OutputStream out = null;
        try {
            out = FileUtils.openOutputStream(dllOnDisk);
            IOUtils.copy(in, out);

            InternalLogger.INSTANCE.trace("Successfully extracted '%s' to local folder", libraryToLoad);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                InternalLogger.INSTANCE.error("Failed to close input stream for dll extraction: %s", e.toString());
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    InternalLogger.INSTANCE.error("Failed to close output stream for dll extraction: %s", e.toString());
                }
            }
        }
    }

}