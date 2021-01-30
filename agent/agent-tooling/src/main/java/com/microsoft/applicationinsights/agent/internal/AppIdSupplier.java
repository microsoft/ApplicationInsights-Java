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

package com.microsoft.applicationinsights.agent.internal;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.web.internal.correlation.InstrumentationKeyResolver;
import io.opentelemetry.instrumentation.api.aiappid.AiAppId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppIdSupplier implements AiAppId.Supplier {

    private static final Logger logger = LoggerFactory.getLogger(AppIdSupplier.class);

    // note: app id is used by distributed trace headers and (soon) jfr profiling
    public static void registerAndTriggerResolution() {
        AppIdSupplier supplier = new AppIdSupplier();
        AiAppId.setSupplier(supplier);
        // early resolution of the app helps distributed trace headers usage:
        //     in order to not block app requests, when the app id is not already resolved,
        //     distributed trace headers will not wait for app id resolution, and so
        //     distributed traces over multiple instrumentation keys will not be correlated
        // and also (soon) should help jfr profiling usage
        supplier.get();
    }

    public String get() {
        String instrumentationKey = TelemetryConfiguration.getActive().getInstrumentationKey();
        if (instrumentationKey == null) {
            // this is possible in Azure Function consumption plan prior to "specialization"
            return null;
        }

        String appId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey(instrumentationKey, TelemetryConfiguration.getActive());

        //it's possible the appId returned is null (e.g. async task is still pending or has failed). In this case, just
        //return and let the next request resolve the ikey.
        if (appId == null) {
            logger.debug("Application correlation Id could not be retrieved (e.g. task may be pending or failed)");
            return "";
        }
        return appId;
    }
}
