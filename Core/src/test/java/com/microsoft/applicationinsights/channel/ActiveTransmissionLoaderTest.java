package com.microsoft.applicationinsights.channel;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.Mockito;

import org.apache.commons.io.FileUtils;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;

public class ActiveTransmissionLoaderTest {
    private final static String TEMP_TEST_FOLDER = "TransmissionTests";

    @Test(expected = NullPointerException.class)
    public void testNullFileSystem() throws Exception {
        new ActiveTransmissionLoader(null, Mockito.mock(TransmissionDispatcher.class), 1);
    }

    @Test(expected = NullPointerException.class)
    public void testNullDispatcher() throws Exception {
        testIllegalState(null, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZeroThreads() throws Exception {
        testIllegalState(Mockito.mock(TransmissionDispatcher.class), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeNumberOfThreads() throws Exception {
        testIllegalState(Mockito.mock(TransmissionDispatcher.class), -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTooManyThreads() throws Exception {
        testIllegalState(Mockito.mock(TransmissionDispatcher.class), ActiveTransmissionLoader.MAX_THREADS_ALLOWED + 1);
    }

    @Test
    public void testOneFileOnDiskBeforeLoaderStarted() throws Exception {
        testFilesOnDiskAreLoaded(1, true);
    }

    @Test
    public void testTwoFilesOnDiskBeforeLoaderStarted() throws Exception {
        testFilesOnDiskAreLoaded(2, true);
    }

    @Test
    public void testOneFileOnDiskAfterLoaderStarted() throws Exception {
        testFilesOnDiskAreLoaded(1, false);
    }

    @Test
    public void testTwoFilesOnDisAfterLoaderStarted() throws Exception {
        testFilesOnDiskAreLoaded(2, false);
    }

    private void testFilesOnDiskAreLoaded(int amount, boolean putFilesFirst) throws IOException, InterruptedException {
        File folder = null;
        ActiveTransmissionLoader tested = null;
        try {
            String filesPath = System.getProperty("java.io.tmpdir") + File.separator + TEMP_TEST_FOLDER;
            folder = new File(filesPath);
            if (folder.exists()) {
                FileUtils.deleteDirectory(folder);
            }
            if (!folder.exists()) {
                folder.mkdir();
            }

            TransmissionFileSystemOutput fileSystem = new TransmissionFileSystemOutput(filesPath);
            TransmissionDispatcher mockDispatcher = Mockito.mock(TransmissionDispatcher.class);
            tested = new ActiveTransmissionLoader(fileSystem, mockDispatcher, 2);
            if (!putFilesFirst) {
                boolean ok = tested.load(true);
                assertTrue("Failed to load", ok);
            }

            for (int i = 0; i < amount; ++i) {
                fileSystem.send(new Transmission(new byte[2], "MockContentType", "MockEncodingType"));
            }

            if (putFilesFirst) {
                boolean ok = tested.load(true);
                assertTrue("Failed to load", ok);
            }

            Thread.sleep(3000);
            Mockito.verify(mockDispatcher, Mockito.times(amount)).dispatch((Transmission) anyObject());
            Mockito.verify(mockDispatcher, Mockito.times(amount)).dispatch((Transmission) anyObject());

        } finally {
            if (tested != null) {
                tested.stop(1L, TimeUnit.SECONDS);
            }

            if (folder != null && folder.exists()) {
                FileUtils.deleteDirectory(folder);
            }
        }
    }

    private void testIllegalState(final TransmissionDispatcher dispatcher, int numberOfThreads) throws Exception {
        File folder = null;
        try {
            String filesPath = System.getProperty("java.io.tmpdir") + File.separator + TEMP_TEST_FOLDER;
            folder = new File(filesPath);
            if (folder.exists()) {
                FileUtils.deleteDirectory(folder);
            }
            if (!folder.exists()) {
                folder.mkdir();
            }

            TransmissionFileSystemOutput mock = new TransmissionFileSystemOutput(filesPath);
            new ActiveTransmissionLoader(mock, dispatcher, numberOfThreads);
        } finally {
            if (folder != null && folder.exists()) {
                FileUtils.deleteDirectory(folder);
            }
        }
    }

}