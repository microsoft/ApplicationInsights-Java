/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v2_0;

import io.opentelemetry.instrumentation.api.instrumenter.AppIdResponseHeaderExtractor;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;

final class ApacheHttpClientAppIdAttributeExtractor
    extends AppIdResponseHeaderExtractor<HttpMethod, HttpMethod> {

  @Override
  protected String header(HttpMethod response, String name) {
    Header header = response.getResponseHeader(name);
    return header == null ? null : header.getValue();
  }
}
