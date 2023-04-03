// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.json;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.microsoft.applicationinsights.alerting.aiconfig.AlertingConfig;

public class AlertApiModule extends SimpleModule {
  public AlertApiModule() {
    addEnumConfig(AlertingConfig.RequestFilterType.class);
    addEnumConfig(AlertingConfig.RequestAggregationType.class);
    addEnumConfig(AlertingConfig.RequestTriggerThresholdType.class);
    addEnumConfig(AlertingConfig.RequestTriggerThrottlingType.class);
    addEnumConfig(AlertingConfig.RequestAggregationType.class);
  }

  private <T extends Enum<T>> void addEnumConfig(Class<T> clazz) {
    addSerializer(clazz, new LowerCaseEnumSerializers.LowerCaseEnumSerializer<>());
    addDeserializer(clazz, new LowerCaseEnumSerializers.LowerCaseEnumDeSerializer<>(clazz));
  }
}
