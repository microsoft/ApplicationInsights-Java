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

package com.microsoft.applicationinsights.internal.schemav2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;
import com.microsoft.applicationinsights.telemetry.SessionState;
import com.microsoft.applicationinsights.telemetry.SessionStateTelemetry;
import java.io.IOException;
import java.io.StringWriter;
import org.junit.Test;

public final class SessionStateDataTest {
  private static void verifyEnvelope(SessionStateData sessionStateData, SessionState expectedState)
      throws IOException {
    Envelope envelope = new Envelope();
    envelope.setName((new SessionStateTelemetry()).getEnvelopName());
    Data<SessionStateData> tmp = new Data<SessionStateData>();
    tmp.setBaseData(sessionStateData);
    tmp.setBaseType((new SessionStateTelemetry()).getBaseTypeName());
    envelope.setData(tmp);

    StringWriter writer = new StringWriter();
    JsonTelemetryDataSerializer jsonWriter = new JsonTelemetryDataSerializer(writer);
    envelope.serialize(jsonWriter);
    jsonWriter.close();
    String asJson = writer.toString();
    String expectedDataAsString =
        String.format(
            "\"data\":{\"baseType\":\"SessionStateData\",\"baseData\":{\"ver\":2,\"state\":\"%s\"}}",
            expectedState.toString());
    int index = asJson.indexOf(expectedDataAsString);
    assertTrue(index != -1);
    index = asJson.indexOf("\"name\":\"SessionState\"");
    assertTrue(index != -1);
  }

  @Test
  public void testCtor() throws IOException {
    SessionStateData sessionStateData = new SessionStateData(SessionState.Start);
    assertEquals(2, sessionStateData.getVer());
    assertEquals(SessionState.Start, sessionStateData.getState());
    verifyEnvelope(sessionStateData, SessionState.Start);
  }

  @Test
  public void testSetState() throws IOException {
    SessionStateData sessionStateData = new SessionStateData(SessionState.Start);
    sessionStateData.setState(SessionState.End);

    assertEquals(SessionState.End, sessionStateData.getState());
    verifyEnvelope(sessionStateData, SessionState.End);
  }
}
