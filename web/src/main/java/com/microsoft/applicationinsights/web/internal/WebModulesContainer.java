package com.microsoft.applicationinsights.web.internal;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.util.List;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.web.extensibility.WebTelemetryModule;

/**
 * Created by yonisha on 2/3/2015.
 */
public class WebModulesContainer {
    private List<WebTelemetryModule> modules;
    private int modulesCount = 0;

    public WebModulesContainer(List<WebTelemetryModule> modules) {
        this.modules = modules;
        this.modulesCount = modules.size();

    }

    public void invokeOnBeginRequest(ServletRequest req, ServletResponse res) {
        for (WebTelemetryModule module : modules) {
            try {
                module.onBeginRequest(req, res);
            } catch (Exception e) {
                InternalLogger.INSTANCE.log(
                        "Web module " + module.getClass().getSimpleName() + " failed on BeginRequest with exception: %s", e.getMessage());
            }
        }


    }

    public void invokeOnEndRequest(ServletRequest req, ServletResponse res) {
        for (WebTelemetryModule module : modules) {
            try {
                module.onEndRequest(req, res);
            } catch (Exception e) {
                InternalLogger.INSTANCE.log(
                        "Web module " + module.getClass().getSimpleName() + " failed on EndRequest with exception: %s", e.getMessage());
            }
        }
    }

    public int getModulesCount() {
        return modulesCount;
    }
}
