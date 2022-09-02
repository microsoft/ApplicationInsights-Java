// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.netty.v4_1.appid;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.opentelemetry.javaagent.bootstrap.CallDepth;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class ChannelPipelineInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("io.netty.channel.ChannelPipeline");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return implementsInterface(named("io.netty.channel.ChannelPipeline"));
  }

  @Override
  public void transform(TypeTransformer typeTransformer) {
    typeTransformer.applyAdviceToMethod(
        isMethod()
            .and(nameStartsWith("add"))
            .and(takesArgument(2, named("io.netty.channel.ChannelHandler"))),
        this.getClass().getName() + "$ChannelPipelineAddAdvice");
  }

  @SuppressWarnings("PrivateConstructorForUtilityClass")
  public static class ChannelPipelineAddAdvice {
    @Advice.OnMethodEnter
    public static void trackCallDepth(@Advice.Local("aiCallDepth") CallDepth callDepth) {
      callDepth = CallDepth.forClass(AppIdHandler.class);
      callDepth.getAndIncrement();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void addHandler(
        @Advice.Local("aiCallDepth") CallDepth callDepth,
        @Advice.This ChannelPipeline pipeline,
        @Advice.Argument(2) ChannelHandler handler) {
      if (callDepth.decrementAndGet() > 0) {
        return;
      }

      try {
        if (handler instanceof HttpServerCodec || handler instanceof HttpResponseEncoder) {
          pipeline.addLast(AppIdHandler.class.getName(), new AppIdHandler());
        }
      } catch (IllegalArgumentException e) {
        // Prevented adding duplicate handlers.
      }
    }
  }
}
