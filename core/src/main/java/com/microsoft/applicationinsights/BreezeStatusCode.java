package com.microsoft.applicationinsights;

public class BreezeStatusCode {

    /**
     * These are some extra status code from Breeze.
     */
    public final static int PARTIAL_SUCCESS = 206;
    public final static int THROTTLED = 429;
    public final static int THROTTLED_OVER_EXTENDED_TIME = 439;
    public final static int CLIENT_SIDE_EXCEPTION = 0;

    private BreezeStatusCode() {}
}
