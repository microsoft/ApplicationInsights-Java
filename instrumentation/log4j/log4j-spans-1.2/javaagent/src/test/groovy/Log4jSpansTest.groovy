/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import io.opentelemetry.instrumentation.test.log.LogEventsTestBase
import org.apache.log4j.Logger

class Log4jSpansTest extends LogEventsTestBase {

  @Override
  Object createLogger(String name) {
    Logger.getLogger(name)
  }
}
