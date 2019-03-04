package com.microsoft.applicationinsights.web.internal.httputils;

import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import java.io.Closeable;
import java.io.IOException;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.Validate;
import org.apache.http.annotation.Experimental;

/**
 * This class implements {@link AsyncListener} to handle span completion for async request handling.
 */
@Experimental
public final class AIHttpServletListener implements Closeable, AsyncListener {

    /**
     * Instance of {@link RequestTelemetryContext}
     */
    private final RequestTelemetryContext context;

    /**
     * Instance of {@link HttpServerHandler}
     */
    private final HttpServerHandler<HttpServletRequest, HttpServletResponse> handler;

    public AIHttpServletListener(HttpServerHandler<HttpServletRequest, HttpServletResponse> handler,
                                    RequestTelemetryContext context) {
        Validate.notNull(handler, "HttpServerHandler");
        Validate.notNull(context, "RequestTelemetryContext");
        this.handler = handler;
        this.context = context;
    }

    @Override
    public void onComplete(AsyncEvent event) throws IOException {
        ServletRequest request = event.getSuppliedRequest();
        ServletResponse response = event.getSuppliedResponse();
        if (request instanceof HttpServletRequest
            && response instanceof HttpServletResponse) {
            handler.handleEnd((HttpServletRequest) request, (HttpServletResponse) response, context);
        }
        this.close();
    }

    @Override
    public void onTimeout(AsyncEvent event) throws IOException {
        ServletRequest request = event.getSuppliedRequest();
        ServletResponse response = event.getSuppliedResponse();
        if (request instanceof HttpServletRequest
            && response instanceof HttpServletResponse) {
            handler.handleEnd((HttpServletRequest) request, (HttpServletResponse) response, context);
        }
        this.close();
    }

    @Override
    public void onError(AsyncEvent event) throws IOException {
        ServletRequest request = event.getSuppliedRequest();
        ServletResponse response = event.getSuppliedResponse();
        if (request instanceof HttpServletRequest
            && response instanceof HttpServletResponse) {
            try {
                Throwable throwable = event.getThrowable();
                if (throwable instanceof Exception) {
                    // AI SDK can only track Exceptions. It doesn't support tracking Throwable
                    handler.handleException((Exception) throwable);
                }
            } finally{
                handler.handleEnd((HttpServletRequest) request, (HttpServletResponse) response, context);
            }
        }
        this.close();
    }

    @Override
    public void onStartAsync(AsyncEvent event) throws IOException {
        AsyncContext asyncContext = event.getAsyncContext();
        if (asyncContext != null) {
            asyncContext.addListener(this, event.getSuppliedRequest(), event.getSuppliedResponse());
        }
    }

    @Override
    public void close() throws IOException {}
}
