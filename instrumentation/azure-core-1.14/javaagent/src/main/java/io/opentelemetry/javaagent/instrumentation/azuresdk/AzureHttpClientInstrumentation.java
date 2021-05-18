/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.azuresdk;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.implementsInterfaceBetter;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;

import com.azure.core.http.HttpResponse;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import reactor.core.publisher.Mono;

public class AzureHttpClientInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return implementsInterfaceBetter(named("com.azure.core.http.HttpClient"));
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return singletonMap(
        isMethod()
            .and(isPublic())
            .and(named("send"))
            .and(returns(named("reactor.core.publisher.Mono"))),
        this.getClass().getName() + "$SuppressNestedClientAdvice");
  }

  public static class SuppressNestedClientAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void methodExit(@Advice.Return(readOnly = false) Mono<HttpResponse> mono) {
      mono = new SuppressNestedClientMono<>(mono);
    }
  }
}
