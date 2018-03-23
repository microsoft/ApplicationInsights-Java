package com.microsoft.applicationinsights.internal.channel.common;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.common.base.Optional;
import com.microsoft.applicationinsights.internal.channel.TransmissionHandler;
import com.microsoft.applicationinsights.internal.channel.TransmissionHandlerArgs;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;

/**
 * This class implements the retry logic for partially accepted transmissions.
 * HTTP status code 206.
 * <p>
 * 
 * @see <a href=
 *      "https://github.com/Microsoft/ApplicationInsights-dotnet/blob/master/docs/ServerTelemetryChannel%20error%20handling.md#partialsuccesstransmissionpolicy">PartialSuccessTransmissionPolicy</a>
 * @author jamdavi
 *
 */
public class PartialSuccessHandler implements TransmissionHandler {

	/**
	 * Ctor
	 * 
	 * Constructs the PartialSuccessHandler object.
	 * 
	 * @param policy
	 *            The {@link TransmissionPolicyManager} object that is needed to
	 *            control the back off policy.
	 */
	public PartialSuccessHandler(TransmissionPolicyManager policy) {
	}

	@Override
	public void onTransmissionSent(TransmissionHandlerArgs args) {
		validateTransmissionAndSend(args);
	}

	/**
	 * Provides the core logic for the retransmission
	 * 
	 * @param args
	 *            The {@link TransmissionHandlerArgs} for this transmission.
	 * @return Returns a pass/fail for handling this transmission.
	 */
	boolean validateTransmissionAndSend(TransmissionHandlerArgs args) {
		if (args.getTransmission() != null && args.getTransmissionDispatcher() != null) {
			switch (args.getResponseCode()) {
			case HttpStatus.SC_PARTIAL_CONTENT:
				BackendResponse backendResponse = getBackendResponse(args.getResponseBody());
				List<String> originalItems = generateOriginalItems(args);

				// Somehow the amount of items received and the items sent do not match
				if (backendResponse != null && (originalItems.size() != backendResponse.itemsReceived)) {
					InternalLogger.INSTANCE.trace(
							"Skipping partial content handler due to itemsReceived being larger than the items sent.");
					return false;
				}

				if (backendResponse != null && (backendResponse.itemsAccepted < backendResponse.itemsReceived)) {
					List<String> newTransmission = new ArrayList<String>();
					for (BackendResponse.Error e : backendResponse.errors) {
						switch (e.statusCode) {
						case TransmissionSendResult.REQUEST_TIMEOUT:
						case TransmissionSendResult.INTERNAL_SERVER_ERROR:
						case TransmissionSendResult.SERVICE_UNAVAILABLE:
						case TransmissionSendResult.THROTTLED: 
						case TransmissionSendResult.THROTTLED_OVER_EXTENDED_TIME: 
							// Unknown condition where backend response returns an index greater than the
							// items we're returning
							if (e.index < originalItems.size()) {
								newTransmission.add(originalItems.get(e.index));
							}
							break;
						}
					}
					return sendNewTransmission(args, newTransmission);
				}
				InternalLogger.INSTANCE
						.trace("Skipping partial content handler due to itemsAccepted and itemsReceived being equal.");
				return false;

			default:
				InternalLogger.INSTANCE.trace("Http response code %s not handled by %s", args.getResponseCode(),
						this.getClass().getName());
				return false;
			}
		}
		return false;
	}

	/**
	 * Used to parse the original telemetry request in order to resend the failed
	 * ones.
	 * 
	 * @param args
	 *            The {@link TransmissionHandlerArgs} that contains the
	 *            {@link Transmission} object.
	 * @return A List<> of each sent item
	 */
	List<String> generateOriginalItems(TransmissionHandlerArgs args) {
		List<String> originalItems = new ArrayList<String>();

		if (args.getTransmission().getWebContentEncodingType() == "gzip") {

			GZIPInputStream gis = null;
			BufferedReader bufferedReader = null;

			try {
				gis = new GZIPInputStream(
						new ByteArrayInputStream(args.getTransmission().getContent()));
				bufferedReader = new BufferedReader(new InputStreamReader(gis));
				String line;
				while ((line = bufferedReader.readLine()) != null) {
					originalItems.add(line);
				}
			} catch (IOException ex) {
				InternalLogger.INSTANCE.error("IOException: Error while reading the GZIP stream.%nStack Trace:%n%s", ExceptionUtils.getStackTrace(ex));
			} catch (Throwable t) {
				InternalLogger.INSTANCE.error("Error while reading the GZIP stream.%nStack Trace:%n%s",	ExceptionUtils.getStackTrace(t));
			} finally {
				if (gis != null) {
					try {
						gis.close();	
					} catch (IOException ex){
						InternalLogger.INSTANCE.warn("Error while closing the GZIP stream.%nStack Trace:%n%s",	ExceptionUtils.getStackTrace(ex));
					}
				}
				if (bufferedReader != null) {
					try {
						bufferedReader.close();	
					} catch (IOException ex){
						InternalLogger.INSTANCE.warn("Error while closing the buffered reader.%nStack Trace:%n%s",	ExceptionUtils.getStackTrace(ex));
					}
				}
			}
		} else {
			for (String s : new String(args.getTransmission().getContent()).split("\r\n")) {
				originalItems.add(s);
			}
		}
		return originalItems;
	}

	/**
	 * Sends a new transmission generated from the failed attempts from the original
	 * request.
	 * 
	 * @param args
	 *            The {@link TransmissionHandlerArgs} object that contains the
	 *            {@link TransmissionDispatcher}
	 * @param newTransmission
	 *            The {@link List} of items to resent
	 * @return A pass/fail response
	 */
	boolean sendNewTransmission(TransmissionHandlerArgs args, List<String> newTransmission) {
		if (!newTransmission.isEmpty()) {
			GzipTelemetrySerializer serializer = new GzipTelemetrySerializer();
			Optional<Transmission> newT = serializer.serialize(newTransmission);
			args.getTransmissionDispatcher().dispatch(newT.get());
			return true;
		}
		return false;
	}

	/**
	 * Helper method to parse the 206 response. Uses {@link Gson}
	 * 
	 * @param response
	 *            The body of the response.
	 * @return A {@link BackendResponse} object that contains the status of the
	 *         partial success.
	 */
	private BackendResponse getBackendResponse(String response) {

		BackendResponse backend = null;
		try {
			// Parse JSON to Java
			GsonBuilder gsonBuilder = new GsonBuilder();
			Gson gson = gsonBuilder.create();
			backend = gson.fromJson(response, BackendResponse.class);
		} catch (Throwable t) {
			InternalLogger.INSTANCE.trace(
					"Error deserializing backend response with Gson.%nStack Trace:%n%s",
					ExceptionUtils.getStackTrace(t));
		} finally {
		}
		return backend;
	}
}
