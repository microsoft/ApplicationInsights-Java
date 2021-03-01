package com.microsoft.applicationinsights.alerting.analysis;

public interface AlertPipelineMXBean {

    //Attributes
    long getCoolDown();

    long getRollingAverageWindow();

    long getProfilerDuration();

    float getThreshold();

    double getCurrentAverage();

    boolean getEnabled();

    boolean isOffCooldown();

    String getLastAlertTime();

    //Operations
    // - no operations currently implemented
    //Notifications
    // - no notifications currently implemented
}
