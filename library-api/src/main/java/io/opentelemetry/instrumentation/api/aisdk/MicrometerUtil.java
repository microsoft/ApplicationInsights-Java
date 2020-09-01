/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.instrumentation.api.aisdk;

import java.util.Map;

public class MicrometerUtil {

  private static MicrometerUtilDelegate delegate;

  public static void setDelegate(final MicrometerUtilDelegate delegate) {
    MicrometerUtil.delegate = delegate;
  }

  public static void trackMetric(
      final String name,
      final double value,
      final Integer count,
      final Double min,
      final Double max,
      final Map<String, String> properties) {
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
