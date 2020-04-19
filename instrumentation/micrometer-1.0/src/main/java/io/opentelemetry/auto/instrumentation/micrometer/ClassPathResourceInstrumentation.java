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

package io.opentelemetry.auto.instrumentation.micrometer;

import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import com.google.auto.service.AutoService;
import io.opentelemetry.auto.bootstrap.Agent;
import io.opentelemetry.auto.tooling.Instrumenter;
import java.io.InputStream;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.core.io.ClassPathResource;

// TODO consider applying this instrumentation more generally on ClassLoaders
// TODO cannot test this currently since AGENT_CLASSLOADER is not set in AgentTestRunner
@AutoService(Instrumenter.class)
public final class ClassPathResourceInstrumentation extends Instrumenter.Default {

  public ClassPathResourceInstrumentation() {
    super("micrometer-actuator");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("org.springframework.core.io.ClassPathResource");
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return singletonMap(
        named("getInputStream").and(takesArguments(0)).and(returns(InputStream.class)),
        ClassPathResourceInstrumentation.class.getName() + "$GetInputStreamAdvice");
  }

  public static class GetInputStreamAdvice {
    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    public static InputStream onEnter(@Advice.This final ClassPathResource resource) {
      if ("io/opentelemetry/auto/instrumentation/micrometer/AzureMonitorAutoConfiguration.class"
          .equals(resource.getPath())) {
        if (Agent.AGENT_CLASSLOADER != null) {
          return Agent.AGENT_CLASSLOADER.getResourceAsStream(
              "io/opentelemetry/auto/instrumentation/micrometer/AzureMonitorAutoConfiguration.class");
        }
      }
      return null;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(
        @Advice.Return(readOnly = false) InputStream result,
        @Advice.Enter final InputStream resultFromAgentLoader) {

      if (resultFromAgentLoader != null) {
        result = resultFromAgentLoader;
      }
    }
  }
}
