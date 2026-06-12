// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.keytransaction;

import com.azure.json.JsonReader;
import com.azure.json.JsonToken;
import java.io.IOException;

public class SdkConfiguration {

  private String key;
  private KeyTransactionConfig value; // for hackathon only supporting one type of value

  public String getKey() {
    return key;
  }

  public KeyTransactionConfig getValue() {
    return value;
  }

  static SdkConfiguration fromJson(JsonReader jsonReader) throws IOException {
    return jsonReader.readObject(
        (reader) -> {
          SdkConfiguration deserializedValue = new SdkConfiguration();

          while (reader.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = reader.getFieldName();
            reader.nextToken();
            if ("Key".equals(fieldName)) {
              deserializedValue.key = reader.getString();
            } else if ("Value".equals(fieldName)) {
              deserializedValue.value = KeyTransactionConfig.fromJson(reader);
            } else {
              reader.skipChildren();
            }
          }

          return deserializedValue;
        });
  }
}
