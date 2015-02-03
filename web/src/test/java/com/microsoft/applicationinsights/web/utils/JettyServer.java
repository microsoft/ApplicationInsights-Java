package com.microsoft.applicationinsights.web.utils;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import java.util.EnumSet;
import com.microsoft.applicationinsights.web.internal.WebRequestTrackingFilter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;

/**
 * Created by yonisha on 2/3/2015.
 */
public class JettyServer {
    private Server server;
    private FilterHolder filterHolder;

    public void start() throws Exception {
        server = new Server(1234);

        //Initialize the server
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addServlet(Callback200Servlet.class, "/");
        filterHolder = context.addFilter(WebRequestTrackingFilter.class, "/*", EnumSet.of(DispatcherType.INCLUDE, DispatcherType.REQUEST));

        server.setHandler(context);
        server.start();
    }

    public void shutdown() throws Exception {
        if (server == null) {
            return;
        }

        server.stop();
        server.destroy();
    }
}
