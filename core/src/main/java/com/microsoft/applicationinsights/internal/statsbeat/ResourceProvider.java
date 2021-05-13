package com.microsoft.applicationinsights.internal.statsbeat;

enum ResourceProvider {

    RP_FUNCTIONS("functions"),
    RP_APPSVC("appsvc"),
    RP_VM("vm"),
    RP_AKS("aks"),
    UNKNOWN("unknown");

    private final String value;

    ResourceProvider(String value) {
        this.value = value;
    }

    String getValue() {
        return value;
    }
}
