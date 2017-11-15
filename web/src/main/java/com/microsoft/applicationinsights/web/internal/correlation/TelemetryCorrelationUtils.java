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

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import javax.servlet.http.HttpServletRequest;

public class TelemetryCorrelationUtils {

    public static final String CORRELATION_HEADER_NAME = "RequestId";
	public static final String CORRELATION_CONTEXT_HEADER_NAME = "Correlation-Context";
	public static final int REQUESTID_MAXLENGTH = 1024; 

	private TelemetryCorrelationUtils() {}

	public static void resolveCorrelation(HttpServletRequest request, RequestTelemetry requestTelemetry) {
		
		try {
			if (request == null) {
				InternalLogger.INSTANCE.error("Failed to resolve correlation. request is null.");
				return;
			}
	
			if (requestTelemetry == null) {
				InternalLogger.INSTANCE.error("Failed to resolve correlation. requestTelemetry is null.");
				return;
			}

			
			String rootId = null;
			String parentId = null;
			String currentId = null;

			String requestId = request.getHeader(CORRELATION_HEADER_NAME);

			if (requestId == null || requestId.isEmpty()) {
				// no incoming requestId, no parent.
				rootId = generateRootId();
				currentId = '|' + rootId + '.';
			} else {
				parentId = requestId;
				rootId = extractRootId(parentId);
				currentId = generateId(parentId);
			}

			requestTelemetry.setId(currentId);
			requestTelemetry.getContext().getOperation().setId(rootId);
			requestTelemetry.getContext().getOperation().setParentId(parentId);

		}
		catch(Exception ex) {
			InternalLogger.INSTANCE.error("Failed to resolve correlation. Exception information: " + ex);
		}
	}

	public static String generateChildDependencyId() {
		
		try {
			RequestTelemetryContext context = ThreadContext.getRequestTelemetryContext();
			RequestTelemetry requestTelemetry = context.getHttpRequestTelemetry();

			// if parentId is non-hierarchical, it means the incoming requestId
			// does not follow hierarchical convention, so we must not modify the children ID's.
			if (!isHierarchicalId((requestTelemetry.getContext().getOperation().getParentId()))) {
				return requestTelemetry.getContext().getOperation().getParentId();
			}

			return requestTelemetry.getId() + context.incrementChildId() + ".";
		}
		catch (Exception ex) {
			InternalLogger.INSTANCE.error("Failed to generate child ID. Exception information: " + ex);
		}

		return null;
	}

	public static boolean isHierarchicalId(String id) {
		if (id == null || id.isEmpty()) {
			return false;
		}

		return id.charAt(0) == '|';
	}

	private static String extractRootId(String parentId) {
		// ported from .NET's System.Diagnostics.Activity.cs implementation:
		// https://github.com/dotnet/corefx/blob/master/src/System.Diagnostics.DiagnosticSource/src/System/Diagnostics/Activity.cs
		
		int rootEnd = parentId.indexOf('.');
		if (rootEnd < 0) {
			rootEnd = parentId.length();
		}

		int rootStart = parentId.charAt(0) == '|' ? 1 : 0;

		return parentId.substring(rootStart, rootEnd);
	}

	private static String generateRootId() {
		UUID guid = UUID.randomUUID();
        long least = guid.getLeastSignificantBits();
        long most = guid.getMostSignificantBits();

        return Long.toHexString(most) + Long.toHexString(least);
	}

	private static String generateId(String parentId) {
		String sanitizedParentId = sanitizeParentId(parentId);
		String suffix = generateSuffix();

		//handle overflow
		if (sanitizedParentId.length() + suffix.length() > REQUESTID_MAXLENGTH) {
			return shortenId(sanitizedParentId, suffix);
		}

		return sanitizedParentId + suffix + "_";
	}

	private static String shortenId(String parentId, String suffix) {
		
		// ported from .NET's System.Diagnostics.Activity.cs implementation:
		// https://github.com/dotnet/corefx/blob/master/src/System.Diagnostics.DiagnosticSource/src/System/Diagnostics/Activity.cs
		int trimPosition = REQUESTID_MAXLENGTH - 9; // make room for suffix + delimiter
		while (trimPosition > 1)
		{
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