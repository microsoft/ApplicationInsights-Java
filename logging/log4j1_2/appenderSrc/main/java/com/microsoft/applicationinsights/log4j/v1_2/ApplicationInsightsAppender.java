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

package com.microsoft.applicationinsights.log4j.v1_2;

import com.microsoft.applicationinsights.internal.common.LogTelemetryClientProxy;
import com.microsoft.applicationinsights.internal.common.TelemetryClientProxy;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.log4j.v1_2.internal.ApplicationInsightsLogEvent;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

public class ApplicationInsightsAppender extends AppenderSkeleton {

  // region Members

  private boolean isInitialized = false;
  private String instrumentationKey;
  private TelemetryClientProxy telemetryClientProxy;

  // endregion Members

  // region Public methods

  public TelemetryClientProxy getTelemetryClientProxy() {
    return this.telemetryClientProxy;
  }

  /**
   * Sets the instrumentation key. This method is used by Log4j system initializer when reading
   * configuration.
   *
   * @param key The instrumentation key.
   */
  public void setInstrumentationKey(String key) {
    this.instrumentationKey = key;
  }

  /**
   * Subclasses of <code>AppenderSkeleton</code> should implement this method to perform actual
   * logging. See also {@link #doAppend AppenderSkeleton.doAppend} method.
   */
  @Override
  protected void append(LoggingEvent event) {
    if (this.closed || !this.isInitialized) {

      // TODO: trace that closed or not initialized.
      return;
    }

    try {
      ApplicationInsightsLogEvent aiEvent = new ApplicationInsightsLogEvent(event);
      this.telemetryClientProxy.sendEvent(aiEvent);
    } catch (Exception e) {
      // Appender failure must not fail the running application.
      // TODO: Assert.Debug/warning on exception?
      InternalLogger.INSTANCE.trace("Stack trace generated is %s", ExceptionUtils.getStackTrace(e));
    }
  }

  /** Release any allocated resources. */
  @Override
  public void close() {
    // No resources to release.
  }

  /**
   * This Appender converts the LoggingEvent it receives into a text string and requires the layout
   * format string to do so.
   */
  @Override
  public boolean requiresLayout() {
    return true;
  }

  /** This method is being called on object initialization. */
  @Override
  public void activateOptions() {
    super.activateOptions();

    try {
      this.telemetryClientProxy = new LogTelemetryClientProxy(this.instrumentationKey);
      this.isInitialized = true;
    } catch (Exception e) {
      // Appender failure must not fail the running application.
      this.isInitialized = false;
      InternalLogger.INSTANCE.error(
          "Failed to initialize appender with exception: %s.", e.toString());
    }
  }

  // endregion Public methods
}
