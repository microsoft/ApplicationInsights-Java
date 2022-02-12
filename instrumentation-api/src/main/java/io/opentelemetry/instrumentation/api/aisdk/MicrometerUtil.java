/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.aisdk;

import java.util.Map;

public class MicrometerUtil {

  private static MicrometerUtilDelegate delegate;

  public static void setDelegate(MicrometerUtilDelegate delegate) {
    MicrometerUtil.delegate = delegate;
  }

  public static void trackMetric(
      String name,
      double value,
      Integer count,
      Double min,
      Double max,
      Map<String, String> properties) {
    if (delegate != null) {
      delegate.trackMetric(name, value, count, min, max, properties);
    }
  }

  public interface MicrometerUtilDelegate {

    void trackMetric(
        String name,
        double value,
        Integer count,
        Double min,
        Double max,
        Map<String, String> properties);
  }
}
