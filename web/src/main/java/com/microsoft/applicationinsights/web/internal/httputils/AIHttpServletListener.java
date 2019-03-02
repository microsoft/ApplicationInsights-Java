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

@Experimental
public final class AIHttpServletListener implements Closeable, AsyncListener {

    private final RequestTelemetryContext context;
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

    }

    @Override
    public void onTimeout(AsyncEvent event) throws IOException {

    }

    @Override
    public void onError(AsyncEvent event) throws IOException {

    }

    @Override
    public void onStartAsync(AsyncEvent event) throws IOException {

    }

    @Override
    public void close() throws IOException {}
}
