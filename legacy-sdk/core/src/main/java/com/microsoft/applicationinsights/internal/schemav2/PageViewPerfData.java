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

package com.microsoft.applicationinsights.internal.schemav2;

import com.microsoft.applicationinsights.telemetry.Duration;

public class PageViewPerfData extends PageViewData {

  private Duration perfTotal = new Duration(0);
  private Duration networkConnect = new Duration(0);
  private Duration sentRequest = new Duration(0);
  private Duration receivedResponse = new Duration(0);
  private Duration domProcessing = new Duration(0);

  public PageViewPerfData() {}

  public Duration getPerfTotal() {
    return perfTotal;
  }

  public void setPerfTotal(Duration perfTotal) {
    this.perfTotal = perfTotal;
  }

  public Duration getNetworkConnect() {
    return networkConnect;
  }

  public void setNetworkConnect(Duration networkConnect) {
    this.networkConnect = networkConnect;
  }

  public Duration getSentRequest() {
    return sentRequest;
  }

  public void setSentRequest(Duration sentRequest) {
    this.sentRequest = sentRequest;
  }

  public Duration getReceivedResponse() {
    return receivedResponse;
  }

  public void setReceivedResponse(Duration receivedResponse) {
    this.receivedResponse = receivedResponse;
  }

  public Duration getDomProcessing() {
    return domProcessing;
  }

  public void setDomProcessing(Duration domProcessing) {
    this.domProcessing = domProcessing;
  }
}
