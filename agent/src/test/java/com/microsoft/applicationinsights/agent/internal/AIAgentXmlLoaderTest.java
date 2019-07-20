package com.microsoft.applicationinsights.agent.internal;

import org.junit.Assert;
import org.junit.Test;

public class AIAgentXmlLoaderTest {

    @Test
    public void testValidJavaIdentifier() {
        Assert.assertFalse(AIAgentXmlLoader.validJavaIdentifier(""));
        Assert.assertFalse(AIAgentXmlLoader.validJavaIdentifier("1"));
        Assert.assertFalse(AIAgentXmlLoader.validJavaIdentifier("a*b"));
        Assert.assertFalse(AIAgentXmlLoader.validJavaIdentifier("/a"));
        Assert.assertTrue(AIAgentXmlLoader.validJavaIdentifier("a"));
        Assert.assertTrue(AIAgentXmlLoader.validJavaIdentifier("a1"));
    }

    @Test
    public void testValidJavaFqcn() {
        Assert.assertFalse(AIAgentXmlLoader.validJavaFqcn(""));
        Assert.assertFalse(AIAgentXmlLoader.validJavaFqcn("1"));
        Assert.assertFalse(AIAgentXmlLoader.validJavaFqcn("a*b"));
        Assert.assertFalse(AIAgentXmlLoader.validJavaFqcn("/a"));
        Assert.assertTrue(AIAgentXmlLoader.validJavaFqcn("a"));
        Assert.assertTrue(AIAgentXmlLoader.validJavaFqcn("a1"));
    }

    @Test
    public void testValidJavaFqcn2() {
        Assert.assertFalse(AIAgentXmlLoader.validJavaFqcn("xyz."));
        Assert.assertFalse(AIAgentXmlLoader.validJavaFqcn("xyz.1"));
        Assert.assertFalse(AIAgentXmlLoader.validJavaFqcn("xyz.a*b"));
        Assert.assertFalse(AIAgentXmlLoader.validJavaFqcn("xyz./a"));
        Assert.assertTrue(AIAgentXmlLoader.validJavaFqcn("xyz.a"));
        Assert.assertTrue(AIAgentXmlLoader.validJavaFqcn("xyz.a1"));
    }
}
