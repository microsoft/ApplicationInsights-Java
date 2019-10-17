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

package com.microsoft.applicationinsights.agentc.internal.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.correlation.InstrumentationKeyResolver;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.glowroot.instrumentation.api.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TelemetryCorrelationUtilsCore {

    private static final Logger logger = LoggerFactory.getLogger(TelemetryCorrelationUtilsCore.class);

    static final String CORRELATION_HEADER_NAME = "Request-Id";

    private static final String CORRELATION_CONTEXT_HEADER_NAME = "Correlation-Context";
    private static final String REQUEST_CONTEXT_HEADER_NAME = "Request-Context";
    private static final String REQUEST_CONTEXT_HEADER_APPID_KEY = "appId";
    private static final String REQUEST_CONTEXT_HEADER_ROLENAME_KEY = "roleName";
    private static final int REQUESTID_MAXLENGTH = 1024;

    private TelemetryCorrelationUtilsCore() {
    }

    /**
     * Resolves the source of a request based on request header information and the appId of the current
     * component, which is retrieved via a query to the AppInsights service.
     *
     * @param request            The servlet request.
     * @param requestTelemetry   The request telemetry in which source will be populated.
     * @param instrumentationKey The instrumentation key for the current component.
     */
    public static <Req> void resolveRequestSource(Req request, Getter<Req> requestHeaderGetter,
                                                  RequestTelemetry requestTelemetry,
                                                  @Nullable String instrumentationKey) {

        try {
            String requestContext = requestHeaderGetter.get(request, REQUEST_CONTEXT_HEADER_NAME);
            if (Strings.isNullOrEmpty(requestContext)) {
                logger.debug("Skip resolving request source as the following header was not found: {}",
                        REQUEST_CONTEXT_HEADER_NAME);
                return;
            }

            if (instrumentationKey == null || instrumentationKey.isEmpty()) {
                logger.debug("Failed to resolve correlation. InstrumentationKey is null or empty.");
                return;
            }

            String source = generateSourceTargetCorrelation(instrumentationKey, requestContext);
            requestTelemetry.setSource(source);
        } catch (Exception e) {
            logger.debug("Failed to resolve request source: ", e.toString());
            logger.trace("Failed to resolve request source", e);
        }
    }

    public static boolean isHierarchicalId(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }

        return id.charAt(0) == '|';
    }

    /**
     * Extracts the appId/roleName out of requestContext and compares it with the current appId. It then
     * generates the appropriate source or target.
     */
    private static String generateSourceTargetCorrelation(String instrumentationKey, String requestContext) {
        String appId = getKeyValueHeaderValue(requestContext, REQUEST_CONTEXT_HEADER_APPID_KEY);
        String roleName = getKeyValueHeaderValue(requestContext, REQUEST_CONTEXT_HEADER_ROLENAME_KEY);

        if (appId == null && roleName == null) {
            return null;
        }

        String myAppId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey(instrumentationKey);

        // it's possible the appId returned is null (e.g. async task is still pending or has failed). In this case,
        // just return and let the next request resolve the ikey.
        if (myAppId == null) {
            logger.trace("Could not generate source/target correlation as the appId could not be resolved" +
                    " (task may be pending or failed)");
            return null;
        }

        String result = null;
        if (appId != null && !appId.equals(myAppId)) {
            result = appId;
        }

        if (roleName != null) {
            if (result != null) {
                result += " | roleName:" + roleName;
            } else {
                result = "roleName:" + roleName;
            }
        }

        return result;
    }

    /**
     * Extracts the value of a "Key-Value" type of header. For example, for a header with value: "foo=bar, name=joe",
     * we can extract "joe" with a call to this method passing the key "name".
     *
     * @param headerFullValue The entire header value.
     * @param key             They key for which to extract the value
     * @return The extracted value
     */
    private static String getKeyValueHeaderValue(String headerFullValue, String key) {
        return getPropertyBag(headerFullValue).get(key);
    }

    private static Map<String, String> getPropertyBag(String baggage) {

        Map<String, String> result = new HashMap<>();

        String[] pairs = baggage.split(",");
        for (String pair : pairs) {
            String[] keyValuePair = pair.trim().split("=");
            if (keyValuePair.length == 2) {
                String key = keyValuePair[0].trim();
                String value = keyValuePair[1].trim();
                result.put(key, value);
            }
        }

        return result;
    }


    static String extractRootId(String parentId) {
        // ported from .NET's System.Diagnostics.Activity.cs implementation:
        // https://github.com/dotnet/corefx/blob/master/src/System.Diagnostics
        // .DiagnosticSource/src/System/Diagnostics/Activity.cs

        int rootEnd = parentId.indexOf('.');
        if (rootEnd < 0) {
            rootEnd = parentId.length();
        }

        int rootStart = parentId.charAt(0) == '|' ? 1 : 0;

        return parentId.substring(rootStart, rootEnd);
    }

    private static String generateRootId() {
        return uuidToStringNoDashes(UUID.randomUUID());
    }

    static String uuidToStringNoDashes(UUID guid) {
        long most = guid.getMostSignificantBits();
        long least = guid.getLeastSignificantBits();
        long msb = 1L << 32;
        return Long.toHexString(msb | ((most >> 32) & (msb - 1))).substring(1)
                + Long.toHexString(msb | (most & (msb - 1))).substring(1)
                + Long.toHexString(msb | ((least >> 32) & (msb - 1))).substring(1)
                + Long.toHexString(msb | (least & (msb - 1))).substring(1);
    }

    private static String generateId(String parentId) {
        String sanitizedParentId = sanitizeParentId(parentId);
        String suffix = generateSuffix();

        // handle overflow
        if (sanitizedParentId.length() + suffix.length() > REQUESTID_MAXLENGTH) {
            return shortenId(sanitizedParentId, suffix);
        }

        return sanitizedParentId + suffix + "_";
    }

    private static String shortenId(String parentId, String suffix) {

        // ported from .NET's System.Diagnostics.Activity.cs implementation:
        // https://github.com/dotnet/corefx/blob/master/src/System.Diagnostics
        // .DiagnosticSource/src/System/Diagnostics/Activity.cs
        int trimPosition = REQUESTID_MAXLENGTH - 9; // make room for suffix + delimiter
        while (trimPosition > 1) {
            if (parentId.charAt(trimPosition - 1) == '.' || parentId.charAt(trimPosition - 1) == '_')
                break;
            trimPosition--;
        }

        // parentId is not a valid requestId, so generate one.
        if (trimPosition == 1) {
            return "|" + generateRootId() + ".";
        }

        return parentId.substring(0, trimPosition) + suffix + '#';
    }

    private static String sanitizeParentId(String parentId) {

        String result = parentId;
        if (!isHierarchicalId(parentId)) {
            result = "|" + result;
        }

        char lastChar = parentId.charAt(parentId.length() - 1);
        if (lastChar != '.' && lastChar != '_') {
            result = result + '.';
        }

        return result;
    }

    private static String generateSuffix() {
        // using ThreadLocalRandom instead of Random to avoid multi-threaded contention which would 
        // result in poor performance.
        int randomNumber = ThreadLocalRandom.current().nextInt();
        return String.format("%08x", randomNumber);
    }
}