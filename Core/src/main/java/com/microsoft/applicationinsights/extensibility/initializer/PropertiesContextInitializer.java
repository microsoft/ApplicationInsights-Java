package com.microsoft.applicationinsights.extensibility.initializer;

import java.util.Map;
import java.util.Properties;

import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.internal.util.MapUtil;
import com.microsoft.applicationinsights.internal.util.PropertyHelper;

import com.google.common.base.Strings;

/**
 * Context initializer loading general properties from a properties file.
 */
public class PropertiesContextInitializer implements ContextInitializer
{
    @Override
    public void Initialize(TelemetryContext context)
    {
        Properties props = PropertyHelper.getProperties();

        Map<String,String> tags = context.getTags();
        Map<String,String> properties = context.getProperties();

        for (Map.Entry<Object,Object> p : props.entrySet())
        {
            String key = (String)p.getKey();
            String value = (String)p.getValue();

            if (Strings.isNullOrEmpty(key)) {
                continue;
            }

            if (key.startsWith("ai."))
            {
                if (!tags.containsKey(key))
                    MapUtil.setStringValueOrRemove(tags, key, value);
            }
            else
            {
                if (!properties.containsKey(key))
                    MapUtil.setStringValueOrRemove(properties, key, value);
            }
        }
    }
}
