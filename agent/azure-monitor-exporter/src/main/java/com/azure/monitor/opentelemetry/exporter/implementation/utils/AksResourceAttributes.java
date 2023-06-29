// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.monitor.opentelemetry.exporter.implementation.utils;

import static com.azure.monitor.opentelemetry.exporter.implementation.ResourceAttributes.CLOUD_PLATFORM;
import static com.azure.monitor.opentelemetry.exporter.implementation.ResourceAttributes.K8S_CRONJOB_NAME;
import static com.azure.monitor.opentelemetry.exporter.implementation.ResourceAttributes.K8S_DAEMONSET_NAME;
import static com.azure.monitor.opentelemetry.exporter.implementation.ResourceAttributes.K8S_DEPLOYMENT_NAME;
import static com.azure.monitor.opentelemetry.exporter.implementation.ResourceAttributes.K8S_JOB_NAME;
import static com.azure.monitor.opentelemetry.exporter.implementation.ResourceAttributes.K8S_POD_NAME;
import static com.azure.monitor.opentelemetry.exporter.implementation.ResourceAttributes.K8S_REPLICASET_NAME;
import static com.azure.monitor.opentelemetry.exporter.implementation.ResourceAttributes.K8S_STATEFULSET_NAME;
import static com.azure.monitor.opentelemetry.exporter.implementation.ResourceAttributes.SERVICE_INSTANCE_ID;
import static com.azure.monitor.opentelemetry.exporter.implementation.ResourceAttributes.SERVICE_NAME;

import com.azure.core.util.logging.ClientLogger;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class AksResourceAttributes {

  private static final String AZURE_AKS = "azure_aks";
  private static final String UNKNOWN_SERVICE = "unknown_service";
  private static final ClientLogger logger = new ClientLogger(AksResourceAttributes.class);
  public static final Map<String, String> OTEL_RESOURCE_ATTRIBUTES = initOtelResourceAttributes();

  public static boolean isAks() {
    return OTEL_RESOURCE_ATTRIBUTES.get(CLOUD_PLATFORM) != null
        && OTEL_RESOURCE_ATTRIBUTES.get(CLOUD_PLATFORM).equals(AZURE_AKS);
  }

  public static String getAksRoleName() {
    String serviceName = OTEL_RESOURCE_ATTRIBUTES.get(SERVICE_NAME);
    if (!Strings.isNullOrEmpty(serviceName) && !serviceName.startsWith(UNKNOWN_SERVICE)) {
      return serviceName;
    }
    String k8sDeploymentName = OTEL_RESOURCE_ATTRIBUTES.get(K8S_DEPLOYMENT_NAME);
    if (!Strings.isNullOrEmpty(k8sDeploymentName)) {
      return k8sDeploymentName;
    }
    String k8sReplicaSetName = OTEL_RESOURCE_ATTRIBUTES.get(K8S_REPLICASET_NAME);
    if (!Strings.isNullOrEmpty(k8sReplicaSetName)) {
      return k8sReplicaSetName;
    }
    String k8sStatefulSetName = OTEL_RESOURCE_ATTRIBUTES.get(K8S_STATEFULSET_NAME);
    if (!Strings.isNullOrEmpty(k8sStatefulSetName)) {
      return k8sStatefulSetName;
    }
    String k8sJobName = OTEL_RESOURCE_ATTRIBUTES.get(K8S_JOB_NAME);
    if (!Strings.isNullOrEmpty(k8sJobName)) {
      return k8sJobName;
    }
    String k8sCronJobName = OTEL_RESOURCE_ATTRIBUTES.get(K8S_CRONJOB_NAME);
    if (!Strings.isNullOrEmpty(k8sCronJobName)) {
      return k8sCronJobName;
    }
    String k8sDaemonSetName = OTEL_RESOURCE_ATTRIBUTES.get(K8S_DAEMONSET_NAME);
    if (!Strings.isNullOrEmpty(k8sDaemonSetName)) {
      return k8sDaemonSetName;
    }
    return serviceName; // default to "unknown_service:java" when no attribute is available
  }

  public static String getAksRoleInstance() {
    String serviceInstanceId = OTEL_RESOURCE_ATTRIBUTES.get(SERVICE_INSTANCE_ID);
    if (!Strings.isNullOrEmpty(serviceInstanceId)) {
      return serviceInstanceId;
    }
    String k8sPodName = OTEL_RESOURCE_ATTRIBUTES.get(K8S_POD_NAME);
    if (!Strings.isNullOrEmpty(k8sPodName)) {
      return k8sPodName;
    }
    return HostName.get(); // default to hostname
  }

  // visible for testing
  public static Map<String, String> initOtelResourceAttributes() {
    Map<String, String> originalMap =
        DefaultConfigProperties.create(Collections.emptyMap()).getMap("otel.resource.attributes");
    Map<String, String> decodedMap = new HashMap<>(originalMap.size());
    // Attributes specified via otel.resource.attributes follow the W3C Baggage spec and
    // characters outside the baggage-octet range are percent encoded
    // https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/resource/sdk.md#specifying-resource-information-via-an-environment-variable
    originalMap.forEach(
        (key, value) -> {
          try {
            decodedMap.put(key, URLDecoder.decode(value, StandardCharsets.UTF_8.displayName()));
          } catch (UnsupportedEncodingException e) {
            logger.warning("Fail to decode OTEL_RESOURCE_ATTRIBUTES value.", e);
          }
        });
    return decodedMap;
  }

  private AksResourceAttributes() {}
}
