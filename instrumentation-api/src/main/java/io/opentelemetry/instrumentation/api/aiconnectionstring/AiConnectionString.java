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

package io.opentelemetry.instrumentation.api.aiconnectionstring;

public class AiConnectionString {

  private static volatile Accessor accessor;

  public static void setAccessor(Accessor accessor) {
    AiConnectionString.accessor = accessor;
  }

  public static boolean hasConnectionString() {
    return accessor != null && accessor.hasValue();
  }

  public static void setConnectionString(String connectionString) {
    if (accessor != null) {
      accessor.setValue(connectionString);
    }
  }

  public interface Accessor {
    boolean hasValue();
    void setValue(String value);
  }
}
