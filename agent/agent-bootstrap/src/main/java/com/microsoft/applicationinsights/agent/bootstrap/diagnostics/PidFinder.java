// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import javax.annotation.Nullable;

public class PidFinder extends CachedDiagnosticsValueFinder {
  public static final String PROPERTY_NAME = "PID";

  @Override
  protected String populateValue() {
    String java9pid = getPidUsingProcessHandle();
    if (java9pid != null) {
      return java9pid;
    }

    return getPidUsingRuntimeBean();
  }

  @Nullable
  private static String getPidUsingRuntimeBean() {
    // will only work with sun based jvm
    RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
    if (rb == null) {
      return null;
    }
    String name = rb.getName();
    if (name == null) {
      return null;
    }
    String pid = name.split("@")[0];
    if (pid == null) {
      return null;
    }
    try {
      return String.valueOf(Integer.parseInt(pid));
    } catch (NumberFormatException nfe) {
      return null;
    }
  }

  @Nullable
  private static String getPidUsingProcessHandle() {
    try {
      // if java.specification.version < 9, the next line will fail.
      Class<?> processHandleClass = Class.forName("java.lang.ProcessHandle");
      Method currentProcessHandleMethod = processHandleClass.getMethod("current");
      Object currentProcessHandle = currentProcessHandleMethod.invoke(null);
      if (currentProcessHandle == null) {
        return null;
      }

      Method pidMethod = processHandleClass.getMethod("pid");
      Object pid = pidMethod.invoke(currentProcessHandle);
      if (pid == null) {
        return null;
      }
      return pid.toString();
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public String getName() {
    return PROPERTY_NAME;
  }
}
