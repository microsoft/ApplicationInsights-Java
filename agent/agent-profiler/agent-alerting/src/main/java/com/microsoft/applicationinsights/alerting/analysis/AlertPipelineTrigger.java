/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.applicationinsights.alerting.analysis;

import java.time.ZonedDateTime;
import java.util.function.Consumer;

import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration.AlertConfiguration;

/**
 * Observes a stream of data, and calls a downstream alert action if
 * the following conditions are met:
 * - data moves above given threshold
 * - alert is not in a cooldown period
 * - alert is enabled
 */
public class AlertPipelineTrigger implements Consumer<Double> {
    private final AlertConfiguration alertConfig;
    private final Consumer<AlertBreach> action;
    private ZonedDateTime lastAlertTime;

    public AlertPipelineTrigger(AlertConfiguration alertConfiguration, Consumer<AlertBreach> action) {
        this.alertConfig = alertConfiguration;
        this.action = action;
    }

    @Override
    public void accept(Double telemetry) {
        if (alertConfig.isEnabled() && telemetry > alertConfig.getThreshold()) {
            if (isOffCooldown()) {
                lastAlertTime = ZonedDateTime.now();
                action.accept(new AlertBreach(alertConfig.getType(), telemetry, alertConfig));
            }
        }
    }

    public boolean isOffCooldown() {
        ZonedDateTime coolDownCutOff = ZonedDateTime.now().minusSeconds(alertConfig.getCooldown());
        return lastAlertTime == null || lastAlertTime.isBefore(coolDownCutOff);
    }

    public ZonedDateTime getLastAlertTime() {
        return lastAlertTime;
    }
}
