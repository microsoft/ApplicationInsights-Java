// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.AiSemanticAttributes;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.ContextCustomizer;
import io.opentelemetry.semconv.UrlAttributes;
import io.opentelemetry.semconv.incubating.HttpIncubatingAttributes;
import java.util.List;
import javax.annotation.Nullable;

// TODO (trask) emit warning in exporter if user is using the (now broken) undocumented use of
//  ai.preview.connection_string, ai.preview.service_name, and ai.preview.instrumentation_key
//  for programmatic access customers can use classic SDK's
//  ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry()
//     .getContext().setConnectionString(...)
//     .getContext().getCloud().setRole(...)
public class AiContextCustomizer<R> implements ContextCustomizer<R> {

  private final List<Configuration.ConnectionStringOverride> connectionStringOverrides;
  private final List<Configuration.RoleNameOverride> roleNameOverrides;

  public AiContextCustomizer(
      List<Configuration.ConnectionStringOverride> connectionStringOverrides,
      List<Configuration.RoleNameOverride> roleNameOverrides) {
    this.connectionStringOverrides = connectionStringOverrides;
    this.roleNameOverrides = roleNameOverrides;
  }

  @Override
  @SuppressWarnings(
      "deprecation") // support deprecated semconv attributes for backwards compatibility
  public Context onStart(Context context, R request, Attributes startAttributes) {

    // TODO (trask) ideally would also check parentSpanContext !isValid || isRemote
    // but context has the span in it, not the parentSpan

    Context newContext = context;

    String path =
        getStableAttribute(
            startAttributes, UrlAttributes.URL_PATH, HttpIncubatingAttributes.HTTP_TARGET);

    String connectionStringOverride = getConnectionStringOverride(path);
    if (connectionStringOverride != null) {
      newContext = newContext.with(AiContextKeys.CONNECTION_STRING, connectionStringOverride);
      // InheritedConnectionStringSpanProcessor will stamp connection string attribute from the
      // context onto other spans, but this onStart() occurs after spanStart(), so we must stamp
      // this span separately
      Span span = Span.fromContext(newContext);
      span.setAttribute(AiSemanticAttributes.INTERNAL_CONNECTION_STRING, connectionStringOverride);
    }

    String roleNameOverride = getRoleNameOverride(path);
    if (roleNameOverride != null) {
      newContext = newContext.with(AiContextKeys.ROLE_NAME, roleNameOverride);
      // InheritedRoleNameSpanProcessor will stamp role name attribute from the
      // context onto other spans, but this onStart() occurs after spanStart(), so we must stamp
      // this span separately
      Span span = Span.fromContext(newContext);
      span.setAttribute(AiSemanticAttributes.INTERNAL_ROLE_NAME, roleNameOverride);
    }

    return newContext;
  }

  @Nullable
  private String getConnectionStringOverride(String target) {
    if (target == null) {
      return null;
    }
    for (Configuration.ConnectionStringOverride override : connectionStringOverrides) {
      if (target.startsWith(override.httpPathPrefix)) {
        return override.connectionString;
      }
    }
    return null;
  }

  @Nullable
  private String getRoleNameOverride(String target) {
    if (target == null) {
      return null;
    }
    for (Configuration.RoleNameOverride override : roleNameOverrides) {
      if (target.startsWith(override.httpPathPrefix)) {
        return override.roleName;
      }
    }
    return null;
  }

  private static <T> T getStableAttribute(
      Attributes attributes, AttributeKey<T> stable, AttributeKey<T> old) {
    T value = attributes.get(stable);
    if (value != null) {
      return value;
    }
    return attributes.get(old);
  }
}
