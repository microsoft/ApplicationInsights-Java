// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap;

import java.util.Map;
import javax.annotation.Nullable;

public class MicrometerUtil {

  private static MicrometerUtilDelegate delegate;

  public static void setDelegate(MicrometerUtilDelegate delegate) {
    MicrometerUtil.delegate = delegate;
  }

  public static void trackMetric(
      String name,
      @Nullable String namespace,
      double value,
      Integer count,
      Double min,
      Double max,
      Map<String, String> properties) {
    if (delegate != null) {
      delegate.trackMetric(name, namespace, value, count, min, max, properties);
    }
  }

  public interface MicrometerUtilDelegate {

    void trackMetric(
        String name,
        @Nullable String namespace,
        double value,
        Integer count,
        Double min,
        Double max,
        Map<String, String> properties);
  }

  private MicrometerUtil() {}
}
