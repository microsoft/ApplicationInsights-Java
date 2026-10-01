// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.keytransaction;

import com.azure.json.JsonReader;
import com.azure.json.JsonToken;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import java.io.IOException;
import java.util.List;

public class KeyTransactionConfig {

  private String name;
  private List<Criterion> startCriteria;
  private List<Criterion> endCriteria;

  String getName() {
    return name;
  }

  List<Criterion> getStartCriteria() {
    return startCriteria;
  }

  List<Criterion> getEndCriteria() {
    return endCriteria;
  }

  public static KeyTransactionConfig fromJson(JsonReader jsonReader) throws IOException {
    return jsonReader.readObject(
        (reader) -> {
          KeyTransactionConfig deserializedValue = new KeyTransactionConfig();

          while (reader.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = reader.getFieldName();
            reader.nextToken();
            if ("Name".equals(fieldName)) {
              deserializedValue.name = reader.getString();
            } else if ("StartCriteria".equals(fieldName)) {
              deserializedValue.startCriteria = reader.readArray(Criterion::fromJson);
            } else if ("EndCriteria".equals(fieldName)) {
              deserializedValue.endCriteria = reader.readArray(Criterion::fromJson);
            } else {
              reader.skipChildren();
            }
          }

          return deserializedValue;
        });
  }

  public static boolean matches(Attributes attributes, List<Criterion> criteria) {
    for (Criterion criterion : criteria) {
      String value = attributes.get(criterion.field);
      switch (criterion.operator) {
        case EQUALS:
          if (value == null || !value.equals(criterion.value)) {
            return false;
          }
          break;
        case STARTSWITH:
          if (value == null || !value.startsWith(criterion.value)) {
            return false;
          }
          break;
        case CONTAINS:
          if (value == null || !value.contains(criterion.value)) {
            return false;
          }
          break;
        default:
          // unexpected operator
          return false;
      }
    }
    return true;
  }

  // TODO (not for hackathon) expand this to work with non-String attributes
  // a bit tricky since Attributes#get(AttributeKey) requires a known type
  // (without iterating over all attributes)
  public static class Criterion {
    private AttributeKey<String> field;
    private String value;
    private Operator operator;

    // visible for testing
    AttributeKey<String> getField() {
      return field;
    }

    // visible for testing
    String getValue() {
      return value;
    }

    // visible for testing
    Operator getOperator() {
      return operator;
    }

    public static Criterion fromJson(JsonReader jsonReader) throws IOException {
      return jsonReader.readObject(
          (reader) -> {
            Criterion deserializedValue = new Criterion();

            while (reader.nextToken() != JsonToken.END_OBJECT) {
              String fieldName = reader.getFieldName();
              reader.nextToken();
              if ("Field".equals(fieldName)) {
                deserializedValue.field = AttributeKey.stringKey(reader.getString());
              } else if ("Operator".equals(fieldName)) {
                deserializedValue.operator = Operator.from(reader.getString());
              } else if ("Value".equals(fieldName)) {
                deserializedValue.value = reader.getString();
              } else {
                reader.skipChildren();
              }
            }

            return deserializedValue;
          });
    }
  }

  public enum Operator {
    EQUALS,
    STARTSWITH,
    CONTAINS;

    private static Operator from(String value) {
      switch (value) {
        case "==":
          return EQUALS;
        case "startswith":
          return STARTSWITH;
        case "contains":
          return CONTAINS;
        default:
          throw new IllegalStateException("Unexpected operator: " + value);
      }
    }
  }
}
