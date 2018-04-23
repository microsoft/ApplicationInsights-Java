package com.microsoft.applicationinsights.web.internal;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.shutdown.SDKShutdownActivity;

@WebListener
public class ApplicationInsightsServletContextListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent sce) {
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		InternalLogger.INSTANCE.info("Shutting down thread pools");
		SDKShutdownActivity.INSTANCE.stopAll();
	}
	
}