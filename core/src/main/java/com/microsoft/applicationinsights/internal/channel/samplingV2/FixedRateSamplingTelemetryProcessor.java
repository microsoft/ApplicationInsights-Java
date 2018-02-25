package com.microsoft.applicationinsights.internal.channel.samplingV2;

import com.microsoft.applicationinsights.extensibility.TelemetryProcessor;
import com.microsoft.applicationinsights.internal.annotation.BuiltInProcessor;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.EventTelemetry;
import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.PageViewTelemetry;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.SupportSampling;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.telemetry.TraceTelemetry;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * This processor is used to Perform Sampling on User specified sampling rate
 * <p>
 * How to use in ApplicationInsights Configuration :
 * <p>
* <TelemetryProcessors>
    <BuiltInProcessors>
        <Processor type = "FixedRateSamplingTelemetryProcessor">
            <Add name = "SamplingPercentage" value = "50" />
            <ExcludedTypes>
                <ExcludedType>Request</ExcludedType>
            </ExcludedTypes>
            <IncludedTypes>
                <IncludedType>Request</IncludedType>
                <IncludedType>Trace</IncludedType>
                <IncludedType>Dependency</IncludedType>
                <IncludedType>Exception</IncludedType>
            </IncludedTypes>
        </Processor>
    </BuiltInProcessors>
</TelemetryProcessors>
 */
@BuiltInProcessor("FixedRateSamplingTelemetryProcessor")
public final class FixedRateSamplingTelemetryProcessor implements TelemetryProcessor {

    private static Map<TelemetryType, Class> allowedTypes = new HashMap<>();

    static {
        allowedTypes.put(TelemetryType.Dependency, RemoteDependencyTelemetry.class);
        allowedTypes.put(TelemetryType.Event, EventTelemetry.class);
        allowedTypes.put(TelemetryType.Exception, ExceptionTelemetry.class);
        allowedTypes.put(TelemetryType.PageView, PageViewTelemetry.class);
        allowedTypes.put(TelemetryType.Request, RequestTelemetry.class);
        allowedTypes.put(TelemetryType.Trace, TraceTelemetry.class);
    }

    private Set<Class> excludedTypes;

    private Set<Class> includedTypes;

    /**
     *  All sampling percentage must be in a ratio of 100/N where N is a whole number (2, 3, 4, …). E.g. 50 for 1/2 or 33.33 for 1/3.
     *  Failure to follow this pattern can result in unexpected / incorrect computation of values in the portal.
     */
    private double samplingPercentage;

    /**
     * constructor is responsible of initializing this processor
     * to default settings
     */
    public FixedRateSamplingTelemetryProcessor() {
        this.samplingPercentage = 100.00;
        this.includedTypes = new HashSet<>();
        this.excludedTypes = new HashSet<>();
    }

    /**
     * This method returns a set of classes of excluded types specified by user
     *
     * @return
     */
    public Set<Class> getExcludedTypes() {
        return excludedTypes;
    }

    /**
     * This method returns a set of classes of included types specified by user
     *
     * @return
     */
    public Set<Class> getIncludedTypes() {
        return includedTypes;
    }


    private void setIncludedOrExcludedTypes(String value, Set<Class> typeSet) {
        value = value.trim();
        TelemetryType telemetryType = TelemetryType.valueOfOrNull(value);
        if (telemetryType != null) {
            typeSet.add(allowedTypes.get(telemetryType));
        } else {
            InternalLogger.INSTANCE.error("Telemetry type " + value + " is either not allowed to sample or is empty");
        }
    }

    /**
     * Gets the sample rate currently set
     */
    double getSamplingPercentage() {
        return samplingPercentage;
    }

    /**
     * Sets the user defined sampling percentage
     *
     * @param samplingPercentage
     */
    public void setSamplingPercentage(String samplingPercentage) {
        try {
            this.samplingPercentage = Double.valueOf(samplingPercentage);
            InternalLogger.INSTANCE.info("Sampling rate set to " + samplingPercentage);
        }
        catch (NumberFormatException ex) {
            this.samplingPercentage = 100.0;
            InternalLogger.INSTANCE.error("Sampling rate specified in improper format, sampling rate is now set to 100.0 (default)");
            InternalLogger.INSTANCE.trace("stack trace is %s", ExceptionUtils.getStackTrace(ex));
        }
    }

    /**
     * This method determines if the telemetry needs to be sampled or not.
     *
     * @param telemetry
     * @return
     */
    @Override
    public boolean process(Telemetry telemetry) {

        double samplingPercentage = this.samplingPercentage;

        if (telemetry instanceof SupportSampling) {

            if (isSamplingApplicable(telemetry.getClass())) {

                SupportSampling samplingSupportingTelemetry = ((SupportSampling) telemetry);

                if (samplingSupportingTelemetry.getSamplingPercentage() == null) {

                    samplingSupportingTelemetry.setSamplingPercentage(samplingPercentage);

                    if (SamplingScoreGeneratorV2.getSamplingScore(telemetry) >= samplingPercentage) {

                        InternalLogger.INSTANCE.info("Item %s sampled out", telemetry.getClass());
                        return false;
                    }
                } else {
                    InternalLogger.INSTANCE.info("Item has sampling percentage already set to :"
                            + samplingSupportingTelemetry.getSamplingPercentage());
                }
            } else {
                InternalLogger.INSTANCE.trace("Skip sampling since %s type is not sampling applicable", telemetry.getClass());
            }
        }

        return true;
    }

    /**
     * Determines if the argument is applicable for sampling
     *
     * @param item : Denotes the class item to be determined applicable for sampling
     * @return boolean
     */
    private boolean isSamplingApplicable(Class item) {

        if (excludedTypes.size() > 0 && excludedTypes.contains(item)) {
            return false;
        }

        if (includedTypes.size() > 0 && !includedTypes.contains(item)) {
            return false;
        }

        return true;
    }

    /**
     * This method is invoked during configuration to add one element to the
     * excluded types set from the xml array list of excluded types
     * @param value
     */
    public void addToExcludedType(String value) {

        setIncludedOrExcludedTypes(value, excludedTypes);
        InternalLogger.INSTANCE.trace(value + " added as excluded to sampling");

    }

    /**
     * This method is invoked during configuration to add one element to the
     * included types set from the xml array list of included types
     * @param value
     */
    public void addToIncludedType(String value) {

        setIncludedOrExcludedTypes(value, includedTypes);
        InternalLogger.INSTANCE.trace(value + " added as included to sampling");

    }
}
