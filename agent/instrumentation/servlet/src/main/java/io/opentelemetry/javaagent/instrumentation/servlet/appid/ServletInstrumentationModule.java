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

package io.opentelemetry.javaagent.instrumentation.servlet.appid;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.namedOneOf;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.google.auto.service.AutoService;
import com.microsoft.applicationinsights.agent.bootstrap.AiAppId;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.bootstrap.CallDepth;
import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(InstrumentationModule.class)
public class ServletInstrumentationModule extends InstrumentationModule {
  public ServletInstrumentationModule() {
    super("servlet");
  }

  // run after the upstream servlet instrumentation
  @Override
  public int order() {
    return 1;
  }

  @Override
  public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
    return hasClassesNamed("javax.servlet.Filter", "javax.servlet.http.HttpServlet");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new ServletAndFilterInstrumentation());
  }

  private static final class ServletAndFilterInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
      return hasSuperType(namedOneOf("javax.servlet.Filter", "javax.servlet.http.HttpServlet"));
    }

    @Override
    public void transform(TypeTransformer typeTransformer) {
      typeTransformer.applyAdviceToMethod(
          namedOneOf("doFilter", "service")
              .and(takesArgument(0, named("javax.servlet.ServletRequest")))
              .and(takesArgument(1, named("javax.servlet.ServletResponse")))
              .and(isPublic()),
          getClass().getName() + "$AddHeadersAdvice");
    }

    @SuppressWarnings({"UnusedMethod", "UnusedNestedClass", "PrivateConstructorForUtilityClass"})
    public static class AddHeadersAdvice {
      @Advice.OnMethodEnter(suppress = Throwable.class)
      public static void onEnter(
          @Advice.Argument(1) ServletResponse response,
          @Advice.Local("aiCallDepth") CallDepth callDepth) {
        if (response instanceof HttpServletResponse) {
          HttpServletResponse httpResponse = (HttpServletResponse) response;
          Context context = Java8BytecodeBridge.currentContext();

          // Make sure call depth is only increased if we actually have a span. Otherwise, in the
          // corner case where the outermost invocation does not have a valid context yet, but an
          // inner one does, no headers would be set.
          if (!Java8BytecodeBridge.spanFromContext(context).getSpanContext().isValid()) {
            return;
          }

          // Only set headers in the outermost invocation, otherwise an inner one could overwrite it
          // with a child span (such as Spring INTERNAL span).
          callDepth = CallDepth.forClass(CallDepthKey.class);
          if (callDepth.getAndIncrement() > 0) {
            return;
          }

          String appId = AiAppId.getAppId();
          if (!appId.isEmpty()) {
            httpResponse.setHeader(AiAppId.RESPONSE_HEADER_NAME, "appId=" + appId);
          }
        }
      }

      @Advice.OnMethodExit(suppress = Throwable.class)
      public static void onExit(@Advice.Local("aiCallDepth") @Nullable CallDepth callDepth) {
        if (callDepth != null) {
          callDepth.decrementAndGet();
        }
      }
    }
  }

  public static class CallDepthKey {}
}
