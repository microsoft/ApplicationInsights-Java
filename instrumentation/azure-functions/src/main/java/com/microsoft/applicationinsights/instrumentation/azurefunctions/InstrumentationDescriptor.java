package com.microsoft.applicationinsights.instrumentation.azurefunctions;

import org.glowroot.instrumentation.api.Descriptor;

@Descriptor(
        id = "azure-functions",
        name = "Azure Functions",
        classes = {
                AzureFunctionsInstrumentation.class
        }
)
public class InstrumentationDescriptor {
}
