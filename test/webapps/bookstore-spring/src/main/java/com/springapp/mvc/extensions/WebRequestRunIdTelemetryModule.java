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

package com.springapp.mvc.extensions;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.extensibility.modules.WebTelemetryModule;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by yonisha on 6/21/2015.
 *
 * This module reads teh 'runid' query parameter, if exists, from the incoming http request and adds it
 * as a property to the RequestTelemetry sent to AI.
 * This run ID is then used to identify requests sent as part of a test suite.
 */
public class WebRequestRunIdTelemetryModule implements WebTelemetryModule<HttpServletRequest, HttpServletResponse>, TelemetryModule {

    protected static final String RUN_ID_QUERY_PARAM_NAME = "runid";

    /**
     * The {@link RequestTelemetryContext} instance propogated from
     * {@link com.microsoft.applicationinsights.web.internal.httputils.HttpServerHandler}
     */
    private RequestTelemetryContext requestTelemetryContext;

    public void setRequestTelemetryContext(
        RequestTelemetryContext requestTelemetryContext) {
        this.requestTelemetryContext = requestTelemetryContext;
    }

    /** Used for test */
    RequestTelemetryContext getRequestTelemetryContext() {
        return this.requestTelemetryContext;
    }

    @Override
    public void initialize(TelemetryConfiguration telemetryConfiguration) {
    }

    @Override
    public void onBeginRequest(HttpServletRequest req, HttpServletResponse res) {
        String queryString = req.getQueryString();

        String runId = getRunIdFromQueryString(queryString);

        if (runId == null) {
            return;
        }

        RequestTelemetry httpRequestTelemetry = this.requestTelemetryContext.getHttpRequestTelemetry();
        httpRequestTelemetry.getProperties().put(RUN_ID_QUERY_PARAM_NAME, runId);
    }

    private String getRunIdFromQueryString(String queryString) {
        if (LocalStringsUtils.isNullOrEmpty(queryString)) {
            return null;
        }

        String[] parameters = queryString.split("&");

        for (String parameter : parameters) {
            String[] paramWithValue = parameter.split("=");

            String param = paramWithValue[0];

            if (param.equalsIgnoreCase(RUN_ID_QUERY_PARAM_NAME)) {
                return paramWithValue[1];
            }
        }

        return null;
    }

    @Override
    public void onEndRequest(HttpServletRequest req, HttpServletResponse res) {
    }
}