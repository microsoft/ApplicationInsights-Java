package com.microsoft.applicationinsights.extensibility;

import com.microsoft.applicationinsights.channel.InProcessTelemetryChannel;
import com.microsoft.applicationinsights.channel.SimpleHttpChannel;
import com.microsoft.applicationinsights.implementation.DeviceInfoContextInitializer;
import com.microsoft.applicationinsights.implementation.PropertiesContextInitializer;
import com.microsoft.applicationinsights.implementation.SdkVersionContextInitializer;
import com.microsoft.applicationinsights.util.PropertyHelper;

import java.util.List;
import java.util.Properties;

/**
 * Initializer class for configuration instances.
 */
public class TelemetryConfigurationFactory
{
    private static TelemetryConfigurationFactory s_instance;

    public static TelemetryConfigurationFactory getInstance()
    {
        if (s_instance == null)
            s_instance = new TelemetryConfigurationFactory();
        return s_instance;
    }

    public final void Initialize(TelemetryConfiguration configuration)
    {
        List<ContextInitializer> initializerList = configuration.getContextInitializers();
        initializerList.add(new SdkVersionContextInitializer());
        initializerList.add(new DeviceInfoContextInitializer());
        initializerList.add(new PropertiesContextInitializer());

        // Load properties file data.
        Properties props = PropertyHelper.getProperties();

        if (props.containsKey("ai.instrumentationKey"))
        {
            configuration.setInstrumentationKey(props.get("ai.instrumentationKey").toString());
        }

        //configuration.setChannel(new StdOutChannel());
        configuration.setChannel(new SimpleHttpChannel());
    }
}
