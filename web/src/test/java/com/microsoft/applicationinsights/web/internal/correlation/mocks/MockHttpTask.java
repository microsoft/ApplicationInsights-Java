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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.http.HttpResponse;

public class MockHttpTask implements Future<HttpResponse> {

  private HttpResponse response;
  private boolean failureOn;
  private boolean isDone;

  public MockHttpTask(HttpResponse response) {
    this.response = response;
    this.failureOn = false;
    this.isDone = false;
  }

  public void setFailureOn(boolean fail) {
    this.failureOn = fail;
  }

  public void setIsDone(boolean isDone) {
    this.isDone = isDone;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public boolean isDone() {
    return this.isDone;
  }

  @Override
  public HttpResponse get() throws InterruptedException, ExecutionException {
    return doGet();
  }

  @Override
  public HttpResponse get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return doGet();
  }

  private HttpResponse doGet() throws ExecutionException {
    if (this.failureOn) {
      throw new ExecutionException("Failure", null);
    }

    return this.response;
  }
}
