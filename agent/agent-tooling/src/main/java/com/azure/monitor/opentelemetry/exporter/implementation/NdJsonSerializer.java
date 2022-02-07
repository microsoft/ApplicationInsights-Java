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

package com.azure.monitor.opentelemetry.exporter.implementation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.List;

/** Custom serializer that serializes a list of items into line-delimited JSON format. */
public class NdJsonSerializer extends StdSerializer<List<?>> {

  /** NDJSON is JSON (non-pretty printed) with a new line delimiter after each line. */
  private static final String NEW_LINE_DELIMITER = System.lineSeparator();

  /** Classes serial ID. */
  private static final long serialVersionUID = 1L;

  /** Creates a new instance of the serializer. */
  public NdJsonSerializer() {
    super(List.class, false);
  }

  @Override
  public void serialize(
      final List<?> values, final JsonGenerator gen, final SerializerProvider provider)
      throws IOException {

    if (values == null) {
      return;
    }

    for (Object o : values) {
      gen.writeObject(o);
      gen.writeRawValue(NEW_LINE_DELIMITER);
    }
  }
}
