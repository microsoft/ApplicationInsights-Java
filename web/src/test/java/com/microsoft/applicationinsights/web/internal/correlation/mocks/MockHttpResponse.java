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

package com.microsoft.applicationinsights.web.internal.correlation.mocks;

import java.util.Locale;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.params.HttpParams;

public class MockHttpResponse implements HttpResponse {

  private HttpEntity entity;
  private MockStatusLine statusLine;

  public MockHttpResponse(HttpEntity entity, int statusCode) {
    this.entity = entity;
    this.statusLine = new MockStatusLine(new ProtocolVersion("http", 1, 1), statusCode, "reason");
  }

  @Override
  public ProtocolVersion getProtocolVersion() {
    return null;
  }

  @Override
  public boolean containsHeader(String name) {
    return false;
  }

  @Override
  public Header[] getHeaders(String name) {
    return null;
  }

  @Override
  public Header getFirstHeader(String name) {
    return null;
  }

  @Override
  public Header getLastHeader(String name) {
    return null;
  }

  @Override
  public Header[] getAllHeaders() {
    return null;
  }

  @Override
  public void addHeader(Header header) {}

  @Override
  public void addHeader(String name, String value) {}

  @Override
  public void setHeader(Header header) {}

  @Override
  public void setHeader(String name, String value) {}

  @Override
  public void setHeaders(Header[] headers) {}

  @Override
  public void removeHeader(Header header) {}

  @Override
  public void removeHeaders(String name) {}

  @Override
  public HeaderIterator headerIterator() {
    return null;
  }

  @Override
  public HeaderIterator headerIterator(String name) {
    return null;
  }

  @Override
  public HttpParams getParams() {
    return null;
  }

  @Override
  public void setParams(HttpParams params) {}

  @Override
  public StatusLine getStatusLine() {
    return this.statusLine;
  }

  @Override
  public void setStatusLine(StatusLine statusline) {}

  @Override
  public void setStatusLine(ProtocolVersion ver, int code) {}

  @Override
  public void setStatusLine(ProtocolVersion ver, int code, String reason) {}

  @Override
  public void setStatusCode(int code) throws IllegalStateException {
    this.statusLine.setStatusCode(code);
  }

  @Override
  public void setReasonPhrase(String reason) throws IllegalStateException {}

  @Override
  public HttpEntity getEntity() {
    return this.entity;
  }

  @Override
  public void setEntity(HttpEntity entity) {
    this.entity = entity;
  }

  @Override
  public Locale getLocale() {
    return null;
  }

  @Override
  public void setLocale(Locale loc) {}
}
