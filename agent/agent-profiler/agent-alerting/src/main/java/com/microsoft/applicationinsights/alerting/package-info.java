package com.microsoft.applicationinsights.alerting;

/*
    Provides an alert service that monitors incoming telemetry (currently CPU and MEMORY supported).
    When a metrics rolling average moves above a configured threshold, an alert is issued.
*/
