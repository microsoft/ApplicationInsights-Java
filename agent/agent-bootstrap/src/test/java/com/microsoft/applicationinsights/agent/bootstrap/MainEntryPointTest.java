package com.microsoft.applicationinsights.agent.bootstrap;

import com.microsoft.applicationinsights.agent.bootstrap.customExceptions.FriendlyException;
import org.junit.*;
import static org.junit.Assert.*;

public class MainEntryPointTest {
    @Test
    public void getFriendlyExceptionTest() {
        FriendlyException friendlyException = MainEntryPoint.getFriendlyException(new FriendlyException());
        FriendlyException nonFriendlyException = MainEntryPoint.getFriendlyException(new IllegalArgumentException());
        FriendlyException nestedFriendlyException = MainEntryPoint.getFriendlyException(new RuntimeException("Run time Exception",new FriendlyException()));
        FriendlyException nestedNonFriendlyException = MainEntryPoint.getFriendlyException(new RuntimeException("Run time Exception",new IllegalArgumentException()));
        assertNotNull(friendlyException);
        assertTrue(friendlyException instanceof FriendlyException);
        assertNull(nonFriendlyException);
        assertNotNull(nestedFriendlyException);
        assertTrue(nestedFriendlyException instanceof  FriendlyException);
        assertNull(nestedNonFriendlyException);
    }
}