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
