package com.microsoft.applicationinsights.channel;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class TransmissionFileSystemOutputTest {
    private final String workingFolder;
    private final static String TRANSMISSION_FILE_EXTENSION = "trn";
    private final static int SIZE_OF_TRANSMISSION_CONTENT = 10;
    private final static String TEMP_TEST_FOLDER = "TransmissionTests";

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
        testSuccessfulSends(2, 1, new Long(SIZE_OF_TRANSMISSION_CONTENT - 1));
    }

    @Test
    public void testSuccessfulSendTenFilesWhereThereIsNoRoomForTheLastThree() throws Exception {
        testSuccessfulSends(10, 7, new Long(SIZE_OF_TRANSMISSION_CONTENT * 7));
    }

    @Test
    public void testStop() throws Exception {
    }

    private void testSuccessfulSends(int amount) throws Exception {
        testSuccessfulSends(amount, amount, null);
    }

    private void testSuccessfulSends(int amount, int expectedSuccess, Long capacity) throws Exception {
        File folder = createFolderForTest();
        try {
            createAndSend(amount, capacity);

            Collection<File> transmissions = FileUtils.listFiles(folder, new String[]{TRANSMISSION_FILE_EXTENSION}, false);

            assertNotNull(transmissions);
            assertEquals(transmissions.size(), expectedSuccess);
        } finally {
            if (folder.exists()) {
                FileUtils.deleteDirectory(folder);
            }
        }
    }

    private void createAndSend(int amount, Long capacity) {
        TransmissionFileSystemOutput tested = new TransmissionFileSystemOutput(workingFolder);
        if (capacity != null) {
            tested.setCapacity(capacity);
        }

        for (int i = 0; i < amount; ++i) {
            tested.send(new Transmission(new byte[SIZE_OF_TRANSMISSION_CONTENT], "MockContentType", "MockEncodingType"));
        }
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