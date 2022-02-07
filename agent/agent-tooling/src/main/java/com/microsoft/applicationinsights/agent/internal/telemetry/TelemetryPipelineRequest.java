package com.microsoft.applicationinsights.agent.internal.telemetry;

import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpRequest;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;
import reactor.core.publisher.Flux;

public class TelemetryPipelineRequest {

  private volatile URL url;
  private final String instrumentationKey;
  private final List<ByteBuffer> telemetry;
  private final int contentLength;

  TelemetryPipelineRequest(URL url, String instrumentationKey, List<ByteBuffer> telemetry) {
    this.url = url;
    this.instrumentationKey = instrumentationKey;
    this.telemetry = telemetry;
    contentLength = telemetry.stream().mapToInt(ByteBuffer::limit).sum();
  }

  public URL getUrl() {
    return url;
  }

  void setUrl(URL url) {
    this.url = url;
  }

  public String getInstrumentationKey() {
    return instrumentationKey;
  }

  public List<ByteBuffer> getTelemetry() {
    return telemetry;
  }

  HttpRequest createHttpRequest() {
    HttpRequest request = new HttpRequest(HttpMethod.POST, url);
    request.setBody(Flux.fromIterable(telemetry));
    request.setHeader("Content-Length", Integer.toString(contentLength));

    // need to suppress the default User-Agent "ReactorNetty/dev", otherwise Breeze ingestionservice
    // will put that User-Agent header into the client_Browser field for all telemetry that doesn't
    // explicitly set it's own UserAgent (ideally Breeze would only have this behavior for ingestion
    // directly from browsers)
    // TODO (trask) not setting User-Agent header at all would be a better option, but haven't
    //  figured out how to do that yet
    request.setHeader("User-Agent", "");
    request.setHeader("Content-Encoding", "gzip");

    return request;
  }
}
