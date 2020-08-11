/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.jul;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.extendsClass;
import static java.util.Collections.singletonList;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import application.java.util.logging.Logger;
import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.api.CallDepthThreadLocalMap;
import io.opentelemetry.javaagent.instrumentation.api.logger.LoggerDepth;
import java.util.List;
import java.util.logging.LogRecord;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(InstrumentationModule.class)
public class JavaUtilLoggingSpansInstrumentationModule extends InstrumentationModule {
  public JavaUtilLoggingSpansInstrumentationModule() {
    // this name is important currently because it's used to disable this instrumentation
    super("java-util-logging-spans");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return singletonList(new JavaUtilLoggingSpansInstrumentation());
  }

  private static final class JavaUtilLoggingSpansInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
      return extendsClass(named("application.java.util.logging.Logger"));
    }

    // TODO adding classLoaderOptimization doesn't work (tests fail)

    @Override
    public void transform(TypeTransformer transformer) {
      transformer.applyAdviceToMethod(
          isMethod()
              .and(isPublic())
              .and(named("log"))
              .and(takesArguments(1))
              .and(takesArgument(0, named("java.util.logging.LogRecord"))),
          JavaUtilLoggingSpansInstrumentationModule.class.getName() + "$LogAdvice");
      transformer.applyAdviceToMethod(
          isMethod()
              .and(isPublic())
              .and(named("logRaw"))
              .and(takesArguments(1))
              .and(takesArgument(0, named("org.jboss.logmanager.ExtLogRecord"))),
          JavaUtilLoggingSpansInstrumentationModule.class.getName() + "$LogAdvice");
    }
  }

  public static class LogAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static boolean methodEnter(
        @Advice.This final Logger logger, @Advice.Argument(0) final LogRecord logRecord) {
      // need to track call depth across all loggers in order to avoid double capture when one
      // logging framework delegates to another
      boolean topLevel = CallDepthThreadLocalMap.incrementCallDepth(LoggerDepth.class) == 0;
      if (topLevel) {
        JavaUtilLoggingSpans.capture(logger, logRecord);
      }
      return topLevel;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void methodExit(@Advice.Enter final boolean topLevel) {
      if (topLevel) {
        CallDepthThreadLocalMap.reset(LoggerDepth.class);
      }
    }
  }
}
