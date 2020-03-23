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

package com.microsoft.applicationinsights.internal.perfcounter;

import com.microsoft.applicationinsights.internal.system.SystemInformation;

import com.microsoft.applicationinsights.internal.util.LocalFileSystemUtils;
import com.microsoft.applicationinsights.internal.util.PropertyHelper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class serves as the connection to the native code that does the work with Windows.
 *
 * Created by gupele on 3/30/2015.
 */
public final class JniPCConnector {

    private static final Logger logger = LoggerFactory.getLogger(JniPCConnector.class);

    public static final String AI_BASE_FOLDER = "AISDK";
    public static final String AI_NATIVE_FOLDER = "native";
    public static final String PROCESS_SELF_INSTANCE_NAME = "__SELF__";

    private static final String BITS_MODEL_64 = "64";
    private static final String NATIVE_LIBRARY_64 = "applicationinsights-core-native-win64.dll";
    private static final String NATIVE_LIBRARY_32 = "applicationinsights-core-native-win32.dll";

    private static String currentInstanceName;

    // Native methods are private and can be accessed through matching public methods in the class.
    private native static String getInstanceName(int processId);

    private native static String addCounter(String category, String counter, String instance);

    private native static double getPerformanceCounterValue(String name);

    /**
     * This method must be called before any other method.
     * All other methods are relevant only if this method was successful.
     *
     * Note that the method should not throw and should gracefully return the boolean value.
     * @return True on success.
     */
    public static boolean initialize() {
        try {
            if (!SystemInformation.INSTANCE.isWindows()) {
                logger.error("Jni connector is only used on Windows OS.");
                return false;
            }

            loadNativeLibrary();
        } catch (ThreadDeath td) {
            throw td;
        } catch (Throwable e) {
            try {
                logger.error(
                    "Failed to load native dll, Windows performance counters will not be used. " +
                "Please make sure that Visual C++ Redistributable is properly installed: {}.", e.toString());

                return false;
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t2) {
                // chomp
            }
        }

        return true;
    }

    /**
     * Adding a performance counter
     * @param category The category must be non null non empty value.
     * @param counter The counter must be non null non empty value.
     * @param instance The instance.
     * @return The key for retrieving counter data.
     */
    public static String addPerformanceCounter(String category, String counter, String instance) {
        if (StringUtils.isEmpty(category)) {
           throw new IllegalArgumentException("category must be non-null non empty string.");
        }
        if (StringUtils.isEmpty(counter)) {
            throw new IllegalArgumentException("counter must be non-null non empty string.");
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Registering performance counter: {}\\{} [{}]", category, counter, StringUtils.trimToEmpty(instance));
        }
        final String s = addCounter(category, counter, instance);
        if (StringUtils.isEmpty(s) && logger.isWarnEnabled()) {
            logger.warn("Performance coutner registration failed for {}\\{} [{}]", category, counter, StringUtils.trimToEmpty(instance));
        }
        return s;
    }

    /**
     * Process instance name is only known at runtime, therefore process level performance counters
     * should use the 'PROCESS_SELF_INSTANCE_NAME' as the requested process name and then call this
     * method to translate that logical name into the actual name that is fetched from the native code.
     * @param instanceName The raw instance name
     * @return The actual instance name.
     * @throws Exception If instanceName equals PROCESS_SELF_INSTANCE_NAME but the actual instance name is unknown.
     */
    public static String translateInstanceName(String instanceName) throws Exception {
        if (PROCESS_SELF_INSTANCE_NAME.equals(instanceName)) {
            if (StringUtils.isEmpty(currentInstanceName)) {
                throw new Exception("Cannot translate instance name: Unknown current instance name");
            }

            return currentInstanceName;
        }

        return instanceName;
    }

    /**
     * This method will delegate the call to the native code after the proper sanity checks.
     * @param name The logical name of the performance counter as was fetched during the 'addPerformanceCounter' call.
     * @return The current value.
     */
    public static double getValueOfPerformanceCounter(String name) {
        if(StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("name must be non-null non empty value.");
        }

        return getPerformanceCounterValue(name);
    }

    private static void initNativeCode() {
        int processId = Integer.parseInt(SystemInformation.INSTANCE.getProcessId());

        currentInstanceName = getInstanceName(processId);
        if (StringUtils.isEmpty(currentInstanceName)) {
            logger.error("Failed to fetch current process instance name, process counters for for the process level will not be activated.");
        } else {
            logger.trace("Java process name is set to '{}'", currentInstanceName);
        }
    }

    /**
     * The method will try to extract the dll for the Windows performance counters to a local
     * folder and then will try to load it. The method will do all that by doing the following things:
     * 1. Find the OS type (64/32) currently supports only 64 bit.
     * 2. Will find the path to extract to, which is %temp%/AI_BASE_FOLDER/AI_NATIVE_FOLDER/sdk_version_number
     * 3. Find out whether or not the file already exists in that directory
     * 4. If the dll is not there, the method will extract it from the jar to that directory
     * 5. The method will call System.load to load the dll and by doing so we are ready to use it
     * @return true on success, otherwise false
     * @throws IOException If there are errors in opening/writing/reading/closing etc.
     *         Note that the method might throw RuntimeExceptions due to critical issues
     */
    private static void loadNativeLibrary() throws IOException {
        String model = System.getProperty("sun.arch.data.model");
        String libraryToLoad = BITS_MODEL_64.equals(model) ? NATIVE_LIBRARY_64 : NATIVE_LIBRARY_32;

        File dllPath = buildDllLocalPath();

        File dllOnDisk = new File(dllPath, libraryToLoad);

        if (!dllOnDisk.exists()) {
            extractToLocalFolder(dllOnDisk, libraryToLoad);
        }

        System.load(dllOnDisk.toString());

        initNativeCode();

        logger.trace("Successfully loaded library '{}'", libraryToLoad);
    }

    private static void extractToLocalFolder(File dllOnDisk, String libraryToLoad) throws IOException {
        ClassLoader classLoader = JniPCConnector.class.getClassLoader();
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

            logger.trace("Successfully extracted '{}' to local folder", libraryToLoad);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                logger.error("Failed to close input stream for dll extraction: {}", e.toString());
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    logger.error("Failed to close output stream for dll extraction: {}", e.toString());
                }
            }
        }
    }

    private static File buildDllLocalPath() {
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

        logger.trace("{} folder exists", dllPath.toString());

        return dllPath;
    }
}