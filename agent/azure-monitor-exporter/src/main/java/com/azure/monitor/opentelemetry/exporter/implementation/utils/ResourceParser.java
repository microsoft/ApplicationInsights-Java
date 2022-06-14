package com.azure.monitor.opentelemetry.exporter.implementation.utils;

import com.azure.monitor.opentelemetry.exporter.implementation.builders.AbstractTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.models.ContextTagKeys;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.util.HashMap;
import java.util.Map;

public final class ResourceParser {

  public static void updateRoleNameAndInstance(AbstractTelemetryBuilder builder, Resource resource) {
    Map<ContextTagKeys, String> resourceMap = ResourceParser.parseRoleNameAndInstance(resource);
    if (resourceMap != null && !resourceMap.isEmpty()) {
      resourceMap.forEach((key, value) -> {
        String stringKey = key.toString();
        if (ContextTagKeys.AI_CLOUD_ROLE.toString().equals(stringKey) || ContextTagKeys.AI_CLOUD_ROLE_INSTANCE.toString().equals(stringKey)) {
          builder.addTag(stringKey, value);
        }
      });
    }
  }

  private static Map<ContextTagKeys, String> parseRoleNameAndInstance(Resource resource) {
    if (resource == null) {
      return null;
    }
    String serviceName = resource.getAttribute(ResourceAttributes.SERVICE_NAME);
    String serviceNamespace = resource.getAttribute(ResourceAttributes.SERVICE_NAMESPACE);
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
