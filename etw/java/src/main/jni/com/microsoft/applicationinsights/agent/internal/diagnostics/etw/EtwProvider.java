// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.diagnostics.etw;

import com.microsoft.applicationinsights.agent.internal.diagnostics.etw.events.model.IpaEtwEventBase;
import java.io.File;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EtwProvider {
  private static final String LIB_FILENAME_32_BIT = "applicationinsights-java-etw-provider-x86.dll";
  private static final String LIB_FILENAME_64_BIT =
      "applicationinsights-java-etw-provider-x86-64.dll";

  private static final Logger logger = LoggerFactory.getLogger(EtwProvider.class);

  public EtwProvider(String sdkVersion) {
    String osname = System.getProperty("os.name");
    if (osname != null && osname.startsWith("Windows")) {
      File dllPath = null;
      try {
        dllPath = loadLibrary(sdkVersion);
        logger.debug("EtwProvider initialized. Lib path={}", dllPath.getAbsolutePath());
      } catch (Throwable t) {
        try {
          logger.debug("Error initializing EtwProvider", t);
          if (dllPath != null) {
            dllPath.deleteOnExit();
          }
        } catch (Throwable chomp) {
          // ignore
        }
      }
    } else {
      LoggerFactory.getLogger(EtwProvider.class)
          .info("Non-Windows OS. Loading ETW library skipped.");
    }
  }

  private static File loadLibrary(String sdkVersion) throws IOException {
    String fileName = getDllFilenameForArch();

    File targetDir = DllFileUtils.buildDllLocalPath(sdkVersion);
    File dllPath = new File(targetDir, fileName);

    if (!dllPath.exists()) {
      DllFileUtils.extractToLocalFolder(dllPath, fileName);
    }

    System.load(dllPath.getAbsolutePath());

    return dllPath;
  }

  static String getDllFilenameForArch() {
    String osarch = System.getProperty("os.arch");
    boolean is32bit = osarch == null ? false : osarch.equalsIgnoreCase("x86");
    return is32bit ? LIB_FILENAME_32_BIT : LIB_FILENAME_64_BIT;
  }

  private native void cppWriteEvent(IpaEtwEventBase event) throws ApplicationInsightsEtwException;

  public void writeEvent(IpaEtwEventBase event) throws ApplicationInsightsEtwException {
    cppWriteEvent(event);
  }
}
