// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.serviceprofilerapi.config;

import com.azure.core.exception.HttpResponseException;
import com.microsoft.applicationinsights.profiler.ProfilerConfiguration;
import com.microsoft.applicationinsights.serviceprofilerapi.client.ServiceProfilerClient;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import reactor.core.publisher.Mono;

/** Client that pulls setting from the service profiler endpoint and emits them if changed. */
public class ServiceProfilerSettingsClient {
  private final ServiceProfilerClient serviceProfilerClient;
  private Date lastModified;

  public ServiceProfilerSettingsClient(ServiceProfilerClient serviceProfilerClient) {
    this.serviceProfilerClient = serviceProfilerClient;
    lastModified = new Date(70, Calendar.JANUARY, 1);
  }

  /** Pulls the latest settings. If they have not been modified empty is returned. */
  public Mono<ProfilerConfiguration> pullSettings() {
    return serviceProfilerClient
        .getSettings(lastModified)
        .flatMap(
            config -> {
              try {
                ProfilerConfiguration serviceProfilerConfiguration =
                    toServiceProfilerConfiguration(config);
                if (serviceProfilerConfiguration != null
                    && serviceProfilerConfiguration.getLastModified().getTime()
                        != lastModified.getTime()) {
                  lastModified = serviceProfilerConfiguration.getLastModified();
                  return Mono.just(serviceProfilerConfiguration);
                }
                return Mono.empty();
              } catch (HttpResponseException e) {
                if (e.getResponse().getStatusCode() == 304) {
                  return Mono.empty();
                } else {
                  return Mono.error(e);
                }
              } catch (Exception e) {
                return Mono.error(e);
              }
            });
  }

  private static ProfilerConfiguration toServiceProfilerConfiguration(String config)
      throws IOException {
    Moshi moshi = new Moshi.Builder().add(Date.class, new Rfc3339DateJsonAdapter()).build();
    JsonAdapter<ProfilerConfiguration> jsonAdapter = moshi.adapter(ProfilerConfiguration.class);

    return jsonAdapter.fromJson(config);
  }
}
