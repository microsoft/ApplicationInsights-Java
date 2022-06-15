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

package com.azure.monitor.opentelemetry.exporter.implementation.utils;

import com.azure.monitor.opentelemetry.exporter.implementation.builders.AbstractTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.models.ContextTagKeys;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

public final class ResourceParser {

  public static void updateRoleNameAndInstance(
      AbstractTelemetryBuilder builder, Resource resource) {
    Map<ContextTagKeys, String> resourceMap = ResourceParser.parseRoleNameAndInstance(resource);
    if (!resourceMap.isEmpty()) {
      // map contains AI_CLOUD_ROLE and AI_CLOUD_ROLE_INSTANCE
      resourceMap.forEach(
          (key, value) -> {
            builder.addTag(key.toString(), value);
          });
    }
  }

  // visible for test
  static Map<ContextTagKeys, String> parseRoleNameAndInstance(@Nullable Resource resource) {
    if (resource == null) {
      return Collections.emptyMap();
    }

    String serviceName = resource.getAttribute(ResourceAttributes.SERVICE_NAME);
    if (serviceName == null || serviceName.isEmpty()) {
      serviceName = Resource.getDefault().getAttribute(ResourceAttributes.SERVICE_NAME);
    }

    String serviceNamespace = resource.getAttribute(ResourceAttributes.SERVICE_NAMESPACE);
    if (serviceNamespace == null || serviceNamespace.isEmpty()) {
      serviceNamespace = Resource.getDefault().getAttribute(ResourceAttributes.SERVICE_NAMESPACE);
    }

    String roleName;
    if (serviceName != null && serviceNamespace != null) {
      roleName = serviceNamespace + "." + serviceName;
    } else {
      roleName = serviceName;
    }

    String roleInstance = resource.getAttribute(ResourceAttributes.SERVICE_INSTANCE_ID);
    if (roleInstance == null) {
      roleInstance = System.getenv("HOSTNAME"); // default hostname
    }

    Map<ContextTagKeys, String> map = new HashMap<>(2);
    if (roleName != null) {
      map.put(ContextTagKeys.AI_CLOUD_ROLE, roleName);
    }
    if (roleInstance != null) {
      map.put(ContextTagKeys.AI_CLOUD_ROLE_INSTANCE, roleInstance);
    }

    return map;
  }

  private ResourceParser() {}
}
