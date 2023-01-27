// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.monitor.opentelemetry.exporter.implementation.configuration.ConnectionString;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.jupiter.api.Test;

public class AppIdSupplierTest {

  @Test
  public void test() {
    // AppIdSupplier.getAppIdUrl();
  }

  @Test // this test does not use this.config
  void appIdUrlIsConstructedWithIkeyFromIngestionEndpoint() throws MalformedURLException {
    ConnectionString cs =
        ConnectionString.parse("InstrumentationKey=fake-ikey;IngestionEndpoint=http://123.com");
    assertThat(AppIdSupplier.getAppIdUrl(cs))
        .isEqualTo(new URL("http://123.com/api/profiles/fake-ikey/appId"));
  }

  @Test
  void appIdUrlWithPathKeepsIt() throws MalformedURLException {
    ConnectionString cs =
        ConnectionString.parse(
            "InstrumentationKey=fake-ikey;IngestionEndpoint=http://123.com/path/321");
    assertThat(AppIdSupplier.getAppIdUrl(cs))
        .isEqualTo(new URL("http://123.com/path/321/api/profiles/fake-ikey/appId"));

    cs =
        ConnectionString.parse(
            "InstrumentationKey=fake-ikey;IngestionEndpoint=http://123.com/path/321/");
    assertThat(AppIdSupplier.getAppIdUrl(cs))
        .isEqualTo(new URL("http://123.com/path/321/api/profiles/fake-ikey/appId"));
  }
}
