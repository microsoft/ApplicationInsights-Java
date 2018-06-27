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

package com.microsoft.applicationinsights.core.volume;

import com.microsoft.applicationinsights.internal.channel.TransmissionOutput;
import com.microsoft.applicationinsights.internal.channel.common.Transmission;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.IOUtils;

/** Created by gupele on 2/4/2015. */
final class FakeTransmissionOutput implements TransmissionOutput {
  private final TestResultsVerifier testResultsVerifier;
  private volatile AtomicInteger counter = null;

  public FakeTransmissionOutput(TestResultsVerifier testResultsVerifier) {
    this.testResultsVerifier = testResultsVerifier;
  }

  public TestResultsVerifier getTestResultsVerifier() {
    return testResultsVerifier;
  }

  @Override
  public boolean send(Transmission transmission) {
    ByteArrayOutputStream out = null;
    try {
      out = new ByteArrayOutputStream();
      IOUtils.copy(new GZIPInputStream(new ByteArrayInputStream(transmission.getContent())), out);
      String[] strings = new String(out.toByteArray()).split(System.getProperty("line.separator"));
      testResultsVerifier.notifyEventsArrival(strings.length);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return true;
  }

  @Override
  public void stop(long timeout, TimeUnit timeUnit) {}
}
