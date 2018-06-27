package com.microsoft.applicationinsights.internal.config;

import static com.microsoft.applicationinsights.internal.config.ReflectionUtils.addClass;

public class WebReflectionUtils {
  public static void initialize() {
    addClass(
        com.microsoft.applicationinsights.web.extensibility.modules
            .WebRequestTrackingTelemetryModule.class);
    addClass(
        com.microsoft.applicationinsights.web.extensibility.modules
            .WebSessionTrackingTelemetryModule.class);
    addClass(
        com.microsoft.applicationinsights.web.extensibility.modules.WebUserTrackingTelemetryModule
            .class);
    addClass(
        com.microsoft.applicationinsights.web.internal.perfcounter.WebPerformanceCounterModule
            .class);

    addClass(
        com.microsoft.applicationinsights.web.extensibility.initializers
            .WebOperationIdTelemetryInitializer.class);
    addClass(
        com.microsoft.applicationinsights.web.extensibility.initializers
            .WebOperationNameTelemetryInitializer.class);
    addClass(
        com.microsoft.applicationinsights.web.extensibility.initializers
            .WebSessionTelemetryInitializer.class);
    addClass(
        com.microsoft.applicationinsights.web.extensibility.initializers
            .WebUserAgentTelemetryInitializer.class);
    addClass(
        com.microsoft.applicationinsights.web.extensibility.initializers.WebUserTelemetryInitializer
            .class);
    addClass(
        com.microsoft.applicationinsights.web.extensibility.initializers
            .WebSyntheticRequestTelemetryInitializer.class);

    TelemetryConfigurationFactory.addDefaultPerfModuleClassName(
        com.microsoft.applicationinsights.web.internal.perfcounter.WebPerformanceCounterModule.class
            .getCanonicalName());
  }
}
