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

package com.microsoft.applicationinsights.agent.internal.statsbeat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class InstrumentationsTest {

  private static final Set<String> instrumentations;
  private static final long[] EXPECTED_INSTRUMENTATIONS;

  static {
    instrumentations = new HashSet<>();
    instrumentations.add("io.opentelemetry.jdbc");
    instrumentations.add("io.opentelemetry.tomcat-7.0");
    instrumentations.add("io.opentelemetry.http-url-connection");
    instrumentations.add("io.opentelemetry.apache-camel-2.20");
    instrumentations.add("io.opentelemetry.akka-http-10.0");
    instrumentations.add("io.opentelemetry.methods");
    instrumentations.add("io.opentelemetry.okhttp-2.2");
    instrumentations.add("io.opentelemetry.play-ws-2.0");
    instrumentations.add("io.opentelemetry.vertx-kafka-client-3.5");

    EXPECTED_INSTRUMENTATIONS = new long[2];
    EXPECTED_INSTRUMENTATIONS[0] =
        (long)
            (Math.pow(2, 5)
                + Math.pow(2, 13)
                + Math.pow(2, 21)
                + Math.pow(2, 42)
                + Math.pow(2, 47)); // Exponents are keys from
    // StatsbeatTestUtils.INSTRUMENTATION_MAP_DECODING.)
    EXPECTED_INSTRUMENTATIONS[1] =
        (long)
            (Math.pow(2, 64 - 64)
                + Math.pow(2, 65 - 64)
                + Math.pow(2, 69 - 64)
                + Math.pow(2, 71 - 64)); // Exponents are keys from
    // StatsbeatTestUtils.INSTRUMENTATION_MAP_DECODING.)
  }

  @Test
  public void testEncodeAndDecodeInstrumentations() {
    long[] longVal = Instrumentations.encode(instrumentations);
    assertThat(longVal).isEqualTo(EXPECTED_INSTRUMENTATIONS);
    Set<String> result = StatsbeatTestUtils.decodeInstrumentations(longVal);
    assertThat(result).isEqualTo(instrumentations);
  }
}
