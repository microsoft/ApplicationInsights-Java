/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import io.opentelemetry.instrumentation.test.AgentInstrumentationSpecification
import org.springframework.boot.autoconfigure.AutoConfigurationImportSelector
import org.springframework.core.annotation.AnnotationAttributes
import org.springframework.core.io.ClassPathResource
import org.springframework.core.type.AnnotationMetadata
import spock.lang.Ignore

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

  // TODO cannot test this currently since AGENT_CLASSLOADER is not set in AgentTestRunner
  @Ignore
  def "should read class bytes"() {
    setup:
    def resource =
      new ClassPathResource("io/opentelemetry/auto/instrumentation/micrometer/AzureMonitorAutoConfiguration.class")

    when:
    def input = resource.getInputStream()

    then:
    input != null
  }
}
