// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.jfr;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;
import java.io.IOException;
import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@SuppressWarnings({
  "Java8ApiChecker"
}) // JFR APIs require Java 11+, but agent targets Java 8 bytecode
@Name("com.microsoft.applicationinsights.diagnostics.jfr.AlertBreach")
@Label("AlertBreach")
@Category("Diagnostic")
@Description("AlertBreach")
@StackTrace(false)
public class AlertBreachJfrEvent extends Event implements JsonSerializable<AlertBreachJfrEvent> {

  public static final String NAME = "com.microsoft.applicationinsights.diagnostics.jfr.AlertBreach";

  private String alertBreach;

  public AlertBreachJfrEvent() {}

  public String getAlertBreach() {
    return alertBreach;
  }

  public AlertBreachJfrEvent setAlertBreach(String alertBreach) {
    this.alertBreach = alertBreach;
    return this;
  }

  /** Serialize a AlertBreachJfrEvent to a JSON writer */
  @Override
  public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
    jsonWriter.writeStartObject();
    jsonWriter.writeStringField("alertBreach", alertBreach);
    jsonWriter.writeEndObject();
    return jsonWriter;
  }

  /** Deserialize a AlertBreachJfrEvent from a JSON reader */
  public static AlertBreachJfrEvent fromJson(JsonReader jsonReader) throws IOException {
    return jsonReader.readObject(
        reader -> {
          AlertBreachJfrEvent event = new AlertBreachJfrEvent();
          while (reader.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = reader.getFieldName();
            reader.nextToken();

            if ("alertBreach".equals(fieldName)) {
              event.setAlertBreach(reader.getString());
            }
          }
          return event;
        });
  }
}
