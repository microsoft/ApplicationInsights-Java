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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiConnectionString {

  private static Accessor accessor;
  private static final Logger log = LoggerFactory.getLogger(AiConnectionString.class);

  public static void setAccessor(Accessor accessor) {
    AiConnectionString.accessor = accessor;
    log.debug("######### setting ConnectionString Accessor for AiConnectionString.");
  }

  public static boolean hasConnectionString() {
    log.debug("######### checking AiConnectionString hasConnectionString");
    log.debug("######### " + String.valueOf("hasConnectionString: " + accessor != null && accessor.hasValue()));
    return accessor != null && accessor.hasValue();
  }

  public static void setConnectionString(String connectionString) {
    if (accessor != null) {
      accessor.setValue(connectionString);
      log.debug(("######### setConnectionString to be " + connectionString));
    } else {
      log.debug("######### accessor is null somehow.  Failed to set connection string.");
    }
  }

  public interface Accessor {
    boolean hasValue();
    void setValue(String value);
  }
}
