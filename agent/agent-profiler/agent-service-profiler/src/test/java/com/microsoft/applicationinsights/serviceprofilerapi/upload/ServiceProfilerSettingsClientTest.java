// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.serviceprofilerapi.upload;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.microsoft.applicationinsights.profiler.ProfilerConfiguration;
import com.microsoft.applicationinsights.serviceprofilerapi.client.ProfilerFrontendClientV2;
import com.microsoft.applicationinsights.serviceprofilerapi.config.ServiceProfilerSettingsClient;
import java.io.IOException;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class ServiceProfilerSettingsClientTest {

  @Test
  void badServiceResponseDoesNotProvideReturn() throws IOException, URISyntaxException {
    ProfilerFrontendClientV2 serviceProfilerClient = Mockito.mock(ProfilerFrontendClientV2.class);

    Mockito.when(serviceProfilerClient.getSettings(Mockito.any())).thenReturn(Mono.just(""));

    ServiceProfilerSettingsClient settingsClient =
        new ServiceProfilerSettingsClient(serviceProfilerClient);
    Mono<ProfilerConfiguration> result = settingsClient.pullSettings();

    assertThatThrownBy(result::block).isInstanceOf(Exception.class);
  }
}
