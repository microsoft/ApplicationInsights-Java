package com.microsoft.applicationinsights.web.internal.correlation;

public class InstrumentationKeyResolverTestHelper {
    private InstrumentationKeyResolverTestHelper() {}
    // to allow test access to package-protected method
    public static void setAppIdResolver(ApplicationIdResolver resolver) {
        InstrumentationKeyResolver.INSTANCE.setAppIdResolver(resolver);
    }
}
