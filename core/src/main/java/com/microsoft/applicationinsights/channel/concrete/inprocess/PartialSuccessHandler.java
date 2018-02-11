package com.microsoft.applicationinsights.channel.concrete.inprocess;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpStatus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.common.base.Optional;
import com.microsoft.applicationinsights.internal.channel.TransmissionHandler;
import com.microsoft.applicationinsights.internal.channel.TransmissionHandlerArgs;
import com.microsoft.applicationinsights.internal.channel.common.GzipTelemetrySerializer;
import com.microsoft.applicationinsights.internal.channel.common.Transmission;
import com.microsoft.applicationinsights.internal.channel.common.TransmissionPolicyManager;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;

public class PartialSuccessHandler implements TransmissionHandler {

	public PartialSuccessHandler(TransmissionPolicyManager policy) {

	}

	@Override
	public void onTransmissionSent(TransmissionHandlerArgs args) {
		validateTransmissionAndSend(args);

	}

	public boolean validateTransmissionAndSend(TransmissionHandlerArgs args) {
		if (args.getTransmission() != null && args.getTransmissionDispatcher() != null) {
			switch (args.getResponseCode()) {
			case HttpStatus.SC_PARTIAL_CONTENT:
				BackendResponse beR = getBackendResponse(args.getResponseBody());
				
				// In case the 206 was false we can break here
				if(beR != null && (beR.itemsAccepted == beR.itemsReceived))
				{
					return false;
				}
			
				List<String> originalItems = generateOriginalItems(args);
				
				// Somehow the amount of items received and the items sent do not match
				if(beR != null && (originalItems.size() != beR.itemsReceived))
				{
					return false;
				}
			
				
				List<String> newTransmission = new ArrayList<String>();
				for (BackendResponse.Error e : beR.errors) {
					switch (e.statusCode) {
					case HttpStatus.SC_REQUEST_TIMEOUT:
					case HttpStatus.SC_INTERNAL_SERVER_ERROR:
					case HttpStatus.SC_SERVICE_UNAVAILABLE:
					case 429:
					case 439:
						// Unknown condition where backend response returns an index greater than the items we're returning
						if (e.index < originalItems.size()) {
							newTransmission.add(originalItems.get(e.index));	
						}
						break;
					}
				}
				
				return sendNewTransmission(args, newTransmission);
			default:
				InternalLogger.INSTANCE.trace("Http response code %s not handled by %s", args.getResponseCode(),
						this.getClass().getName());
				return false;
			}
		}
		return false;
	}

	public List<String> generateOriginalItems(TransmissionHandlerArgs args) {
		List<String> originalItems = new ArrayList<String>();
		

		if (args.getTransmission().getWebContentEncodingType() == "gzip") {
			
			try {
				GZIPInputStream gis = new GZIPInputStream(
						new ByteArrayInputStream(args.getTransmission().getContent()));
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(gis));
				String line;
				while ((line = bufferedReader.readLine()) != null) {
					originalItems.add(line);
				}
				if (gis != null)
				{
					gis.close();	
				}
				if (bufferedReader != null) 
				{
					bufferedReader.close();
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (Throwable t) {
				// TODO Auto-generated catch block
				t.printStackTrace();
			} finally {					
			}
		} else {
			for (String s : new String(args.getTransmission().getContent()).split("\r\n")) {
				originalItems.add(s);
			}
		}
		return originalItems;
	}

	public boolean sendNewTransmission(TransmissionHandlerArgs args, List<String> newTransmission) {
		if (!newTransmission.isEmpty())
		{
			GzipTelemetrySerializer serializer = new GzipTelemetrySerializer();
			Optional<Transmission> newT = serializer.serialize(newTransmission);
			args.getTransmissionDispatcher().dispatch(newT.get());
			return true;
		}
		return false;
	}

	private BackendResponse getBackendResponse(String response) {

		BackendResponse backend = null;
		try {
			// Parse JSON to Java
			GsonBuilder gsonBuilder = new GsonBuilder();
			Gson gson = gsonBuilder.create();
			backend = gson.fromJson(response, BackendResponse.class);
		} catch (Throwable t) {
			InternalLogger.INSTANCE.trace("Error deserializing backend response with Gson, message: %s",
					t.getMessage());
		} finally {
		}
		return backend;
	}
}
