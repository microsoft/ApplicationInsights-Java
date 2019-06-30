package com.microsoft.applicationinsights.web.internal.httputils;

import com.microsoft.applicationinsights.internal.agent.AgentBinding;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import java.io.IOException;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.apache.commons.lang3.Validate;

/**
 * This class implements {@link AsyncListener} to handle span completion for async request handling.
 */
public final class AIHttpServletListener implements AsyncListener {

    /**
     * Instance of {@link RequestTelemetryContext}
     */
    private final RequestTelemetryContext context;

    /**
     * Instance of {@link HttpServerHandler}
     */
    private final HttpServerHandler handler;

    private final AgentBinding agentBinding;

    public AIHttpServletListener(HttpServerHandler handler, RequestTelemetryContext context) {
        this(handler, context, null);
    }

    public AIHttpServletListener(HttpServerHandler handler, RequestTelemetryContext context, AgentBinding agentBinding) {
        Validate.notNull(handler, "HttpServerHandler");
        Validate.notNull(context, "RequestTelemetryContext");
        this.handler = handler;
        this.context = context;
        this.agentBinding = agentBinding;
    }

    @Override
    public void onComplete(AsyncEvent event) {
        ServletRequest request = event.getSuppliedRequest();
        ServletResponse response = event.getSuppliedResponse();
        handler.handleEnd(request, response, context);
        if (agentBinding != null) {
            agentBinding.unbindFromRunawayChildThreads();
        }
    }

    @Override
    public void onTimeout(AsyncEvent event) {
        ServletRequest request = event.getSuppliedRequest();
        ServletResponse response = event.getSuppliedResponse();
        handler.handleEnd(request, response, context);
        if (agentBinding != null) {
            agentBinding.unbindFromRunawayChildThreads();
        }
    }

    @Override
    public void onError(AsyncEvent event) {
        ServletRequest request = event.getSuppliedRequest();
        ServletResponse response = event.getSuppliedResponse();
        Throwable throwable = event.getThrowable();
        if (throwable instanceof Exception) {
            // AI SDK can only track Exceptions. It doesn't support tracking Throwable
            handler.handleException((Exception) throwable);
        } else {
            InternalLogger.INSTANCE.warn("Throwable is not instance of exception, cannot be captured: %s", throwable);
        }
        handler.handleEnd(request, response, context);
        if (agentBinding != null) {
            agentBinding.unbindFromRunawayChildThreads();
        }
    }

    @Override
    public void onStartAsync(AsyncEvent event) throws IOException {
        AsyncContext asyncContext = event.getAsyncContext();
        if (asyncContext != null) {
            asyncContext.addListener(this, event.getSuppliedRequest(), event.getSuppliedResponse());
        }
    }
}
