// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.config;

import com.microsoft.applicationinsights.agent.internal.profiler.service.ServiceProfilerClient;
import java.util.Date;
import reactor.core.publisher.Mono;

/** Client that pulls setting from the service profiler endpoint and emits them if changed. */
class ConfigService {

  private final ServiceProfilerClient serviceProfilerClient;

  private volatile Date lastModified;

  ConfigService(ServiceProfilerClient serviceProfilerClient) {
    this.serviceProfilerClient = serviceProfilerClient;
    lastModified = new Date(0); // January 1, 1970, 00:00:00 GMT
  }

  /** Pulls the latest settings. If they have not been modified empty is returned. */
  Mono<ProfilerConfiguration> pullSettings() {
    return serviceProfilerClient
        .getSettings(lastModified)
        .flatMap(
            config -> {
              if (config != null && config.getLastModified().getTime() != lastModified.getTime()) {
                lastModified = config.getLastModified();
                return Mono.just(config);
              }
              return Mono.empty();
            });
  }
}
