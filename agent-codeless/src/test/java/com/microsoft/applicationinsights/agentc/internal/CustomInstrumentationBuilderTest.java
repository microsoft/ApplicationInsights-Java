package com.microsoft.applicationinsights.agentc.internal;

import org.junit.Assert;
import org.junit.Test;

public class CustomInstrumentationBuilderTest {

    @Test
    public void testValidJavaIdentifier() {
        Assert.assertFalse(CustomInstrumentationBuilder.validJavaIdentifier(""));
        Assert.assertFalse(CustomInstrumentationBuilder.validJavaIdentifier("1"));
        Assert.assertFalse(CustomInstrumentationBuilder.validJavaIdentifier("a*b"));
        Assert.assertFalse(CustomInstrumentationBuilder.validJavaIdentifier("/a"));
        Assert.assertTrue(CustomInstrumentationBuilder.validJavaIdentifier("a"));
        Assert.assertTrue(CustomInstrumentationBuilder.validJavaIdentifier("a1"));
    }

    @Test
    public void testValidJavaFqcn() {
        Assert.assertFalse(CustomInstrumentationBuilder.validJavaFqcn(""));
        Assert.assertFalse(CustomInstrumentationBuilder.validJavaFqcn("1"));
        Assert.assertFalse(CustomInstrumentationBuilder.validJavaFqcn("a*b"));
        Assert.assertFalse(CustomInstrumentationBuilder.validJavaFqcn("/a"));
        Assert.assertTrue(CustomInstrumentationBuilder.validJavaFqcn("a"));
        Assert.assertTrue(CustomInstrumentationBuilder.validJavaFqcn("a1"));
    }

    @Test
    public void testValidJavaFqcn2() {
        Assert.assertFalse(CustomInstrumentationBuilder.validJavaFqcn("xyz."));
        Assert.assertFalse(CustomInstrumentationBuilder.validJavaFqcn("xyz.1"));
        Assert.assertFalse(CustomInstrumentationBuilder.validJavaFqcn("xyz.a*b"));
        Assert.assertFalse(CustomInstrumentationBuilder.validJavaFqcn("xyz./a"));
        Assert.assertTrue(CustomInstrumentationBuilder.validJavaFqcn("xyz.a"));
        Assert.assertTrue(CustomInstrumentationBuilder.validJavaFqcn("xyz.a1"));
    }
}
