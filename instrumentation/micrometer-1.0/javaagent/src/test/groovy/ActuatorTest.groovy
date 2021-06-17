/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */


import io.opentelemetry.instrumentation.test.AgentInstrumentationSpecification
import org.springframework.boot.autoconfigure.AutoConfigurationImportSelector
import org.springframework.core.annotation.AnnotationAttributes
import org.springframework.core.io.ClassPathResource
import org.springframework.core.type.AnnotationMetadata

class ActuatorTest extends AgentInstrumentationSpecification {

  def "should add AzureMonitorAutoConfiguration"() {
    setup:
    def selector = new AutoConfigurationImportSelector()
    def metadata = Mock(AnnotationMetadata)
    def attributes = Mock(AnnotationAttributes)

    when:
    def list = selector.getCandidateConfigurations(metadata, attributes)

    println list

    then:
    list.contains("io.opentelemetry.javaagent.instrumentation.micrometer.AzureMonitorAutoConfiguration")
    !list.contains("com.microsoft.azure.spring.autoconfigure.metrics.AzureMonitorMetricsExportAutoConfiguration")
  }

  def "should read class bytes"() {
    setup:
    def resource =
      new ClassPathResource("io/opentelemetry/javaagent/instrumentation/micrometer/AzureMonitorAutoConfiguration.class")

    when:
    def input = resource.getInputStream()

    then:
    input != null
  }
}
