// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.serviceprofilerapi.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.microsoft.applicationinsights.serviceprofilerapi.ProfilerConfiguration;
import com.microsoft.applicationinsights.serviceprofilerapi.client.ServiceProfilerClient;
import java.io.IOException;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class ConfigServiceTest {

  @Test
  void badServiceResponseDoesNotProvideReturn() throws IOException, URISyntaxException {
    ServiceProfilerClient serviceProfilerClient = Mockito.mock(ServiceProfilerClient.class);

    Mockito.when(serviceProfilerClient.getSettings(Mockito.any())).thenReturn(Mono.just(""));

    ConfigService settingsClient = new ConfigService(serviceProfilerClient);
    Mono<ProfilerConfiguration> result = settingsClient.pullSettings();

    assertThatThrownBy(result::block).isInstanceOf(Exception.class);
  }
}
