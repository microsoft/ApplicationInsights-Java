package com.microsoft.applicationinsights.channel;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.Mockito;

import org.apache.commons.io.FileUtils;

import static org.mockito.Matchers.anyObject;

public class ActiveTransmissionLoaderTest {
    private final static String TEMP_TEST_FOLDER = "TransmissionTests";

    @Test(expected = NullPointerException.class)
    public void testNullFileSystem() throws Exception {
        new ActiveTransmissionLoader(null, Mockito.mock(TransmissionDispatcher.class), 1);
    }

    @Test(expected = NullPointerException.class)
    public void testNullDispatcher() throws Exception {
        TransmissionFileSystemOutput mock = new TransmissionFileSystemOutput();
        new ActiveTransmissionLoader(mock, null, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZeroThreads() throws Exception {
        TransmissionFileSystemOutput mock = new TransmissionFileSystemOutput();
        new ActiveTransmissionLoader(mock, Mockito.mock(TransmissionDispatcher.class), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeNumberOfThreads() throws Exception {
        TransmissionFileSystemOutput mock = new TransmissionFileSystemOutput();
        new ActiveTransmissionLoader(mock, Mockito.mock(TransmissionDispatcher.class), -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTooManyThreads() throws Exception {
        TransmissionFileSystemOutput mock = new TransmissionFileSystemOutput();
        new ActiveTransmissionLoader(mock, Mockito.mock(TransmissionDispatcher.class), ActiveTransmissionLoader.MAX_THREADS_ALLOWED + 1);
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
    public void testTenFilesOnDiskBeforeLoaderStarted() throws Exception {
        testFilesOnDiskAreLoaded(10, true);
    }

    @Test
    public void testOneFileOnDiskAfterLoaderStarted() throws Exception {
        testFilesOnDiskAreLoaded(1, false);
    }

    @Test
    public void testTwoFilesOnDisAfterLoaderStarted() throws Exception {
        testFilesOnDiskAreLoaded(2, false);
    }

    @Test
    public void testTenFilesOnDiskAfterLoaderStarted() throws Exception {
        testFilesOnDiskAreLoaded(10, false);
    }

    private void testFilesOnDiskAreLoaded(int amount, boolean putFilesFirst) throws IOException, InterruptedException {
        TransmissionFileSystemOutput fileSystem = new TransmissionFileSystemOutput();
        TransmissionDispatcher mockDispatcher = Mockito.mock(TransmissionDispatcher.class);
        ActiveTransmissionLoader tested = new ActiveTransmissionLoader(fileSystem, mockDispatcher, 2);
        File folder = null;
        try {
            folder = new File(System.getProperty("java.io.tmpdir") + File.separator + TEMP_TEST_FOLDER);
            if (folder.exists()) {
                FileUtils.deleteDirectory(folder);
            }
            if (!folder.exists()) {
                folder.mkdir();
            }

            if (!putFilesFirst) {
                tested.load();
                Thread.sleep(7000);
            }

            for (int i = 0; i < amount; ++i) {
                fileSystem.send(new Transmission(new byte[2], "MockContentType", "MockEncodingType"));
            }

            if (putFilesFirst) {
                tested.load();
            }
            Thread.sleep(7000);

            Mockito.verify(mockDispatcher, Mockito.times(amount)).dispatch((Transmission) anyObject());
            Mockito.verify(mockDispatcher, Mockito.times(amount)).dispatch((Transmission) anyObject());

        } finally {
            tested.stop(7L, TimeUnit.SECONDS);
            if (folder != null && folder.exists()) {
                FileUtils.deleteDirectory(folder);
            }
        }
    }
}