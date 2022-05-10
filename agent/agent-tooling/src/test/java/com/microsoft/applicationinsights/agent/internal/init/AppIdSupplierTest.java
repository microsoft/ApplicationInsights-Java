/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.agent.internal.init;

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
