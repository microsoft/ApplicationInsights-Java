/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.opentelemetry.auto.test.AgentTestRunner
import org.springframework.boot.autoconfigure.AutoConfigurationImportSelector
import org.springframework.core.annotation.AnnotationAttributes
import org.springframework.core.io.ClassPathResource
import org.springframework.core.type.AnnotationMetadata
import spock.lang.Ignore

class ActuatorTest extends AgentTestRunner {

  def "should add AzureMonitorAutoConfiguration"() {
    setup:
    def selector = new AutoConfigurationImportSelector()
    def metadata = Mock(AnnotationMetadata)
    def attributes = Mock(AnnotationAttributes)

    when:
    def list = selector.getCandidateConfigurations(metadata, attributes)

    println list

    then:
    list.contains("io.opentelemetry.auto.instrumentation.micrometer.AzureMonitorAutoConfiguration")
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
