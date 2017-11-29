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

import com.microsoft.applicationinsights.common.CommonUtils;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import java.util.Map;

/**
 * Created by yonisha on 2/16/2015.
 */
public class WebOperationIdTelemetryInitializer extends WebTelemetryInitializerBase {

    /**
     * Initializes the properties of the given telemetry.
     */
    @Override
    protected void onInitializeTelemetry(Telemetry telemetry) {
        RequestTelemetryContext telemetryContext = ThreadContext.getRequestTelemetryContext();

        if (telemetryContext == null) {
            InternalLogger.INSTANCE.error(
                "Unexpected error. No telemetry context found. OperationContext will not be initialized.");
                return;
        }

        RequestTelemetry requestTelemetry = telemetryContext.getHttpRequestTelemetry();
        String currentOperationId = requestTelemetry.getContext().getOperation().getId();

        // if there's no current operation (e.g. telemetry being initialized outside of 
        // request scope), just initialize operationId to the generic id currently in request
        if (currentOperationId == null || currentOperationId.isEmpty()) {
            telemetry.getContext().getOperation().setId(requestTelemetry.getId());
            return;
        }

        // set operationId to the request telemetry's operation ID
        if (CommonUtils.isNullOrEmpty(telemetry.getContext().getOperation().getId())) {
            telemetry.getContext().getOperation().setId(currentOperationId);
        }

        // set operation parentId to the request telemetry's ID
        if (CommonUtils.isNullOrEmpty(telemetry.getContext().getOperation().getParentId())) {
            telemetry.getContext().getOperation().setParentId(requestTelemetry.getId());
        }

        // add correlation context to properties
        Map<String, String> correlationContextMap = telemetryContext.getCorrelationContext().getMappings();
        for (String key : correlationContextMap.keySet()) {
            telemetry.getProperties().putIfAbsent(key, correlationContextMap.get(key));
        }
    }
}
