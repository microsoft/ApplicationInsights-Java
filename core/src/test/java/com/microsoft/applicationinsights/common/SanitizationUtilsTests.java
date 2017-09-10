package com.microsoft.applicationinsights.common;

import org.junit.Assert;
import org.junit.Test;

public class SanitizationUtilsTests {

    @Test
    public void testSanitizeStringForJSON() {
        String result = SanitizationUtils.sanitizeStringForJSON("{\"entityType\":\"CONTACTNAME\",\"contactField\":\"\",\"query\":\"zuck\",\"candidates\":[{\"id\":\"2\",\"name\":\"Andrew Zuck\",\"aliases\":[],\"phoneNumbers\":[{\"type\":\"MOBILENUMBER\",\"number\":\"+1 (555) 222-2222\"}],\"emailAddresses\":[],\"streetAddresses\":[]}],\"scores\":{\"2\":2.327993297520463}}");
        Assert.assertEquals(result, "{\\\"entityType\\\":\\\"CONTACTNAME\\\",\\\"contactField\\\":\\\"\\\",\\\"query\\\":\\\"zuck\\\",\\\"candidates\\\":[{\\\"id\\\":\\\"2\\\",\\\"name\\\":\\\"Andrew Zuck\\\",\\\"aliases\\\":[],\\\"phoneNumbers\\\":[{\\\"type\\\":\\\"MOBILENUMBER\\\",\\\"number\\\":\\\"+1 (555) 222-2222\\\"}],\\\"emailAddresses\\\":[],\\\"streetAddresses\\\":[]}],\\\"scores\\\":{\\\"2\\\":2.327993297520463}}");
    }
}
