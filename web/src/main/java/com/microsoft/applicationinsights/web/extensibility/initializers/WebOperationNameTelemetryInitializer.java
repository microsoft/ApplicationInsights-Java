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

package com.microsoft.applicationinsights.web.extensibility.initializers;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;

/**
 * Created by yonisha on 2/17/2015.
 */
public class WebOperationNameTelemetryInitializer extends WebTelemetryInitializerBase {

    /**
     * Initializes the properties of the given telemetry.
     *
     * @param telemetry The {@link com.microsoft.applicationinsights.telemetry.Telemetry} to initialize.
     */
    @Override
    protected void onInitializeTelemetry(Telemetry telemetry) {
        RequestTelemetryContext telemetryContext = ThreadContext.getRequestTelemetryContext();
        String operationName = telemetryContext.getHttpRequestTelemetry().getName();

        updateRequestNameIfRequestTelemetry(telemetry, operationName);

        telemetry.getContext().getOperation().setName(operationName);
    }

    // region Private

    private void updateRequestNameIfRequestTelemetry(Telemetry telemetry, String operationName) {
        if (!(telemetry instanceof RequestTelemetry)) {
            return;
        }

        RequestTelemetry requestTelemetry = (RequestTelemetry)telemetry;

        // We only update the request telemetry name if not already provided by the user.
        if (requestTelemetry != null && Strings.isNullOrEmpty(requestTelemetry.getName())) {
            requestTelemetry.setName(operationName);
        }
    }

    // endregion Private
}
