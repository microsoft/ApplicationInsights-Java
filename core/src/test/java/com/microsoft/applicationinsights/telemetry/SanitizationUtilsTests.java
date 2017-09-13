package com.microsoft.applicationinsights.telemetry;

import com.microsoft.applicationinsights.telemetry.SanitizationUtils;
import org.junit.Assert;
import org.junit.Test;

public class SanitizationUtilsTests {

    @Test
    public void testSanitizeStringForJSON() {

        String result1 = SanitizationUtils.sanitizeStringForJSON("\\'\\f\\b\\f\\n\\r\\t/\\", false);
        Assert.assertEquals(result1, "\\\\\\'\\\\f\\\\b\\\\f\\\\n\\\\r\\\\t\\/\\\\");

        String resultControlCharVer = SanitizationUtils.sanitizeStringForJSON("\u0000", true);
        Assert.assertEquals(resultControlCharVer, "\\u0000");
    }
}
