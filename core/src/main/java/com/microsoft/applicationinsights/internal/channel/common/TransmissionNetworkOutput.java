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

package com.microsoft.applicationinsights.internal.channel.common;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.microsoft.applicationinsights.internal.channel.TransmissionDispatcher;
import com.microsoft.applicationinsights.internal.channel.TransmissionHandlerArgs;
import com.microsoft.applicationinsights.internal.channel.TransmissionOutput;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

/**
 * The class is responsible for the actual sending of
 * {@link com.microsoft.applicationinsights.internal.channel.common.Transmission}
 *
 * The class uses Apache's HttpClient framework for that.
 *
 * Created by gupele on 12/18/2014.
 */
public final class TransmissionNetworkOutput implements TransmissionOutput {	
	private final static String CONTENT_TYPE_HEADER = "Content-Type";
	private final static String CONTENT_ENCODING_HEADER = "Content-Encoding";
	private final static String RESPONSE_THROTTLING_HEADER = "Retry-After";

	private final static String DEFAULT_SERVER_URI = "https://dc.services.visualstudio.com/v2/track";

	// For future use: re-send a failed transmission back to the dispatcher
	private TransmissionDispatcher transmissionDispatcher;

	private final String serverUri;

	private volatile boolean stopped;

	// Use one instance for optimization
	private final ApacheSender httpClient;

	private TransmissionPolicyManager transmissionPolicyManager;

	/**
	 * Creates an instance of the network transmission class.
	 * <p>
	 * Will use the DEFAULT_SERVER_URI for the endpoint.
	 * 
	 * @param transmissionPolicyManager
	 *            The transmission policy used to mark this sender active or
	 *            blocked.
	 * @return
	 */
	public static TransmissionNetworkOutput create(TransmissionPolicyManager transmissionPolicyManager) {
		return create(DEFAULT_SERVER_URI, transmissionPolicyManager);
	}

	/**
	 * Creates an instance of the network transmission class.
	 * 
	 * @param endpoint
	 *            The HTTP endpoint to send our telemetry too.
	 * @param transmissionPolicyManager
	 *            The transmission policy used to mark this sender active or
	 *            blocked.
	 * @return
	 */
	public static TransmissionNetworkOutput create(String endpoint,
			TransmissionPolicyManager transmissionPolicyManager) {
		String realEndpoint = Strings.isNullOrEmpty(endpoint) ? DEFAULT_SERVER_URI : endpoint;
		return new TransmissionNetworkOutput(realEndpoint, transmissionPolicyManager);
	}

	/**
	 * Private Ctor to initialize class.
	 * <p>
	 * Also creates the httpClient using the ApacheSender instance
	 * 
	 * @param serverUri
	 *            The HTTP endpoint to send our telemetry too.
	 * @param transmissionPolicyManager
	 */
	private TransmissionNetworkOutput(String serverUri, TransmissionPolicyManager transmissionPolicyManager) {
		Preconditions.checkNotNull(serverUri, "serverUri should be a valid non-null value");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(serverUri), "serverUri should be a valid non-null value");
		Preconditions.checkNotNull(transmissionPolicyManager,
				"transmissionPolicyManager should be a valid non-null value");

		this.serverUri = serverUri;

