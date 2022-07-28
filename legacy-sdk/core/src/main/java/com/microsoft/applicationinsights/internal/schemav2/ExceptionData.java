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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ExceptionData {

  private List<ExceptionDetails> exceptions;
  private SeverityLevel severityLevel;
  private String problemId;

  private ConcurrentMap<String, String> properties;
  private ConcurrentMap<String, Double> measurements;

  public ExceptionData() {}

  public List<ExceptionDetails> getExceptions() {
    if (this.exceptions == null) {
      this.exceptions = new ArrayList<>();
    }
    return this.exceptions;
  }

  public void setExceptions(List<ExceptionDetails> value) {
    this.exceptions = value;
  }

  public SeverityLevel getSeverityLevel() {
    return this.severityLevel;
  }

  public void setSeverityLevel(SeverityLevel value) {
    this.severityLevel = value;
  }

  public String getProblemId() {
    return this.problemId;
  }

  public void setProblemId(String value) {
    this.problemId = value;
  }

  public ConcurrentMap<String, String> getProperties() {
    if (this.properties == null) {
      this.properties = new ConcurrentHashMap<>();
    }
    return this.properties;
  }

  public void setProperties(ConcurrentMap<String, String> value) {
    this.properties = value;
  }

  public ConcurrentMap<String, Double> getMeasurements() {
    if (this.measurements == null) {
      this.measurements = new ConcurrentHashMap<>();
    }
    return this.measurements;
  }

  public void setMeasurements(ConcurrentMap<String, Double> value) {
    this.measurements = value;
  }
}
