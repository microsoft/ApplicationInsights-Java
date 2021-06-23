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

package com.microsoft.applicationinsights.internal.statsbeat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class InstrumentationsTest {

  private static final Set<String> instrumentations;

  static {
    instrumentations = new HashSet<>();
    instrumentations.add("io.opentelemetry.javaagent.jdbc");
    instrumentations.add("io.opentelemetry.javaagent.tomcat-7.0");
    instrumentations.add("io.opentelemetry.javaagent.http-url-connection");
  }

  private static final long EXPECTED_INSTRUMENTATION =
      (long)
          (Math.pow(2, 13)
              + Math.pow(2, 21)
              + Math.pow(2, 57)); // Exponents are keys from StatsbeatHelper.INSTRUMENTATION_MAP.)

  @Test
  public void testEncodeAndDecodeInstrumentations() {
    long longVal = Instrumentations.encode(instrumentations);
    assertThat(longVal).isEqualTo(EXPECTED_INSTRUMENTATION);
    Set<String> result = StatsbeatTestUtils.decodeInstrumentations(longVal);
    assertThat(result).isEqualTo(instrumentations);
  }
}
