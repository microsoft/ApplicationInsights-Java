/*
 * Application Insights for JavaEE
 */
package com.microsoft.applicationinsights.web.javaee;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 *
 * @author Daichi Isami
 */
@Interceptor
@RequestName
public class RequestNameInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext ic) throws Exception {
        setRequestNameSafe(ic);

        return ic.proceed();
    }

    private void setRequestNameSafe(InvocationContext ic) {
        try {
            RequestTelemetryContext context = ThreadContext.getRequestTelemetryContext();
            if (context != null) {

                String actionName = String.format("%s.%s", ic.getMethod().getDeclaringClass().getName(), ic.getMethod().getName());
                String httpMethod = context.getHttpRequestTelemetry().getHttpMethod();
                String requestName = String.format("%s %s", httpMethod, actionName);
                
                context.getHttpRequestTelemetry().setName(requestName);
            }
        } catch (Exception e) {
            InternalLogger.INSTANCE.error(
                    "Failed to invoke interceptor '%s' with exception: %s.",
                    this.getClass().getSimpleName(),
                    e.getMessage());
        }
    }
}
