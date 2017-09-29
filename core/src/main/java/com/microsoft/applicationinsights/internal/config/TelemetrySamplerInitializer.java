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

package com.microsoft.applicationinsights.internal.config;

import com.microsoft.applicationinsights.channel.TelemetrySampler;
import com.microsoft.applicationinsights.internal.channel.sampling.AdaptiveTelemetrySampler;
import com.microsoft.applicationinsights.internal.channel.sampling.FixedRateTelemetrySampler;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;

/**
 * Created by gupele on 11/14/2016.
 */
final class TelemetrySamplerInitializer {
    /**
     * Sets the configuration data of Context Initializers in configuration class.
     * @param sampler The configuration data.
     */
    public TelemetrySampler getSampler(SamplerXmlElement sampler) {
        if (sampler == null) {
            return null;
        }

        FixedSamplerXmlElement fixedSamplerXmlElement = sampler.getFixedSamplerXmlElement();
        TelemetrySampler telemetrySampler = null;
        if (fixedSamplerXmlElement != null) {
            Double percentage = null;

            String percentageAsString = fixedSamplerXmlElement.getSamplingPercentage();
            if (percentageAsString != null) {
                percentageAsString = percentageAsString.trim();
            }
            if (!LocalStringsUtils.isNullOrEmpty(percentageAsString)) {
                try {
                    percentage = Double.valueOf(percentageAsString);
                } catch (Exception e) {
                    InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, "Failed to parse %s", percentageAsString);
                }
            }

            telemetrySampler = new FixedRateTelemetrySampler();
            telemetrySampler.setIncludeTypes(fixedSamplerXmlElement.getIncludeTypes());
            telemetrySampler.setExcludeTypes(fixedSamplerXmlElement.getExcludeTypes());
            telemetrySampler.setSamplingPercentage(percentage);
        } else {
            AdaptiveSamplerXmlElement adaptiveSamplerXmlElement = sampler.getAdaptiveSamplerXmlElement();
            if (adaptiveSamplerXmlElement != null) {
                AdaptiveTelemetrySampler adaptiveTelemetrySampler = new AdaptiveTelemetrySampler();

                adaptiveTelemetrySampler.setIncludeTypes(adaptiveSamplerXmlElement.getIncludeTypes());
                adaptiveTelemetrySampler.setExcludeTypes(adaptiveSamplerXmlElement.getExcludeTypes());

                adaptiveTelemetrySampler.initialize(
                        adaptiveSamplerXmlElement.getMaxTelemetryItemsPerSecond(),
                        adaptiveSamplerXmlElement.getEvaluationInterval(),
                        adaptiveSamplerXmlElement.getSamplingPercentageDecreaseTimeout(),
                        adaptiveSamplerXmlElement.getSamplingPercentageIncreaseTimeout(),
                        adaptiveSamplerXmlElement.getMinSamplingPercentage(),
                        adaptiveSamplerXmlElement.getMaxSamplingPercentage(),
                        adaptiveSamplerXmlElement.getInitialSamplingPercentage(),
                        adaptiveSamplerXmlElement.getMovingAverageRatio()
                );

                telemetrySampler = adaptiveTelemetrySampler;
            } else {
                InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, "Could not resolve sampler type. Possible values are 'Fixed' or 'Adaptive'");
            }
        }

        return telemetrySampler;
    }
}
