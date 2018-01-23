package com.microsoft.applicationinsights.smoketest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.zip.GZIPInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.io.CharStreams;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.microsoft.applicationinsights.internal.schemav2.Base;
import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.DataPointType;
import com.microsoft.applicationinsights.internal.schemav2.Domain;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.EventData;
import com.microsoft.applicationinsights.internal.schemav2.ExceptionData;
import com.microsoft.applicationinsights.internal.schemav2.MessageData;
import com.microsoft.applicationinsights.internal.schemav2.MetricData;
import com.microsoft.applicationinsights.internal.schemav2.PerformanceCounterData;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.internal.schemav2.SessionStateData;
import com.microsoft.applicationinsights.telemetry.Duration;

public class MockedAppInsightsIngestion implements AutoCloseable {
	public static final int DEFAULT_PORT = 60606;
	private final Server server;

	// FIXME deserialize to object instead of string
	private final Queue<Envelope> telemetryReceived = new ConcurrentLinkedDeque<Envelope>();
	private final ListMultimap<String, Envelope> type2envelope = MultimapBuilder.treeKeys().arrayListValues().build();

	public static final String ENDPOINT_HEALTH_CHECK_RESPONSE = "Fake AI Endpoint Online";
	public static final String PING = "PING";
	public static final String PONG = "PONG";

	public MockedAppInsightsIngestion() {
		server = new Server(DEFAULT_PORT);
		ServletHandler handler = new ServletHandler();
		server.setHandler(handler);

		handler.addServletWithMapping(new ServletHolder(new MockedAppInsightsIngestionServlet()), "/*");
		// TODO add another mapping for /api/profiles? as seen in node.js sdk functional tests.
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
		System.out.println("Clearing fake ingestion accumulator...");
		this.telemetryReceived.clear();
	}

	public boolean hasData() {
		return !telemetryReceived.isEmpty();
	}

	public int getItemCount() {
		return telemetryReceived.size();
	}

	public Envelope nextItem() {
		return telemetryReceived.poll();
	}

	public int getCountForType(String type) {
		Preconditions.checkNotNull(type, "type");
		return getItemsByType(type).size();
	}

	public List<Envelope> getItemsByType(String type) {
		Preconditions.checkNotNull(type, "type");
		return type2envelope.get(type);
	}

	@Override
	public void close() throws Exception {
		stopServer();
	}

	public class MockedAppInsightsIngestionServlet extends HttpServlet {
		public static final long serialVersionUID = -1;

		private final String appid = "DUMMYAPPID";

		private void logit(String message) {
			System.out.println("FAKE INGESTION: INFO - "+message);
		}

		private void logerr(String message, Exception e) {
			System.err.println("FAKE INGESTION: ERROR - "+message);
			if (e != null){
				e.printStackTrace();
			}
		}

		@Override
		protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			logit("caught: POST "+req.getPathInfo());
			
			switch (req.getPathInfo()) {
				case "/v2/track":
					StringWriter w = new StringWriter();
					try {
						String contentEncoding = req.getHeader("content-encoding");
						final Readable reader;
						if ("gzip".equals(contentEncoding)) {
							reader = new InputStreamReader(new GZIPInputStream(req.getInputStream()));
						}
						else {
							reader = req.getReader();
						}
						
						CharStreams.copy(reader, w);
						String body = w.toString();
						if (PING.equals(body)) {
							logit("Ping received for /v2/track");
							resp.getWriter().append(PONG);
						}
						else {
							logit("Deserializing payload...");
							// logit(body); // FIXME this should print if debug logging is enabled... not sure how to turn that on yet
							String[] lines = body.split("\n");
							for (String line : lines) {
								Envelope envelope;
								try {
									envelope = JsonHelper.GSON.fromJson(line.trim(), Envelope.class);
								}
								catch (JsonSyntaxException jse) {
									logerr("Could not deserialize to Envelope", jse);
									throw jse;
								}
								String baseType = envelope.getData().getBaseType();
								logit("Adding telemetry item: "+baseType);
								type2envelope.put(baseType, envelope);
								telemetryReceived.offer(envelope);
							}
						}
						resp.setStatus(200);
						return;
					}
					catch (Exception e) {
						e.printStackTrace();
						resp.sendError(500, e.getLocalizedMessage());
						return;
					}
					finally {
						w.close();
					}
				default:
					resp.sendError(404, "Unknown URI");
					return;
			}
		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			System.out.println("caught: GET "+req.getPathInfo());
			switch (req.getPathInfo()) {
				case "/":
					resp.getWriter().append(ENDPOINT_HEALTH_CHECK_RESPONSE);
					resp.setStatus(200);
					return;
				case "/api/profiles": // FIXME is this needed?
					resp.getWriter().append(this.appid);
					resp.setStatus(200);
					return;
				default:
					resp.sendError(404, "Unknown URI");
			}
		}
	}

	

	public static void main(String args[]) throws Exception {
		final MockedAppInsightsIngestion i = new MockedAppInsightsIngestion();
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