/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.log4j.v2_0;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.extendsClass;
import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.ClassLoaderMatcher.hasClassesNamed;
import static java.util.Collections.singletonList;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isProtected;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.instrumentation.api.CallDepthThreadLocalMap;
import io.opentelemetry.javaagent.instrumentation.api.logger.LoggerDepth;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.Message;

@AutoService(InstrumentationModule.class)
public class Log4jSpansInstrumentationModule extends InstrumentationModule {
  public Log4jSpansInstrumentationModule() {
    // this name is important currently because it's used to disable this instrumentation
    super("log4j");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return singletonList(new Log4jSpansInstrumentation());
  }

  private static final class Log4jSpansInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<? super TypeDescription> typeMatcher() {
      return extendsClass(named("org.apache.logging.log4j.spi.AbstractLogger"));
    }

    @Override
    public ElementMatcher<ClassLoader> classLoaderOptimization() {
      return hasClassesNamed("org.apache.logging.log4j.spi.AbstractLogger");
    }

    @Override
    public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
      Map<ElementMatcher<? super MethodDescription>, String> transformers = new HashMap<>();
      transformers.put(
          isMethod()
              .and(isPublic())
              .and(named("logMessage"))
              .and(takesArguments(5))
              .and(takesArgument(0, named("java.lang.String")))
              .and(takesArgument(1, named("org.apache.logging.log4j.Level")))
              .and(takesArgument(2, named("org.apache.logging.log4j.Marker")))
              .and(takesArgument(3, named("org.apache.logging.log4j.message.Message")))
              .and(takesArgument(4, named("java.lang.Throwable"))),
          Log4jSpansInstrumentationModule.class.getName() + "$LogMessageAdvice");
      // log4j 2.12.1 introduced and started using this new log() method
      transformers.put(
          isMethod()
              .and(isProtected().or(isPublic()))
              .and(named("log"))
              .and(takesArguments(6))
              .and(takesArgument(0, named("org.apache.logging.log4j.Level")))
              .and(takesArgument(1, named("org.apache.logging.log4j.Marker")))
              .and(takesArgument(2, named("java.lang.String")))
              .and(takesArgument(3, named("java.lang.StackTraceElement")))
              .and(takesArgument(4, named("org.apache.logging.log4j.message.Message")))
              .and(takesArgument(5, named("java.lang.Throwable"))),
          Log4jSpansInstrumentationModule.class.getName() + "$LogAdvice");
      return transformers;
    }
  }

  public static class LogMessageAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static boolean methodEnter(
        @Advice.This final Logger logger,
        @Advice.Argument(1) final Level level,
        @Advice.Argument(3) final Message message,
        @Advice.Argument(4) final Throwable t) {
      // need to track call depth across all loggers in order to avoid double capture when one
      // logging framework delegates to another
      boolean topLevel = CallDepthThreadLocalMap.incrementCallDepth(LoggerDepth.class) == 0;
      if (topLevel) {
        Log4jSpans.capture(logger, level, message, t);
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

  public static class LogAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static boolean methodEnter(
        @Advice.This final Logger logger,
        @Advice.Argument(0) final Level level,
        @Advice.Argument(4) final Message message,
        @Advice.Argument(5) final Throwable t) {
      // need to track call depth across all loggers in order to avoid double capture when one
      // logging framework delegates to another
      boolean topLevel = CallDepthThreadLocalMap.incrementCallDepth(LoggerDepth.class) == 0;
      if (topLevel) {
        Log4jSpans.capture(logger, level, message, t);
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
