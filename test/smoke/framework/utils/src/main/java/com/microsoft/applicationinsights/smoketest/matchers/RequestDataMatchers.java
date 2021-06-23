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

package com.microsoft.applicationinsights.smoketest.matchers;

import static org.hamcrest.Matchers.*;

import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.telemetry.Duration;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public class RequestDataMatchers {
  public static Matcher<RequestData> hasName(String name) {
    return new FeatureMatcher<RequestData, String>(equalTo(name), "RequestData with name", "name") {
      @Override
      protected String featureValueOf(RequestData actual) {
        return actual.getName();
      }
    };
  }

  public static Matcher<RequestData> hasDuration(Duration duration) {
    return new FeatureMatcher<RequestData, Duration>(
        equalTo(duration), "RequestData with duration", "duration") {
      @Override
      protected Duration featureValueOf(RequestData actual) {
        return actual.getDuration();
      }
    };
  }

  public static Matcher<RequestData> hasSuccess(boolean success) {
    return new FeatureMatcher<RequestData, Boolean>(
        equalTo(success), "RequestData with success", "success") {
      @Override
      protected Boolean featureValueOf(RequestData actual) {
        return actual.getSuccess();
      }
    };
  }

  public static Matcher<RequestData> hasResponseCode(String responseCode) {
    return new FeatureMatcher<RequestData, String>(
        equalTo(responseCode), "RequestData with response code", "response code") {
      @Override
      protected String featureValueOf(RequestData actual) {
        return actual.getResponseCode();
      }
    };
  }

  public static Matcher<RequestData> hasUrl(String url) {
    return new FeatureMatcher<RequestData, String>(equalTo(url), "ReqeustData with url", "url") {
      @Override
      protected String featureValueOf(RequestData actual) {
        return actual.getUrl();
      }
    };
  }

  private RequestDataMatchers() {}
}
