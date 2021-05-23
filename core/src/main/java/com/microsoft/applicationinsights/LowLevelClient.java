package com.microsoft.applicationinsights;

import com.azure.core.http.*;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.util.tracing.Tracer;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.internal.authentication.AadAuthentication;
import reactor.util.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;

class LowLevelClient {

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private final HttpPipeline pipeline;
    private final URL endpoint;

    LowLevelClient(URL endpoint) {
        HttpClient client = HttpClient.createDefault();
        HttpPipelineBuilder pipeline = new HttpPipelineBuilder()
                .httpClient(client);
        // TODO handle authentication exceptions
        HttpPipelinePolicy authenticationPolicy = AadAuthentication.getInstance().getAuthenticationPolicy();
        if (authenticationPolicy != null) {
            pipeline.policies(authenticationPolicy);
        }
        this.pipeline = pipeline.build();
        this.endpoint = endpoint;
    }

    void send(List<TelemetryItem> telemetryItems) {
        try {
            internalSend(telemetryItems);
        } catch (IOException e) {
            // TODO(trask)
            e.printStackTrace();
        }
    }

    void internalSend(List<TelemetryItem> telemetryItems) throws IOException {
        HttpRequest request = new HttpRequest(HttpMethod.POST, endpoint + "v2/track");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (TelemetryItem telemetryItem : telemetryItems) {
            mapper.writeValue(baos, telemetryItem);
            baos.write('\n');
        }
        // FIXME(trask) optimize
        //  * use HttpRequest.setBody(Flux<ByteBuffer>)
        //  * don't create a single large byte array (memory fragmentation)
        //  * reuse ByteBuffers
        request.setBody(baos.toByteArray());
        pipeline.send(request)
                .contextWrite(Context.of(Tracer.DISABLE_TRACING_KEY, true))
                // TODO(trask) handle subscribe with listener
                //  * retry on first failure
                //  * write to disk on second failure
                .subscribe();
    }
}
