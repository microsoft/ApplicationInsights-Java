package com.microsoft.applicationinsights.test.fakeingestion;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;
import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Domain;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class MockedAppInsightsIngestionServer implements AutoCloseable {
	public static final int DEFAULT_PORT = 60606;

	private final MockedAppInsightsIngestionServlet servlet;
	private final Server server;

	public MockedAppInsightsIngestionServer() {
		server = new Server(DEFAULT_PORT);
		ServletHandler handler = new ServletHandler();
		server.setHandler(handler);

		servlet = new MockedAppInsightsIngestionServlet();

		handler.addServletWithMapping(new ServletHolder(servlet), "/*");
	}

	public int getPort() {
		return DEFAULT_PORT; // TODO this could be configurable
	}

	public void startServer() throws Exception {
		System.out.println("Starting fake ingestion...");
		server.start();
	}

	public void stopServer() throws Exception {
		System.out.println("Stopping fake ingestion...");
		server.stop();
		server.join();
	}

	public void resetData() {
		this.servlet.resetData();
	}

	public boolean hasData() {
		return this.servlet.hasData();
	}

	public int getItemCount() {
		return this.servlet.getItemCount();
	}

	public Envelope nextItem() {
		return this.servlet.nextItem();
	}

	public int getCountForType(String type) {
		Preconditions.checkNotNull(type, "type");
		return getItemsByType(type).size();
	}

	public List<Envelope> getItemsByType(String type) {
		return this.servlet.getItemsByType(type);
	}

	public <T extends Domain> List<T> getTelemetryDataByType(String type) {
		Preconditions.checkNotNull(type, "type");
		List<Envelope> items = getItemsByType(type);
		List<T> dataItems = new ArrayList<T>();
		for (Envelope e : items) {
			Data<T> dt = (Data<T>) e.getData();
			dataItems.add(dt.getBaseData());
		}
		return dataItems;
	}

	public <T extends Domain> T getBaseDataForType(int index, String type) {
		Data<T> data = (Data<T>) getItemsByType(type).get(index).getData();
		return data.getBaseData();
	}

	@Override
	public void close() throws Exception {
		stopServer();
	}


	public static void main(String args[]) throws Exception {
		final MockedAppInsightsIngestionServer i = new MockedAppInsightsIngestionServer();
		System.out.println("Starting mocked ingestion on port "+DEFAULT_PORT);
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					i.stopServer();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}));
		i.startServer();
	}
}