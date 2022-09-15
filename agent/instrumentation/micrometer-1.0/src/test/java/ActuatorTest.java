// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.autoconfigure.AutoConfigurationImportSelector;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.AnnotationMetadata;

class ActuatorTest {

  @Mock AnnotationMetadata metadata;
  @Mock AnnotationAttributes attributes;

  @Test
  void shouldAddAzureMonitorAutoConfiguration() throws Exception {
    // given
    Method method =
        AutoConfigurationImportSelector.class.getDeclaredMethod(
            "getCandidateConfigurations", AnnotationMetadata.class, AnnotationAttributes.class);
    method.setAccessible(true);
    AutoConfigurationImportSelector selector = new AutoConfigurationImportSelector();

    // when
    @SuppressWarnings("unchecked")
    List<String> list = (List<String>) method.invoke(selector, metadata, attributes);

    // then
    assertThat(list)
        .contains(
            "io.opentelemetry.javaagent.instrumentation.micrometer.ai.AzureMonitorAutoConfiguration");
    assertThat(list)
        .doesNotContain(
            "com.microsoft.azure.spring.autoconfigure.metrics.AzureMonitorMetricsExportAutoConfiguration");
  }

  @Test
  void shouldReadClassBytes() throws IOException {
    new AutoConfigurationImportSelector(); // this is needed to trigger helper injection

    // given
    ClassPathResource resource =
        new ClassPathResource(
            "io/opentelemetry/javaagent/instrumentation/micrometer/ai/AzureMonitorAutoConfiguration.class");

    // when
    InputStream input = resource.getInputStream();

    // then
    assertThat(input).isNotNull();
  }
}
