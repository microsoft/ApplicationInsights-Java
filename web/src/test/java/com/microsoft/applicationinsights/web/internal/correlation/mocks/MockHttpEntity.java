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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;

public class MockHttpEntity implements HttpEntity {

  private String content;

  @Override
  public boolean isRepeatable() {
    return false;
  }

  @Override
  public boolean isChunked() {
    return false;
  }

  @Override
  public long getContentLength() {
    return 0;
  }

  @Override
  public Header getContentType() {
    return null;
  }

  @Override
  public Header getContentEncoding() {
    return null;
  }

  @Override
  public InputStream getContent() throws IOException, UnsupportedOperationException {
    return new ByteArrayInputStream(this.content.getBytes());
  }

  public void setContent(String content) {
    this.content = content;
  }

  @Override
  public void writeTo(OutputStream outstream) throws IOException {}

  @Override
  public boolean isStreaming() {
    return false;
  }

  @Override
  public void consumeContent() throws IOException {}
}
