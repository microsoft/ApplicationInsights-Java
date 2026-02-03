// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.appinsights;

import com.google.auto.service.AutoService;
import com.microsoft.applicationinsights.diagnostics.DiagnosticEngine;
import com.microsoft.applicationinsights.diagnostics.DiagnosticEngineFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ScheduledExecutorService;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Factory for Code Optimizer diagnostics to be service loaded */
@AutoService(DiagnosticEngineFactory.class)
public class CodeOptimizerApplicationInsightFactoryJfr implements DiagnosticEngineFactory {

  private static final Path FILE_SYSTEM_ROOT =
      Paths.get(System.getProperty("applicationinsights.profiler.filesystemRoot", "/"));
  private static final Path CGROUP_DIR = Paths.get("./sys/fs/cgroup");

  private static final Logger logger =
      LoggerFactory.getLogger(CodeOptimizerApplicationInsightFactoryJfr.class);

  @Override
  public DiagnosticEngine create(
      ScheduledExecutorService executorService, @Nullable String cgroupBasePath) {
    Path cgroupPath = getCgroupPath(cgroupBasePath);
    return new CodeOptimizerDiagnosticEngineJfr(executorService, cgroupPath);
  }

  @SuppressFBWarnings(
      value = "SECPTI", // Potential Path Traversal
      justification =
          "The constructed file path cannot be controlled by an end user of the instrumented application")
  @Nullable
  private static Path getCgroupPath(@Nullable String cgroupBasePath) {
    Path cgroupPath = null;
    if (cgroupBasePath != null) {
      cgroupPath = Paths.get(cgroupBasePath);

      if (!Files.exists(cgroupPath)) {
        logger.warn("Configured Cgroup path {} does not exist, setting to default", cgroupBasePath);
        cgroupPath = null;
      }
    }

    if (cgroupPath == null) {
      cgroupPath = FILE_SYSTEM_ROOT.resolve(CGROUP_DIR);

      if (!Files.exists(cgroupPath)) {
        logger.warn("Expected default Cgroup path {} does not exist", cgroupBasePath);
        cgroupPath = null;
      }
    }

    return cgroupPath;
  }
}
