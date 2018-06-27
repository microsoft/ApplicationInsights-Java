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

package com.microsoft.applicationinsights.sample;

import com.microsoft.applicationinsights.extensibility.TelemetryProcessor;
import com.microsoft.applicationinsights.telemetry.Telemetry;

/**
 * Sample filter This class is an example for having a custom filter that can filter out unneded
 * telemetries. To have one, you should implement {@link TelemetryProcessor} and implement its
 * 'process' method.
 *
 * <p>The method gets a {@link Telemetry} instance that is ready to be sent. This is your chance to
 * approve or deny it. Returning 'false' means that the Telemetry will not be sent while 'true'
 * means you approve it.
 *
 * <p>The Telemetry might go through other filters though, that might deny its sending.
 *
 * <p>To enable this processor you need to add it in the ApplicationInsights.xml like this:
 *
 * <p>{@code <TelemetryProcessors>
 *
 * <p><CustomProcessors> <Processor type="com.microsoft.applicationinsights.sample.SampleFilter">
 * <Add name="Pass" value="false"/> </Processor> </CustomProcessors>
 *
 * <p></TelemetryProcessors> }
 *
 * <p>Note that the class defines a property named 'pass' which is configured too. Every property
 * that you wish to configure needs to have a 'setX' public method like 'setPass' in this example
 * <b>Exceptions thrown from the 'setX' methods will be caught by the framework that will ignore the
 * filter</b>
 *
 * <p>Created by gupele on 7/26/2016.
 */
public class SampleFilter implements TelemetryProcessor {
  private boolean pass = false;

  public void setPass(String bad) {
    this.pass = Boolean.valueOf(bad);
  }

  @Override
  public boolean process(Telemetry telemetry) {
    return pass;
  }
}
