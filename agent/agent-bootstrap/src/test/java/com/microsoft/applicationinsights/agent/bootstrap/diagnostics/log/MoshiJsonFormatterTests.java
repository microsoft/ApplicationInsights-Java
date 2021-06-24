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

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MoshiJsonFormatterTests {

  private MoshiJsonFormatter formatter;

  @BeforeEach
  void setup() {
    formatter = new MoshiJsonFormatter();
  }

  @AfterEach
  void tearDown() {
    formatter = null;
  }

  @Test
  void formatterSerializesSimpleMap() {
    Map<String, Object> m = new HashMap<>();
    m.put("s1", "v1");
    m.put("int1", 123);
    m.put("b", true);
    assertThat(formatter.toJsonString(m)).isEqualTo("{\"b\":true,\"int1\":123,\"s1\":\"v1\"}");
  }

  @Test
  void formatterWithPrettyPrintPrintsPretty() {
    Map<String, Object> m = new HashMap<>();
    m.put("s1", "v1");
    m.put("int1", 123);
    formatter.setPrettyPrint(true);
    assertThat(formatter.toJsonString(m))
        .isEqualTo("{\n" + "  \"int1\": 123,\n" + "  \"s1\": \"v1\"\n" + "}");
  }
}
