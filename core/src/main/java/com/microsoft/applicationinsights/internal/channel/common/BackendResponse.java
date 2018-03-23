package com.microsoft.applicationinsights.internal.channel.common;

/**
 * Utility class used by the {@link PartialSuccessHandler}
 * 
 * @author jamdavi
 *
 */
class BackendResponse {

	int itemsReceived;
	int itemsAccepted;
	Error[] errors;

	class Error {
		public int index;
		public int statusCode;
		public String message;
	}
}
