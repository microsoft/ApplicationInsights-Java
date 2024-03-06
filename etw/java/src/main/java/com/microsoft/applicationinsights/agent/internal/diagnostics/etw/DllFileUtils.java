// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.diagnostics.etw;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DllFileUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(DllFileUtils.class);

  private DllFileUtils() {}

  public static final String AI_BASE_FOLDER = "AISDK";
  public static final String AI_NATIVE_FOLDER = "native";

  @SuppressFBWarnings(
      value = "SECPTI", // Potential Path Traversal
      justification =
          "The constructed file path cannot be controlled by an end user of the instrumented application")
  public static File buildDllLocalPath(@Nullable String versionDirectory) {
    File dllPath = getTempDir();
    LOGGER.debug("#### temp dir: '{}'", dllPath.getPath());

    dllPath = new File(dllPath, AI_BASE_FOLDER);
    dllPath = new File(dllPath, AI_NATIVE_FOLDER);
    if (versionDirectory == null || versionDirectory.isEmpty()) {
      dllPath = new File(dllPath, "unknown-version");
    } else {
      dllPath = new File(dllPath, versionDirectory);
    }

    if (!dllPath.exists()) {
      try {
        dllPath.mkdirs();
      } catch (SecurityException e) {
        throw new IllegalStateException(
            "Failed to create a folder AISDK/native for the native dll.", e);
      }
    }

    if (!dllPath.exists() || !dllPath.canRead() || !dllPath.canWrite()) {
      throw new IllegalStateException("Failed to create a read/write folder for the native dll.");
    }
    LOGGER.debug("{} folder exists", dllPath);

    return dllPath;
  }

  /** Assumes dllOnDisk is non-null and exists. */
  public static void extractToLocalFolder(File dllOnDisk, String libraryToLoad) throws IOException {
    LOGGER.debug("#### extractToLocalFolder start");
    ClassLoader classLoader = DllFileUtils.class.getClassLoader();
    if (classLoader == null) {
      classLoader = ClassLoader.getSystemClassLoader();
    }
    try (InputStream in = classLoader.getResourceAsStream("inst/" + libraryToLoad)) {
      if (in == null) {
        throw new IllegalStateException(
            String.format("Failed to find '%s' in jar inst/", libraryToLoad));
      }
      byte[] buffer = new byte[8192];
      try (OutputStream out = new FileOutputStream(dllOnDisk, false)) {
        if (dllOnDisk.exists()) {
          LOGGER.debug("#### dllOnDisk exists: '{}'", dllOnDisk);
          if (dllOnDisk.isDirectory()) {
            LOGGER.debug("#### dllOnDisk is a directory: '{}'", dllOnDisk.isDirectory());
            throw new IOException(
                "Cannot extract dll: " + dllOnDisk.getAbsolutePath() + " exists as a directory");
          }
          if (!dllOnDisk.canWrite()) {
            LOGGER.debug("#### dllOnDisk is not writeable: '{}'", dllOnDisk.canWrite());
            throw new IOException(
                "Cannote extract dll: " + dllOnDisk.getAbsolutePath() + " is not writeable.");
          } else {
            LOGGER.debug("#### dllOnDisk is writeable: '{}'", dllOnDisk.canWrite());
          }
        } else {
          LOGGER.debug("#### dllOnDisk does not exist: '{}'", dllOnDisk);
        }

        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) { // while not EOF
          out.write(buffer, 0, bytesRead);
        }
        LOGGER.debug("#### Successfully extracted '{}' to local folder", libraryToLoad);
      } catch (IOException e) {
        LOGGER.error("#### Failed to extract '{}' to local folder", libraryToLoad, e);
        return;
      }
    }
    LOGGER.debug("#### extractToLocalFolder done");
  }

  private static final List<String> CANDIDATE_USERNAME_ENVIRONMENT_VARIABLES =
      Collections.unmodifiableList(Arrays.asList("USER", "LOGNAME", "USERNAME"));

  /** From com.microsoft.applicationinsights.agent.internal.telemetry.util.LocalFileSystemUtils */
  private static File getTempDir() {
    String tempDirectory = System.getProperty("java.io.tmpdir");
    String currentUserName = determineCurrentUserName();

    File result = getTempDir(tempDirectory, currentUserName);
    if (!result.isDirectory()) {
      // Noinspection ResultOfMethodCallIgnored
      result.mkdirs();
    }
    return result;
  }

  /** From com.microsoft.applicationinsights.agent.internal.telemetry.util.LocalFileSystemUtils */
  @SuppressFBWarnings(
      value = "SECPTI", // Potential Path Traversal
      justification =
          "The constructed file path cannot be controlled by an end user of the instrumented application")
  private static File getTempDir(String initialValue, String userName) {
    String tempDirectory = initialValue;

    // does it look shared?
    // TODO: this only catches the Linux case; I think a few system users on Windows might share
    // c:\Windows\Temp
    if ("/tmp".contentEquals(tempDirectory)) {
      File candidate = new File(tempDirectory, userName);
      tempDirectory = candidate.getAbsolutePath();
    }

    return new File(tempDirectory);
  }

  /** From com.microsoft.applicationinsights.agent.internal.telemetry.util.LocalFileSystemUtils */
  private static String determineCurrentUserName() {
    String userName;
    // Start with the value of the "user.name" property
    userName = System.getProperty("user.name");

    if (userName != null && !userName.isEmpty()) {
      // Try some environment variables
      for (String candidate : CANDIDATE_USERNAME_ENVIRONMENT_VARIABLES) {
        userName = System.getenv(candidate);
        if (userName != null && userName.isEmpty()) {
          break;
        }
      }
    }

    if (userName == null || userName.isEmpty()) {
      userName = "unknown";
    }

    return userName;
  }
}
