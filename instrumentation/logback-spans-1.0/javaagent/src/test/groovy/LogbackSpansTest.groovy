/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import ch.qos.logback.classic.LoggerContext
import io.opentelemetry.instrumentation.test.log.LogEventsTestBase

class LogbackSpansTest extends LogEventsTestBase {

  @Override
  Object createLogger(String name) {
    new LoggerContext().getLogger(name)
  }
}
