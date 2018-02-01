package com.microsoft.applicationinsights.test.fakeingestion;

import com.google.common.base.Preconditions;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.io.CharStreams;
import com.google.gson.JsonSyntaxException;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.JsonHelper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.zip.GZIPInputStream;

public class MockedAppInsightsIngestionServlet extends HttpServlet {
    public static final long serialVersionUID = -1;
    public static final String ENDPOINT_HEALTH_CHECK_RESPONSE = "Fake AI Endpoint Online";
    public static final String PING = "PING";
    public static final String PONG = "PONG";

    private final String appid = "DUMMYAPPID";

    private final Queue<Envelope> telemetryReceived = new ConcurrentLinkedDeque<Envelope>();
    private final ListMultimap<String, Envelope> type2envelope = MultimapBuilder.treeKeys().arrayListValues().build();

    private void logit(String message) {
        System.out.println("FAKE INGESTION: INFO - "+message);
    }

    private void logerr(String message, Exception e) {
        System.err.println("FAKE INGESTION: ERROR - "+message);
        if (e != null){
            e.printStackTrace();
        }
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

    public List<Envelope> getItemsByType(String type) {
        Preconditions.checkNotNull(type, "type");
        return type2envelope.get(type);
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
            case "/api/profiles":
                resp.getWriter().append(this.appid);
                resp.setStatus(200);
                return;
                // TODO create endpoint to retrieve telemetry data
            default:
                resp.sendError(404, "Unknown URI");
        }
    }
}
