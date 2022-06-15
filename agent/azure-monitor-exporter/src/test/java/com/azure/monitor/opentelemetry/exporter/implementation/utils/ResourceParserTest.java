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

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.monitor.opentelemetry.exporter.implementation.models.ContextTagKeys;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;

class ResourceParserTest {

  private static final String DEFAULT_ROLE_NAME = "unknown_service:java";
  private static final String DEFAULT_ROLE_INSTANCE = System.getenv("HOSTNAME");

  @Test
  void testNullResource() {
    assertThat(ResourceParser.parseRoleNameAndInstance(null)).isNull();
  }

  @Test
  void testDefaultResource() {
    Map<ContextTagKeys, String> result =
        ResourceParser.parseRoleNameAndInstance(Resource.create(Attributes.empty()));
    assertThat(result.get(ContextTagKeys.AI_CLOUD_ROLE)).isEqualTo(DEFAULT_ROLE_NAME);
    assertThat(result.get(ContextTagKeys.AI_CLOUD_ROLE_INSTANCE)).isEqualTo(DEFAULT_ROLE_INSTANCE);
  }

  @Test
  void testServiceNameFromResource() {
    Resource resource = createTestResource("fake-service-name", null, null);
    Map<ContextTagKeys, String> result = ResourceParser.parseRoleNameAndInstance(resource);
    assertThat(result.get(ContextTagKeys.AI_CLOUD_ROLE)).isEqualTo("fake-service-name");
    assertThat(result.get(ContextTagKeys.AI_CLOUD_ROLE_INSTANCE)).isEqualTo(DEFAULT_ROLE_INSTANCE);
  }

  @Test
  void testServiceInstanceFromResource() {
    Resource resource = createTestResource(null, null, "fake-service-instance");
    Map<ContextTagKeys, String> result = ResourceParser.parseRoleNameAndInstance(resource);
    assertThat(result.get(ContextTagKeys.AI_CLOUD_ROLE)).isEqualTo(DEFAULT_ROLE_NAME);
    assertThat(result.get(ContextTagKeys.AI_CLOUD_ROLE_INSTANCE))
        .isEqualTo("fake-service-instance");
  }

  @Test
  void testServiceNamespaceFromResource() {
    Resource resource = createTestResource(null, "fake-service-namespace", null);
    Map<ContextTagKeys, String> result = ResourceParser.parseRoleNameAndInstance(resource);
    assertThat(result.get(ContextTagKeys.AI_CLOUD_ROLE))
        .isEqualTo("fake-service-namespace." + DEFAULT_ROLE_NAME);
    assertThat(result.get(ContextTagKeys.AI_CLOUD_ROLE_INSTANCE)).isEqualTo(DEFAULT_ROLE_INSTANCE);
  }

  @Test
  void testServiceNameAndInstanceFromResource() {
    Resource resource = createTestResource("fake-service-name", null, "fake-instance");
    Map<ContextTagKeys, String> result = ResourceParser.parseRoleNameAndInstance(resource);
    assertThat(result.get(ContextTagKeys.AI_CLOUD_ROLE)).isEqualTo("fake-service-name");
    assertThat(result.get(ContextTagKeys.AI_CLOUD_ROLE_INSTANCE)).isEqualTo("fake-instance");
  }

  @Test
  void testServiceNameAndInstanceAndNamespaceFromResource() {
    Resource resource =
        createTestResource("fake-service-name", "fake-service-namespace", "fake-instance");
    Map<ContextTagKeys, String> result = ResourceParser.parseRoleNameAndInstance(resource);
    assertThat(result.get(ContextTagKeys.AI_CLOUD_ROLE))
        .isEqualTo("fake-service-namespace.fake-service-name");
    assertThat(result.get(ContextTagKeys.AI_CLOUD_ROLE_INSTANCE)).isEqualTo("fake-instance");
  }

  private static Resource createTestResource(
      @Nullable String serviceName,
      @Nullable String serviceNameSpace,
      @Nullable String serviceInstance) {
    AttributesBuilder builder = Attributes.builder();
    if (serviceName != null) {
      builder.put(ResourceAttributes.SERVICE_NAME, serviceName);
    }
    if (serviceNameSpace != null) {
      builder.put(ResourceAttributes.SERVICE_NAMESPACE, serviceNameSpace);
    }
    if (serviceInstance != null) {
      builder.put(ResourceAttributes.SERVICE_INSTANCE_ID, serviceInstance);
    }
    return Resource.create(builder.build());
  }
}