		httpClient = ApacheSenderFactory.INSTANCE.create();
		this.transmissionPolicyManager = transmissionPolicyManager;
		stopped = false;

	}

	/**
	 * Used to inject the dispatcher used for this output so it can be injected to
	 * the retry logic.
	 * 
	 * @param transmissionDispatcher
	 *            The dispatcher to be injected.
	 */
	public void setTransmissionDispatcher(TransmissionDispatcher transmissionDispatcher) {
		this.transmissionDispatcher = transmissionDispatcher;
	}

	/**
	 * Stops all threads from sending data.
	 * 
	 * @param timeout
	 *            The timeout to wait, which is not relevant here.
	 * @param timeUnit
	 *            The time unit, which is not relevant in this method.
	 */
	@Override
	public synchronized void stop(long timeout, TimeUnit timeUnit) {
		if (stopped) {
			return;
		}

		httpClient.close();
		stopped = true;
	}

	/**
	 * Tries to send a
	 * {@link com.microsoft.applicationinsights.internal.channel.common.Transmission}
	 * The thread that calls that method might be suspended if there is a throttling
	 * issues, in any case the thread that enters this method is responsive for
	 * 'stop' request that might be issued by the application.
	 * 
	 * @param transmission
	 *            The data to send
	 * @return True when done.
	 */
	@Override
	public boolean send(Transmission transmission) {
		if (!stopped) {
			// If we're not stopped but in a blocked state then fail to second
			// TransmissionOutput
			if (transmissionPolicyManager.getTransmissionPolicyState()
					.getCurrentState() != TransmissionPolicy.UNBLOCKED) {
				return false;
			}

			HttpResponse response = null;
			HttpPost request = null;
			int code = 0;
			String respString = null;
			Throwable ex = null;
			Header retryAfterHeader = null;
			try {
				// POST the transmission data to the endpoint
				request = createTransmissionPostRequest(transmission);
				httpClient.enhanceRequest(request);
				response = httpClient.sendPostRequest(request);
				HttpEntity respEntity = response.getEntity();
				code = response.getStatusLine().getStatusCode();
				respString = EntityUtils.toString(respEntity);
				retryAfterHeader = response.getFirstHeader(RESPONSE_THROTTLING_HEADER);

				// After we reach our instant retry limit we should fail to second TransmissionOutput
				if (code > HttpStatus.SC_PARTIAL_CONTENT && transmission.getNumberOfSends() > this.transmissionPolicyManager.getMaxInstantRetries()) {
					return false;
				} else if (code == HttpStatus.SC_OK) {
					// If we've completed then clear the back off flags as the channel does not need
					// to be throttled
					transmissionPolicyManager.clearBackoff();
				}
				return true;

			} catch (ConnectionPoolTimeoutException e) {
				ex = e;
				InternalLogger.INSTANCE.error("Failed to send, connection pool timeout exception%nStack Trace:%n%s",
						ExceptionUtils.getStackTrace(e));
			} catch (SocketException e) {
				ex = e;
				InternalLogger.INSTANCE.error("Failed to send, socket exception.%nStack Trace:%n%s",
						ExceptionUtils.getStackTrace(e));
			} catch (UnknownHostException e) {
				ex = e;
				InternalLogger.INSTANCE.error(
						"Failed to send, wrong host address or cannot reach address due to network issues.%nStack Trace:%n%s",
						ExceptionUtils.getStackTrace(e));
			} catch (IOException ioe) {
				ex = ioe;
				InternalLogger.INSTANCE.error("Failed to send.%nStack Trace:%n%s", ExceptionUtils.getStackTrace(ioe));
			} catch (Exception e) {
				ex = e;
				InternalLogger.INSTANCE.error("Failed to send, unexpected exception.%nStack Trace:%n%s",
						ExceptionUtils.getStackTrace(e));
			} catch (Throwable t) {
				ex = t;
				InternalLogger.INSTANCE.error("Failed to send, unexpected error.%nStack Trace:%n%s",
						ExceptionUtils.getStackTrace(t));
			} finally {
				if (request != null) {
					request.releaseConnection();
				}
				httpClient.dispose(response);

				if (code != HttpStatus.SC_OK) {
					// Invoke the listeners for handling things like errors
					// The listeners will handle the back off logic as well as the dispatch
					// operation
					TransmissionHandlerArgs args = new TransmissionHandlerArgs();
					args.setTransmission(transmission);
					args.setTransmissionDispatcher(transmissionDispatcher);
					args.setResponseBody(respString);
					args.setResponseCode(code);
					args.setException(ex);
					args.setRetryHeader(retryAfterHeader);
					this.transmissionPolicyManager.onTransmissionSent(args);
				}
			}
		}
		// If we end up here we've hit an error code we do not expect (403, 401, 400,
		// etc.)
		// This also means that unless there is a TransmissionHandler for this code we
		// will not retry.
		return true;
	}

	/**
	 * Generates the HTTP POST to send to the endpoint.
	 * 
	 * @param transmission
	 *            The transmission to send.
	 * @return The completed {@link HttpPost} object
	 */
	private HttpPost createTransmissionPostRequest(Transmission transmission) {
		HttpPost request = new HttpPost(serverUri);
		request.addHeader(CONTENT_TYPE_HEADER, transmission.getWebContentType());
		request.addHeader(CONTENT_ENCODING_HEADER, transmission.getWebContentEncodingType());

		ByteArrayEntity bae = new ByteArrayEntity(transmission.getContent());
		request.setEntity(bae);

		return request;
	}

}
