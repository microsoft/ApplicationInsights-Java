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

package com.microsoft.applicationinsights.test.framework.telemetries;

import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import java.net.URISyntaxException;
import java.util.Properties;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Created by moralt on 05/05/2015. */
public abstract class TelemetryItem extends Properties {
  private DocumentType docType;

  /**
   * Initializes a new TelemetryItem object
   *
   * @param docType The document type of the telemetry item
   */
  public TelemetryItem(DocumentType docType) {
    this.docType = docType;
  }

  public TelemetryItem(DocumentType docType, JSONObject jsonObject)
      throws URISyntaxException, JSONException {
    this(docType);

    initTelemetryItemWithCommonProperties(jsonObject);
  }

  protected abstract String[] getDefaultPropertiesToCompare();

  public DocumentType getDocType() {
    return this.docType;
  }

  /**
   * Tests if the properties of the this item equals to the properties of another telemetry item
   *
   * @param obj The other object
   * @return True if equals, otherwise false.
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null || !(obj instanceof TelemetryItem)) {
      return false;
    }

    TelemetryItem telemetry = (TelemetryItem) obj;

    if (telemetry.getDocType() != this.getDocType()) {
      return false;
    }

    for (String propertyName : getDefaultPropertiesToCompare()) {
      if (telemetry.getProperty(propertyName) == null && this.getProperty(propertyName) == null) {
        continue;
      }

      if (telemetry.getProperty(propertyName) == null
          || this.getProperty(propertyName) == null
          || !telemetry
              .getProperty(propertyName)
              .equalsIgnoreCase(this.getProperty(propertyName))) {
        System.out.println(
            "Mismatch for property name '"
                + propertyName
                + "': '"
                + telemetry.getProperty(propertyName)
                + "' '"
                + getProperty(propertyName)
                + "'.");
        return false;
      }
    }

    return true;
  }

  /**
   * Returns the Hashcode of the ID of this object
   *
   * @return The Hashcode of the ID of this object
   */
  @Override
  public int hashCode() {
    int hash = 0;
    for (String propertyName : getDefaultPropertiesToCompare()) {
      String property = getProperty(propertyName);

      if (!LocalStringsUtils.isNullOrEmpty(property)) {
        hash ^= property.hashCode();
      }
    }

    return hash;
  }

  private void initTelemetryItemWithCommonProperties(JSONObject json)
      throws URISyntaxException, JSONException {
    System.out.println("Extracting JSON common properties (" + this.docType + ")");
    JSONObject context = json.getJSONObject("context");

    JSONObject sessionJson = context.getJSONObject("session");
    String sessionId = !sessionJson.isNull("id") ? sessionJson.getString("id") : "";

    JSONObject userJson = context.getJSONObject("user");
    String userId = !userJson.isNull("anonId") ? userJson.getString("anonId") : "";

    String operationId = context.getJSONObject("operation").getString("id");
    String operationName = context.getJSONObject("operation").getString("name");

    JSONObject custom = context.getJSONObject("custom");
    JSONArray dimensions = custom.getJSONArray("dimensions");

    String runId = null;
    for (int i = 0; i < dimensions.length(); i++) {
      JSONObject jsonObject = dimensions.getJSONObject(i);
      if (!jsonObject.isNull("runid")) {
        runId = jsonObject.getString("runid");
        break;
      }
    }

    this.setProperty("userId", userId);
    this.setProperty("sessionId", sessionId);
    this.setProperty("runId", runId);
    this.setProperty("operationId", operationId);
    this.setProperty("operationName", operationName);
  }
}
