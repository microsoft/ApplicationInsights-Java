/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.micrometer;

import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.javaagent.bootstrap.AgentInitializer;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.io.InputStream;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.core.io.ClassPathResource;

// TODO consider applying this instrumentation more generally on ClassLoaders
// TODO cannot test this currently since AGENT_CLASSLOADER is not set in AgentTestRunner
public final class ClassPathResourceInstrumentation implements TypeInstrumentation {

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
      if ("io/opentelemetry/javaagent/instrumentation/micrometer/AzureMonitorAutoConfiguration.class"
          .equals(resource.getPath())) {
        if (AgentInitializer.AGENT_CLASSLOADER != null) {
          return AgentInitializer.AGENT_CLASSLOADER.getResourceAsStream(
              "io/opentelemetry/javaagent/instrumentation/micrometer/AzureMonitorAutoConfiguration.class");
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
