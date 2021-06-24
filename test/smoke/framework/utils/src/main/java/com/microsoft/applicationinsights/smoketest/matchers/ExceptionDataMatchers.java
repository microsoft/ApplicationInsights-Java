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

import com.microsoft.applicationinsights.internal.schemav2.ExceptionData;
import com.microsoft.applicationinsights.internal.schemav2.ExceptionDetails;
import com.microsoft.applicationinsights.internal.schemav2.SeverityLevel;
import java.util.List;
import java.util.Map;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public class ExceptionDataMatchers {

  public static Matcher<ExceptionData> hasSeverityLevel(SeverityLevel level) {
    return new FeatureMatcher<ExceptionData, SeverityLevel>(
        equalTo(level), "seveirity level to be", "severity level") {

      @Override
      protected SeverityLevel featureValueOf(ExceptionData actual) {
        return actual.getSeverityLevel();
      }
    };
  }

  public static Matcher<ExceptionData> hasProperty(String key, String value) {
    return new FeatureMatcher<ExceptionData, Map<String, String>>(
        hasEntry(key, value), "ExceptionData with property", "") {

      @Override
      protected Map<String, String> featureValueOf(ExceptionData actual) {
        return actual.getProperties();
      }
    };
  }

  public static Matcher<ExceptionData> hasMeasurement(String key, Double value) {
    return new FeatureMatcher<ExceptionData, Map<String, Double>>(
        hasEntry(key, value), "ExceptionData with measurement", "") {

      @Override
      protected Map<String, Double> featureValueOf(ExceptionData actual) {
        return actual.getMeasurements();
      }
    };
  }

  public static Matcher<ExceptionData> hasException(Matcher<ExceptionDetails> exception) {
    return new FeatureMatcher<ExceptionData, List<ExceptionDetails>>(
        hasItem(exception), "ExceptionData with exception", "") {

      @Override
      protected List<ExceptionDetails> featureValueOf(ExceptionData actual) {
        return actual.getExceptions();
      }
    };
  }

  public static class ExceptionDetailsMatchers {
    public static Matcher<ExceptionDetails> withMessage(String message) {
      return new FeatureMatcher<ExceptionDetails, String>(
          equalTo(message), "ExceptionDetails with message", "message") {

        @Override
        protected String featureValueOf(ExceptionDetails actual) {
          return actual.getMessage();
        }
      };
    }

    private ExceptionDetailsMatchers() {}
  }

  private ExceptionDataMatchers() {}
}
