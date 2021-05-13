package com.microsoft.applicationinsights.internal.statsbeat;

enum ResourceProvider {

    RP_FUNCTIONS("functions"),
    RP_APPSVC("appsvc"),
    RP_VM("vm"),
    RP_AKS("aks"),
    UNKNOWN("unknown");

    private final String id;

    ResourceProvider(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }
}
