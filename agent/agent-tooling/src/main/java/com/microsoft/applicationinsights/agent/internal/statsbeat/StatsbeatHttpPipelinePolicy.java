package com.microsoft.applicationinsights.agent.internal.statsbeat;

import com.azure.core.http.HttpPipelineCallContext;
import com.azure.core.http.HttpPipelineNextPolicy;
import com.azure.core.http.HttpResponse;
import com.azure.core.http.policy.HttpPipelinePolicy;
import java.util.concurrent.atomic.AtomicLong;
import reactor.core.publisher.Mono;

public class StatsbeatHttpPipelinePolicy implements HttpPipelinePolicy {

  private static final String INSTRUMENTATION_KEY_DATA = "instrumentationKey";

  private final NetworkStatsbeat networkStatsbeat;

  public StatsbeatHttpPipelinePolicy(NetworkStatsbeat networkStatsbeat) {
    this.networkStatsbeat = networkStatsbeat;
  }

  @Override
  public Mono<HttpResponse> process(HttpPipelineCallContext context, HttpPipelineNextPolicy next) {
    // using AtomicLong for both mutable holder and volatile (but atomicity is not needed here)
    AtomicLong startTime = new AtomicLong();
    // TODO (trask) use host here to break sharing of ikey redirect cache!
    String host = context.getHttpRequest().getUrl().getHost();
    String instrumentationKey =
        context.getData(INSTRUMENTATION_KEY_DATA).orElse("unknown").toString();
    return next.process()
        .doOnSubscribe(subscription -> startTime.set(System.currentTimeMillis()))
        .doOnSuccess(
            response -> {
              int statusCode = response.getStatusCode();
              if (statusCode == 200) {
                networkStatsbeat.incrementRequestSuccessCount(
                    System.currentTimeMillis() - startTime.get(), instrumentationKey);
              } else if (statusCode == 301
                  || statusCode == 302
                  || statusCode == 307
                  || statusCode == 308) {
                // these are not tracked as success or failure since they will be retried
              } else if (statusCode == 439) {
                networkStatsbeat.incrementThrottlingCount(instrumentationKey);
              } else {
                // note: 401 and 403 are currently tracked as failures
                networkStatsbeat.incrementRequestFailureCount(instrumentationKey);
              }
            })
        .doOnError(
            throwable -> {
              networkStatsbeat.incrementRequestFailureCount(instrumentationKey);
            });
  }
}
