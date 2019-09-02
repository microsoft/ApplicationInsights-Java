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

package com.microsoft.applicationinsights.instrumentation.azurefunctions;

import org.glowroot.instrumentation.api.Agent;
import org.glowroot.instrumentation.api.Getter;
import org.glowroot.instrumentation.api.MessageSupplier;
import org.glowroot.instrumentation.api.OptionalThreadContext;
import org.glowroot.instrumentation.api.OptionalThreadContext.AlreadyInTransactionBehavior;
import org.glowroot.instrumentation.api.Span;
import org.glowroot.instrumentation.api.TimerName;
import org.glowroot.instrumentation.api.checker.Nullable;
import org.glowroot.instrumentation.api.weaving.Advice;
import org.glowroot.instrumentation.api.weaving.Bind;
import org.glowroot.instrumentation.api.weaving.Shim;

public class AzureFunctionsInstrumentation {

    private static final TimerName TIMER_NAME = Agent.getTimerName("azure fn");

    private static final MessageSupplier MESSAGE_SUPPLIER = MessageSupplier.create("azure fn");

    private static final Getter<RpcTraceContext> GETTER = new HttpRequestMessageGetter();

    @Shim("com.microsoft.azure.functions.rpc.messages.InvocationRequest")
    public interface InvocationRequest {

        @Shim("com.microsoft.azure.functions.rpc.messages.RpcTraceContext getTraceContext()")
        RpcTraceContext getTraceContext();
    }

    @Shim("com.microsoft.azure.functions.rpc.messages.RpcTraceContext")
    public interface RpcTraceContext {

        String getTraceParent();

        String getTraceState();
    }

    @Advice.Pointcut(
            className = "com.microsoft.azure.functions.worker.handler.InvocationRequestHandler",
            methodName = "execute",
            methodParameterTypes = {"com.microsoft.azure.functions.rpc.messages.InvocationRequest", ".."},
            nestingGroup = "outer-servlet-or-filter")
    public static class ExecuteAdvice {

        @Advice.OnMethodBefore
        public static Span onBefore(OptionalThreadContext context,
                                    @Bind.Argument(0) InvocationRequest request) {

            return context.startIncomingSpan("ai.internal:AZUREFN", "AZUREFN", GETTER, request.getTraceContext(),
                    MESSAGE_SUPPLIER, TIMER_NAME, AlreadyInTransactionBehavior.CAPTURE_NEW_TRANSACTION);
        }

        @Advice.OnMethodReturn
        public static void onReturn(@Bind.Enter Span span) {
            span.end();
        }

        @Advice.OnMethodThrow
        public static void onThrow(@Bind.Thrown Throwable t,
                                   @Bind.Enter Span span) {
            span.endWithError(t);
        }
    }

    private static class HttpRequestMessageGetter implements Getter<RpcTraceContext> {

        @Override
        public @Nullable String get(RpcTraceContext carrier, String key) {
            // TODO what about older AI headers if W3C is not enabled?
            if (key.equals("traceparent")) {
                return carrier.getTraceParent();
            } else if (key.equals("tracestate")) {
                return carrier.getTraceState();
            } else {
                return null;
            }
        }
    }
}
