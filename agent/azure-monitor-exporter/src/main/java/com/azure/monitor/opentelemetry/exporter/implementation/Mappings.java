// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.monitor.opentelemetry.exporter.implementation;

import com.azure.monitor.opentelemetry.exporter.implementation.builders.AbstractTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.Trie;
import io.opentelemetry.api.common.AttributeKey;
import java.util.Map;

class Mappings {

  private final Map<String, MappingsBuilder.ExactMapping> exactMappings;
  private final Trie<MappingsBuilder.PrefixMapping> prefixMappings;
  private final MappingsBuilder.DefaultMapping defaultMapping;

  Mappings(
      Map<String, MappingsBuilder.ExactMapping> exactMappings,
      Trie<MappingsBuilder.PrefixMapping> prefixMappings,
      MappingsBuilder.DefaultMapping defaultMapping) {
    this.exactMappings = exactMappings;
    this.prefixMappings = prefixMappings;
    this.defaultMapping = defaultMapping;
  }

  void map(AbstractTelemetryBuilder telemetryBuilder, AttributeKey attributeKey, Object value) {
    String key = attributeKey.getKey();
    MappingsBuilder.ExactMapping exactMapping = exactMappings.get(key);
    if (exactMapping != null) {
      exactMapping.map(telemetryBuilder, value);
      return;
    }
    MappingsBuilder.PrefixMapping prefixMapping = prefixMappings.getOrNull(key);
    if (prefixMapping != null) {
      prefixMapping.map(telemetryBuilder, key, value);
      return;
    }
    defaultMapping.map(telemetryBuilder, attributeKey, value);
  }
}
