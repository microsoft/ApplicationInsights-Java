// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.config;

import com.microsoft.applicationinsights.agent.internal.profiler.service.ServiceProfilerClient;
import java.util.Date;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/** Client that pulls setting from the service profiler endpoint and emits them if changed. */
class ConfigService {

  private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);

  private final ServiceProfilerClient serviceProfilerClient;

  private volatile Date lastModified;

  ConfigService(ServiceProfilerClient serviceProfilerClient) {
    this.serviceProfilerClient = serviceProfilerClient;
    lastModified = new Date(0); // January 1, 1970, 00:00:00 GMT
  }

  void pollForConfigUpdates(Consumer<ProfilerConfiguration> updateListener) {
    try {
      pullSettings()
          .doOnError(e -> logger.error("Error pulling service profiler settings", e))
          .subscribe(config -> updateListener.accept(config));
    } catch (Throwable t) {
      logger.error("Error pulling service profiler settings", t);
    }
  }

  /** Pulls the latest settings. If they have not been modified empty is returned. */
  // visible for testing
  Mono<ProfilerConfiguration> pullSettings() {
    return serviceProfilerClient
        .getSettings(lastModified)
        .flatMap(
            config -> {
              if (config != null && config.getLastModified().compareTo(lastModified) != 0) {
                lastModified = config.getLastModified();
                return Mono.just(config);
              }
              return Mono.empty();
            });
  }
}
