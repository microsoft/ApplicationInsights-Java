/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.microsoft.applicationinsights.internal.util.LocalFileSystemUtils;
import com.microsoft.applicationinsights.internal.util.PropertyHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DllFileUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(DllFileUtils.class);

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
        LOGGER.trace("{} folder exists", dllPath.toString());

        return dllPath;
    }

    /**
     * Assumes dllOnDisk is non-null and exists.
     * @param dllOnDisk
     * @param libraryToLoad
     * @throws IOException
     */
    public static void extractToLocalFolder(File dllOnDisk, String libraryToLoad) throws IOException {
        ClassLoader classLoader = DllFileUtils.class.getClassLoader();
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        try (InputStream in = classLoader.getResourceAsStream(libraryToLoad)) {
            if (in == null) {
                throw new RuntimeException(String.format("Failed to find '%s' in jar", libraryToLoad));
            }
            final byte[] buffer = new byte[8192];
            try (OutputStream out = new FileOutputStream(dllOnDisk, false)) {
                if (dllOnDisk.exists()) {
                    if (dllOnDisk.isDirectory()) {
                        throw new IOException("Cannot extract dll: "+dllOnDisk.getAbsolutePath()+" exists as a directory");
                    }
                    if (!dllOnDisk.canWrite()) {
                        throw new IOException("Cannote extract dll: "+dllOnDisk.getAbsolutePath()+" is not writeable.");
                    }
                }

                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) { // while not EOF
                    out.write(buffer, 0, bytesRead);
                }
            }
        }
        LOGGER.info("Successfully extracted '{}' to local folder", libraryToLoad);
    }

}