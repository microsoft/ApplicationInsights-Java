package com.microsoft.applicationinsights.channel;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.junit.Test;

import org.apache.commons.io.FileUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class TransmissionFileSystemOutputTest {
    private final static String TRANSMISSION_FILE_EXTENSION = "trn";
    private final static int SIZE_OF_TRANSMISSION_CONTENT = 10;
    private final static String TEMP_TEST_FOLDER = "TransmissionTests";
    private final static String MOCK_CONTENT = "MockContent";
    private final static String MOCK_CONTENT_TYPE_BASE = "MockContent";
    private final static String MOCK_ENCODING_TYPE_BASE = "MockEncodingType";
    private final static int SIZE_OF_MOCK_TRANSMISSION = 228;

    private final String workingFolder;

    public TransmissionFileSystemOutputTest() {
        workingFolder = System.getProperty("java.io.tmpdir") + File.separator + TEMP_TEST_FOLDER;
    }

    @Test
    public void testSuccessfulSendOneFile() throws Exception {
        testSuccessfulSends(1);
    }

    @Test
    public void testSuccessfulSendTwoFiles() throws Exception {
        testSuccessfulSends(2);
    }

    @Test
    public void testSuccessfulSendTenFiles() throws Exception {
        testSuccessfulSends(10);
    }

    @Test
    public void testSuccessfulSendTwoFilesWhereThereIsNoRoomForTheSecond() throws Exception {
        testSuccessfulSends(2, 1, new Long(SIZE_OF_MOCK_TRANSMISSION - 1), null);
    }

    @Test
    public void testSuccessfulSendTenFilesWhereThereIsNoRoomForTheLastThree() throws Exception {
        testSuccessfulSends(10, 7, new Long(SIZE_OF_MOCK_TRANSMISSION * 7), null);
    }

    @Test
    public void testStop() throws Exception {
    }

    @Test
    public void testFetchOldestFiles() throws Exception {
        File folder = createFolderForTest();
        try {
            TransmissionFileSystemOutput tested = new TransmissionFileSystemOutput(workingFolder);

            for (int i = 10; i != 0; --i) {
                String iAsString = String.valueOf(i);
                String content = MOCK_CONTENT + iAsString;
                tested.send(new Transmission(content.getBytes(), MOCK_CONTENT_TYPE_BASE + iAsString, MOCK_ENCODING_TYPE_BASE + iAsString));
            }

            for (int i = 1; i <= 10; ++i) {
                Transmission transmission = tested.fetchOldestFile();
                assertNotNull(transmission);

                String iAsString = String.valueOf(i);
                assertEquals(String.format("Wrong WebContentType %s", transmission.getWebContentType()), transmission.getWebContentType(), MOCK_CONTENT_TYPE_BASE + iAsString);
                assertEquals(String.format("Wrong WebContentEncodingType %s", transmission.getWebContentEncodingType()), transmission.getWebContentEncodingType(), MOCK_ENCODING_TYPE_BASE + iAsString);
                String fetchedContent = new String(transmission.getContent());
                assertEquals(String.format("Wrong content %s", fetchedContent), fetchedContent, MOCK_CONTENT + iAsString);
            }

            Transmission transmission = tested.fetchOldestFile();
            assertNull(transmission);
        } finally {
            if (folder.exists()) {
                FileUtils.deleteDirectory(folder);
            }
        }
    }

    private TransmissionFileSystemOutput testSuccessfulSends(int amount) throws Exception {
        return testSuccessfulSends(amount, amount, null, null);
    }

    private TransmissionFileSystemOutput testSuccessfulSends(int amount, int expectedSuccess, Long capacity, File testFolder) throws Exception {
        File folder = testFolder == null ? createFolderForTest() : testFolder;
        TransmissionFileSystemOutput tested = null;
        try {
            tested = createAndSend(amount, capacity);

            Collection<File> transmissions = FileUtils.listFiles(folder, new String[]{TRANSMISSION_FILE_EXTENSION}, false);

            assertNotNull(transmissions);
            assertEquals(transmissions.size(), expectedSuccess);
        } finally {
            if (testFolder == null && folder.exists()) {
                FileUtils.deleteDirectory(folder);
            }
        }

        return tested;
    }

    private TransmissionFileSystemOutput createAndSend(int amount, Long capacity) {
        TransmissionFileSystemOutput tested = new TransmissionFileSystemOutput(workingFolder);
        if (capacity != null) {
            tested.setCapacity(capacity);
        }

        for (int i = 0; i < amount; ++i) {
            tested.send(new Transmission(new byte[SIZE_OF_TRANSMISSION_CONTENT], "MockContentType", "MockEncodingType"));
        }

        return tested;
    }

    private File createFolderForTest() throws IOException {
        File folder = new File(workingFolder);
        if (folder.exists()) {
            FileUtils.deleteDirectory(folder);
        }
        if (!folder.exists()) {
            folder.mkdir();
        }

        return folder;
    }
}