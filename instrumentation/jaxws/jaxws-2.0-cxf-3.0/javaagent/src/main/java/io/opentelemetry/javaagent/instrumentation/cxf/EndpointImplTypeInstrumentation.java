/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.cxf;

import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesNoArguments;

import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;

public class EndpointImplTypeInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("org.apache.cxf.jaxws.JaxWsServerFactoryBean");
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return singletonMap(
        named("create")
            .and(takesNoArguments().and(returns(named("org.apache.cxf.endpoint.Server")))),
        EndpointImplTypeInstrumentation.class.getName() + "$CreateServerAdvice");
  }

  public static class CreateServerAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onEnter(@Advice.Return Server server) {
      Endpoint endpoint = server.getEndpoint();
      endpoint.getInInterceptors().add(new TracingStartInInterceptor());
      endpoint.getInInterceptors().add(new TracingEndInInterceptor());
      endpoint.getOutFaultInterceptors().add(new TracingOutFaultInterceptor());
    }
  }
}
