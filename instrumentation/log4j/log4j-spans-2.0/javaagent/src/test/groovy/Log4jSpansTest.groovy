/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import io.opentelemetry.instrumentation.test.log.LogEventsTestBase
import org.apache.logging.log4j.LogManager

class Log4jSpansTest extends LogEventsTestBase {

  static {
    // need to initialize logger before tests to flush out init warning message:
    // "Unable to instantiate org.fusesource.jansi.WindowsAnsiOutputStream"
    LogManager.getLogger(Log4jSpansTest)
  }

  @Override
  Object createLogger(String name) {
    LogManager.getLogger(name)
  }
}
