// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.monitor.opentelemetry.exporter.implementation;

import static java.util.Arrays.asList;

import com.azure.monitor.opentelemetry.exporter.implementation.builders.AbstractTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.Trie;
import io.opentelemetry.api.common.AttributeKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class MappingsBuilder {

  // TODO need to keep this list in sync as new semantic conventions are defined
  private static final Set<String> STANDARD_ATTRIBUTE_PREFIXES =
      new HashSet<>(
          asList(
              "http.",

              // specifically listing out all standard db.* attributes
              // so that db.cosmosdb.* attributes will be preserved
              SemanticAttributes.DB_SYSTEM.getKey(),
              SemanticAttributes.DB_NAME.getKey(),
              SemanticAttributes.DB_STATEMENT.getKey(),
              SemanticAttributes.DB_OPERATION.getKey(),
              SemanticAttributes.DB_CONNECTION_STRING.getKey(),
              SemanticAttributes.DB_USER.getKey(),
              SemanticAttributes.DB_SQL_TABLE.getKey(),
              "message.",
              "messaging.",
              "rpc.",
              "enduser.",
              "net.",
              "peer.",
              "exception.",
              "thread.",
              "faas.",
              "code.",
              "job.", // proposed semantic convention which we use for job,
              "applicationinsights.internal."));

  private final Map<String, ExactMapping> exactMappings = new HashMap<>();
  private final Trie.Builder<PrefixMapping> prefixMappings = Trie.newBuilder();

  MappingsBuilder() {
    // ignore all standard attribute prefixes
    for (String prefix : STANDARD_ATTRIBUTE_PREFIXES) {
      prefixMappings.put(prefix, (telemetryBuilder, key, value) -> {});
    }
  }

  MappingsBuilder ignoreExact(String key) {
    exactMappings.put(key, (telemetryBuilder, value) -> {});
    return this;
  }

  MappingsBuilder ignorePrefix(String prefix) {
    prefixMappings.put(prefix, (telemetryBuilder, key, value) -> {});
    return this;
  }

  MappingsBuilder exact(String key, ExactMapping mapping) {
    exactMappings.put(key, mapping);
    return this;
  }

  MappingsBuilder prefix(String prefix, PrefixMapping mapping) {
    prefixMappings.put(prefix, mapping);
    return this;
  }

  MappingsBuilder exactString(AttributeKey<String> attributeKey, String propertyName) {
    exactMappings.put(
        attributeKey.getKey(),
        (telemetryBuilder, value) -> {
          if (value instanceof String) {
            telemetryBuilder.addProperty(propertyName, (String) value);
          }
        });
    return this;
  }

  MappingsBuilder exactLong(AttributeKey<Long> attributeKey, String propertyName) {
    exactMappings.put(
        attributeKey.getKey(),
        (telemetryBuilder, value) -> {
          if (value instanceof Long) {
            telemetryBuilder.addProperty(propertyName, Long.toString((Long) value));
          }
        });
    return this;
  }

  MappingsBuilder exactStringArray(AttributeKey<List<String>> attributeKey, String propertyName) {
    exactMappings.put(
        attributeKey.getKey(),
        (telemetryBuilder, value) -> {
          if (value instanceof List) {
            telemetryBuilder.addProperty(propertyName, String.join(",", (List) value));
          }
        });
    return this;
  }

  public Mappings build() {
    return new Mappings(exactMappings, prefixMappings.build());
  }

  @FunctionalInterface
  interface ExactMapping {
    void map(AbstractTelemetryBuilder telemetryBuilder, Object value);
  }

  @FunctionalInterface
  interface PrefixMapping {
    void map(AbstractTelemetryBuilder telemetryBuilder, String key, Object value);
  }
}
