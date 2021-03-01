package com.microsoft.applicationinsights.alerting.analysis;

public interface AlertPipelineMXBean {

    //Attributes
    long getCoolDown();

    long getRollingAverageWindow();

    long getProfilerDuration();

    float getThreshold();

    double getCurrentAverage();

    boolean enabled();

    boolean isOnCooldown();

    String lastAlertTime();

    //Operations
    // - no operations currently implemented
    //Notifications
    // - no notifications currently implemented
}
