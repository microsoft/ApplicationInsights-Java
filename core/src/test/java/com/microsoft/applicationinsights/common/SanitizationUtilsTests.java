package com.microsoft.applicationinsights.common;

import org.junit.Assert;
import org.junit.Test;

public class SanitizationUtilsTests {

    @Test
    public void testSanitizeStringForJSON() {

        String resutl1 = SanitizationUtils.sanitizeStringForJSON("\\'\\f\\b\\f\\n\\r\\t/\\");
        Assert.assertEquals(resutl1, "\\\\\\'\\\\f\\\\b\\\\f\\\\n\\\\r\\\\t\\/\\\\");

        String resultControlCharVer = SanitizationUtils.sanitizeStringForJSON("\u0000");
        Assert.assertEquals(resultControlCharVer, "\\u0000");
    }
}
