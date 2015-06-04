package com.microsoft.applicationinsights.internal.channel.common;

import com.microsoft.applicationinsights.internal.reflect.ClassDataUtils;
import com.microsoft.applicationinsights.internal.reflect.ClassDataVerifier;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;

public final class ApacheSenderFactoryTest {
    @Test
    public void testBadJar() throws NoSuchFieldException, IllegalAccessException {
        Field field = ClassDataUtils.class.getDeclaredField("verifier");
        field.setAccessible(true);

        ClassDataVerifier mockVerifier = Mockito.mock(ClassDataVerifier.class);
        Mockito.doReturn(false).when(mockVerifier).verifyClassExists(anyString());
        field.set(ClassDataUtils.INSTANCE, mockVerifier);

        ApacheSender sender = new ApacheSenderFactory().create();
        assertNotNull(sender);
        assertTrue(sender instanceof ApacheSender42);
    }

    @Test
    public void testGoodJar() throws NoSuchFieldException, IllegalAccessException {
        Field field = ClassDataUtils.class.getDeclaredField("verifier");
        field.setAccessible(true);

        ClassDataVerifier mockVerifier = Mockito.mock(ClassDataVerifier.class);
        Mockito.doReturn(true).when(mockVerifier).verifyClassExists(anyString());
        field.set(ClassDataUtils.INSTANCE, mockVerifier);

        ApacheSender sender = new ApacheSenderFactory().create();
        assertNotNull(sender);
        assertTrue(sender instanceof ApacheSender43);
    }
}
