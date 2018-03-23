package com.microsoft.applicationinsights.test.fakeingestion;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.io.CharStreams;
import com.google.gson.JsonSyntaxException;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.JsonHelper;

import javax.annotation.concurrent.GuardedBy;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.zip.GZIPInputStream;

public class MockedAppInsightsIngestionServlet extends HttpServlet {
    public static final long serialVersionUID = -1;
    public static final String ENDPOINT_HEALTH_CHECK_RESPONSE = "Fake AI Endpoint Online";
    public static final String PING = "PING";
    public static final String PONG = "PONG";

    private final String appid = "DUMMYAPPID";


    private final Queue<Envelope> telemetryReceived;
    @GuardedBy("multimapLock")
    private final ListMultimap<String, Envelope> type2envelope;
    private List<Predicate<Envelope>> filters;

    private final Object multimapLock = new Object();

    private MockedIngestionServletConfig config;

    private final ExecutorService itemExecutor = Executors.newSingleThreadExecutor();

    public static final String LOG_PAYLOADS_PARAMETER_KEY = "logPayloads";
    public static final String RETAIN_PAYLOADS_PARAMETER_KEY = "retainPayloads";

    public MockedAppInsightsIngestionServlet() {
        telemetryReceived = new ConcurrentLinkedDeque<Envelope>();
        type2envelope = MultimapBuilder.treeKeys().arrayListValues().build();
        filters = new ArrayList<>();
        config = new MockedIngestionServletConfig();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        Boolean retainPayloads = extractBooleanInitParam(RETAIN_PAYLOADS_PARAMETER_KEY, config);
        if (retainPayloads != null) {
            this.config.setRetainPayloadsEnabled(retainPayloads);
        }
        Boolean logPayloads = extractBooleanInitParam(LOG_PAYLOADS_PARAMETER_KEY, config);
        if (logPayloads != null) {
            this.config.setLogPayloadsEnabled(logPayloads);
        }
    }

    private Boolean extractBooleanInitParam(String key, ServletConfig config) {
        String value = config.getInitParameter(key);
        if (value == null) {
            return null;
        }

        try {
            return Boolean.valueOf(value);
        } catch (Exception e) {
            System.err.printf("could not parse init param as boolean: %s=%s%n", key, value);
            return null;
        }
    }

    private void logit(String message) {
        System.out.println("FAKE INGESTION: INFO - "+message);
    }

    private void logerr(String message, Exception e) {
        System.err.println("FAKE INGESTION: ERROR - "+message);
        if (e != null){
            e.printStackTrace();
        }
    }

    public void addIngestionFilter(Predicate<Envelope> filter) {
        this.filters.add(filter);
    }

    public MockedIngestionServletConfig getIngestionConfig() {
        return config;
    }

    public void resetData() {
        logit("Clearing telemetry accumulator...");
        telemetryReceived.clear();
        synchronized (multimapLock) {
            type2envelope.clear();
        }
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
        synchronized (multimapLock) {
            return type2envelope.get(type);
        }
    }

    public List<Envelope> waitForItems(final Predicate<Envelope> condition, final int numItems, int timeout, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        final Future<List<Envelope>> future = itemExecutor.submit(new Callable<List<Envelope>>() {
            @Override
            public List<Envelope> call() throws Exception {
                List<Envelope> targetCollection = new ArrayList<>(numItems);
                while(targetCollection.size() < numItems) {
                    targetCollection.clear();
                    final Collection<Envelope> currentValues;
                    synchronized (multimapLock) {
                        currentValues = new ArrayList<>(type2envelope.values());
                    }
                    for (Envelope val : currentValues) {
                        if (condition.apply(val)) {
                            targetCollection.add(val);
                        }
                    }
                    TimeUnit.MILLISECONDS.sleep(75);
                }
                return targetCollection;
            }
        });
        return future.get(timeout, timeUnit);
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
                        if (config.isLogPayloadsEnabled()) {
                            logit("raw payload:\n\n"+body+"\n");
                        }
                        String[] lines = body.split("\n");
                        for (String line : lines) {
                            Envelope envelope;
                            try {
                                envelope = JsonHelper.GSON.fromJson(line.trim(), Envelope.class);
                            } catch (JsonSyntaxException jse) {
                                logerr("Could not deserialize to Envelope", jse);
                                throw jse;
                            }
                            if (config.isRetainPayloadsEnabled()) {
                                String baseType = envelope.getData().getBaseType();
                                if (filtersAllowItem(envelope)) {
                                    logit("Adding telemetry item: "+baseType);
                                    telemetryReceived.offer(envelope);
                                    synchronized (multimapLock) {
                                        type2envelope.put(baseType, envelope);
                                    }
                                } else {
                                    logit("Rejected telemetry item by filter: "+baseType);
                                }
                            }
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

    private boolean filtersAllowItem(Envelope item) {
        if (this.filters.isEmpty()) {
            return true;
        }
        for (Predicate<Envelope> filter : this.filters) {
            if (!filter.apply(item)) {
                return false;
            }
        }
        return true;
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

    private class MockedIngestionServletConfig {
        private boolean retainPayloadsEnabled = true;
        private boolean logPayloadsEnabled = true;

        public boolean isRetainPayloadsEnabled() {
            return retainPayloadsEnabled;
        }

        public void setRetainPayloadsEnabled(boolean retainPayloadsEnabled) {
            this.retainPayloadsEnabled = retainPayloadsEnabled;
        }

        public boolean isLogPayloadsEnabled() {
            return logPayloadsEnabled;
        }

        public void setLogPayloadsEnabled(boolean logPayloadsEnabled) {
            this.logPayloadsEnabled = logPayloadsEnabled;
        }
    }
}
