package com.microsoft.applicationinsights.agentc.internal.diagnostics;

import java.util.AbstractMap.SimpleImmutableEntry;

public class ApplicationMetadataFactory {
    private final AgentExtensionVersionFinder extensionVersion = new AgentExtensionVersionFinder();
    private final InstrumentationKeyFinder instrumentationKey = new InstrumentationKeyFinder();
    private final MachineNameFinder machineName = new MachineNameFinder();
    private final PidFinder pid = new PidFinder();
    private final SiteNameFinder siteName = new SiteNameFinder();
    private final SubscriptionIdFinder subscriptionId = new SubscriptionIdFinder();
    private final SdkVersionFinder sdkVersion = new SdkVersionFinder();

    ApplicationMetadataFactory() {}

    public static class MetadataEntry extends SimpleImmutableEntry<String, String> {
        private static final long serialVersionUID = 2079366300769487514L;
        public MetadataEntry(String key, String value) {
            super(key, value);
        }
    }

    public MetadataEntry getExtensionVersion() {
        return new MetadataEntry(extensionVersion.getName(), extensionVersion.getValue());
    }

    public MetadataEntry getInstrumentationKey() {
        return new MetadataEntry(instrumentationKey.getName(), instrumentationKey.getValue());
    }


    public MetadataEntry getMachineName() {
        return new MetadataEntry(machineName.getName(), machineName.getValue());
    }


    public MetadataEntry getPid() {
        return new MetadataEntry(pid.getName(), pid.getValue());
    }


    public MetadataEntry getSiteName() {
        return new MetadataEntry(siteName.getName(), siteName.getValue());
    }


    public MetadataEntry getSubscriptionId() {
        return new MetadataEntry(subscriptionId.getName(), subscriptionId.getValue());
    }


    public MetadataEntry getsdkVersion() {
        return new MetadataEntry(sdkVersion.getName(), sdkVersion.getValue());
    }

}