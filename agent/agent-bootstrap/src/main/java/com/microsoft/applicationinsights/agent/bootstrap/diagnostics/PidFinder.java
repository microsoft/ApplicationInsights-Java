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

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import org.checkerframework.checker.nullness.qual.Nullable;

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
