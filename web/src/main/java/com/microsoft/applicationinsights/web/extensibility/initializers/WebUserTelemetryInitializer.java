/*
 * AppInsights-Java
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

import java.util.Date;
import com.google.common.base.Strings;
import com.microsoft.applicationinsights.extensibility.context.UserContext;
import com.microsoft.applicationinsights.telemetry.HttpRequestTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.web.internal.ThreadContext;

/**
 * Created by yonisha on 3/5/2015.
 */
public class WebUserTelemetryInitializer extends WebTelemetryInitializerBase {

    /**
     * Initializes the properties of the given telemetry.
     *
     * @param telemetry The {@link com.microsoft.applicationinsights.telemetry.Telemetry} to initialize.
     */
    @Override
    protected void onInitializeTelemetry(Telemetry telemetry) {
        UserContext userContext = telemetry.getContext().getUser();

        if (!Strings.isNullOrEmpty(userContext.getId())) {
            return;
        }

        HttpRequestTelemetry requestTelemetry = ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry();
        UserContext requestUserContext = requestTelemetry.getContext().getUser();

        userContext.setId(requestUserContext.getId());

        Date requestUserAcquisitionDate = requestUserContext.getAcquisitionDate();
        if (requestUserAcquisitionDate != null) {
            userContext.setAcquisitionDate(requestUserAcquisitionDate);
        }
    }
}
