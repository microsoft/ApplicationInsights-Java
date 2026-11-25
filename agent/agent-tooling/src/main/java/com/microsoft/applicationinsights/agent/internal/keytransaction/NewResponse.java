// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.keytransaction;

import com.azure.json.JsonReader;
import com.azure.json.JsonToken;
import java.io.IOException;
import java.util.List;

class NewResponse {

  private List<SdkConfiguration> sdkConfigurations;

  public List<SdkConfiguration> getSdkConfigurations() {
    return sdkConfigurations;
  }

  static NewResponse fromJson(JsonReader jsonReader) throws IOException {
    return jsonReader.readObject(
        (reader) -> {
          NewResponse deserializedValue = new NewResponse();

          while (reader.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = reader.getFieldName();
            reader.nextToken();
            if ("sdkConfiguration".equals(fieldName)) {
              deserializedValue.sdkConfigurations = reader.readArray(SdkConfiguration::fromJson);
            } else {
              reader.skipChildren();
            }
          }

          return deserializedValue;
        });
  }
}
