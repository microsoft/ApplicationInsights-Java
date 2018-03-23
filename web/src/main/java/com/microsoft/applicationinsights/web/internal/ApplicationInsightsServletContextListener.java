package com.microsoft.applicationinsights.web.internal;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.shutdown.SDKShutdownActivity;
import com.microsoft.applicationinsights.internal.shutdown.Stoppable;

@WebListener
public class ApplicationInsightsServletContextListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent sce) {
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		InternalLogger.INSTANCE.trace("Shutting down threads");
		SDKShutdownActivity.INSTANCE.stopAll();
	}
	
}