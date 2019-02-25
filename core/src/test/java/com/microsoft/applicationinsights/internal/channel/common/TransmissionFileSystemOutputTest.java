/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.internal.channel.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.microsoft.applicationinsights.internal.util.LocalFileSystemUtils;
import java.io.File;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class TransmissionFileSystemOutputTest {
    private final static String TRANSMISSION_FILE_EXTENSION = "trn";

    // This is derived from the following relationship
    // 100 Bytes -> fill 394 bytes of file
    // So by doing the math to fill 1 MB with 3 transmission each size of transmission should be the
    // following
    private final static int SIZE_OF_TRANSMISSION_CONTENT = 349525;
    private final static String TEMP_TEST_FOLDER = "TransmissionTests";
    private final static String MOCK_CONTENT = "MockContent";
    private final static String MOCK_CONTENT_TYPE_BASE = "MockContent";
    private final static String MOCK_ENCODING_TYPE_BASE = "MockEncodingType";
    private final static int SIZE_OF_MOCK_TRANSMISSION = 1;

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

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
    public void testSuccessfulSendTenFilesWhereThereIsNoRoomForTheLastThree() throws Exception {
        testSuccessfulSends(12, 3, new Integer(SIZE_OF_MOCK_TRANSMISSION), null);
    }

    @Ignore("Tests run flaky on Mac OS")
    @Test
    public void testFetchOldestFiles() throws Exception {
        File folder = tmpFolder.newFolder(TEMP_TEST_FOLDER+"2");
        try {
            TransmissionFileSystemOutput tested = new TransmissionFileSystemOutput(folder.getAbsolutePath());

            for (int i = 1; i <= 10; ++i) {
                String iAsString = String.valueOf(i);
                String content = MOCK_CONTENT + iAsString;
                tested.send(new Transmission(content.getBytes(), MOCK_CONTENT_TYPE_BASE + iAsString, MOCK_ENCODING_TYPE_BASE + iAsString));
                TimeUnit.MILLISECONDS.sleep(150); // sleep a bit so 2 files can never have the same timestamp.
            }

            for (int i = 1; i <= 10; ++i) {
                Transmission transmission = tested.fetchOldestFile();
                assertNotNull(transmission);

                String iAsString = String.valueOf(i);
                assertEquals(String.format("Wrong WebContentType %s", transmission.getWebContentType()), MOCK_CONTENT_TYPE_BASE + iAsString, transmission.getWebContentType());
                assertEquals(String.format("Wrong WebContentEncodingType %s", transmission.getWebContentEncodingType()), MOCK_ENCODING_TYPE_BASE + iAsString, transmission.getWebContentEncodingType());
                String fetchedContent = new String(transmission.getContent());
                assertEquals(String.format("Wrong content %s", fetchedContent), MOCK_CONTENT + iAsString, fetchedContent);
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

    private TransmissionFileSystemOutput testSuccessfulSends(int amount, int expectedSuccess, Integer capacity, File testFolder) throws Exception {
        File folder = testFolder == null ? tmpFolder.newFolder(TEMP_TEST_FOLDER) : testFolder;
        TransmissionFileSystemOutput tested = null;
        try {
            tested = createAndSend(folder.getAbsolutePath(), amount, capacity);

            Collection<File> transmissions = FileUtils.listFiles(folder, new String[]{TRANSMISSION_FILE_EXTENSION}, false);

            assertNotNull(transmissions);
            assertEquals(expectedSuccess, transmissions.size());

        } finally {
            if (testFolder == null && folder.exists()) {
                FileUtils.deleteDirectory(folder);
            }
        }

        return tested;
    }

    private TransmissionFileSystemOutput createAndSend(String absoulutePath, int amount, Integer capacity) {
        TransmissionFileSystemOutput tested = null;
        if (capacity != null) {
            tested = new TransmissionFileSystemOutput(absoulutePath, String.valueOf(capacity));;
        } else {
            tested = new TransmissionFileSystemOutput(absoulutePath);
        }

        for (int i = 0; i < amount; ++i) {
            tested.send(new Transmission(new byte[SIZE_OF_TRANSMISSION_CONTENT], "MockContentType", "MockEncodingType"));
        }

        return tested;
    }
}
