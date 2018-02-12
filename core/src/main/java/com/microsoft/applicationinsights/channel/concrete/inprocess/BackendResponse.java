package com.microsoft.applicationinsights.channel.concrete.inprocess;

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
