package com.springapp.mvc;

import com.microsoft.applicationinsights.TelemetryClient;

public enum ApplicationInsights {
	INSTANCE;
    
    private volatile boolean initialized = false;
    private TelemetryClient telemetryClient;
    
    public TelemetryClient getTelemetryClient() {
           initialize();
           return telemetryClient;
    }
    
    private void initialize() {
           if (!initialized) {
                  synchronized (ApplicationInsights.INSTANCE) {
                        if (!initialized) {
                               telemetryClient = new TelemetryClient();
                               initialized = true;
                        }
                  }
           }
    }
}
