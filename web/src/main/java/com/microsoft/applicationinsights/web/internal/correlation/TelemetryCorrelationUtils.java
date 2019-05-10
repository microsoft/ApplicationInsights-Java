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

package com.microsoft.applicationinsights.web.internal.correlation;

import java.util.Enumeration;

import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.correlation.TelemetryCorrelationUtilsCore.RequestHeaderGetter;
import com.microsoft.applicationinsights.web.internal.correlation.TelemetryCorrelationUtilsCore.ResponseHeaderSetter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TelemetryCorrelationUtils {

    public static final String CORRELATION_HEADER_NAME = "Request-Id";
    public static final String CORRELATION_CONTEXT_HEADER_NAME = "Correlation-Context";
    public static final String REQUEST_CONTEXT_HEADER_NAME = "Request-Context";
    public static final String REQUEST_CONTEXT_HEADER_APPID_KEY = "appId";
    public static final String REQUEST_CONTEXT_HEADER_ROLENAME_KEY = "roleName";
    public static final int REQUESTID_MAXLENGTH = 1024;

    static final RequestHeaderGetter<HttpServletRequest> REQUEST_HEADER_GETTER = new RequestHeaderGetterImpl();
    static final ResponseHeaderSetter<HttpServletResponse> RESPONSE_HEADER_SETTER = new ResponseHeaderSetterImpl();

    private TelemetryCorrelationUtils() {}

    /**
     * Resolves correlation ID's by parsing well-known correlation headers in the request.
     * @param request The servlet request.
     * @param requestTelemetry The request telemetry to be populated with correlation ID's.
     */
    public static void resolveCorrelation(HttpServletRequest request, HttpServletResponse response, RequestTelemetry requestTelemetry) {
        TelemetryCorrelationUtilsCore.resolveCorrelation(request, REQUEST_HEADER_GETTER, response, RESPONSE_HEADER_SETTER, requestTelemetry);
    }

    /**
     * Generates a child Id for dependencies. Dependencies are children of requests and, therefore, their ID's
     * reflect this. The generated ID is based on the current request scope (stored in TLS).
     * @return The child Id.
     */
    public static String generateChildDependencyId() {
        return TelemetryCorrelationUtilsCore.generateChildDependencyId();
    }

    /**
     * Retrieves the currently stored correlation context from the request context.
     * @return The correlation context as a string.
     */
    public static String retrieveCorrelationContext() {
        return TelemetryCorrelationUtilsCore.retrieveCorrelationContext();
    }

    /**
     * Retrieves the appId (in correlation format) for the current active config's instrumentation key.
     */
    public static String retrieveApplicationCorrelationId() {
        return TelemetryCorrelationUtilsCore.retrieveApplicationCorrelationId();
    }

    /**
     * Given a request context, it generates a new dependency target, possibly including the appId found in
     * the given Request-Context.
     * @param requestContext - the Request-Context header value
     * @return the dependency target
     */
    public static String generateChildDependencyTarget(String requestContext) {
        return TelemetryCorrelationUtilsCore.generateChildDependencyTarget(requestContext);
    }

    /**
     * Resolves the source of a request based on request header information and the appId of the current
     * component, which is retrieved via a query to the AppInsights service.
     * @param request The servlet request.
     * @param requestTelemetry The request telemetry in which source will be populated.
     * @param instrumentationKey The instrumentation key for the current component.
     */
    public static void resolveRequestSource(HttpServletRequest request, RequestTelemetry requestTelemetry, String instrumentationKey) {
        TelemetryCorrelationUtilsCore.resolveRequestSource(request, REQUEST_HEADER_GETTER, requestTelemetry, instrumentationKey);
    }

    public static boolean isHierarchicalId(String id) {
        return TelemetryCorrelationUtilsCore.isHierarchicalId(id);
    }
    
    private static class RequestHeaderGetterImpl implements RequestHeaderGetter<HttpServletRequest> {

        @Override
        public String getFirst(HttpServletRequest request, String name) {
            return request.getHeader(name);
        }

        @Override
        public Enumeration<String> getAll(HttpServletRequest request, String name) {
            return request.getHeaders(name);
        }
    }
    
    private static class ResponseHeaderSetterImpl implements ResponseHeaderSetter<HttpServletResponse> {

        @Override
        public boolean containsHeader(HttpServletResponse response, String name) {
            return response.containsHeader(name);
        }

        @Override
        public void addHeader(HttpServletResponse response, String name, String value) {
            response.addHeader(name, value);
        }
    }
}