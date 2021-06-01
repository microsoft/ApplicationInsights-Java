/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v4_0;

import io.opentelemetry.instrumentation.api.instrumenter.AppIdResponseHeaderExtractor;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

final class ApacheHttpClientAppIdAttributeExtractor
    extends AppIdResponseHeaderExtractor<HttpUriRequest, HttpResponse> {

  @Override
  protected String header(HttpResponse response, String name) {
    Header header = response.getFirstHeader(name);
    return header == null ? null : header.getValue();
  }
}
