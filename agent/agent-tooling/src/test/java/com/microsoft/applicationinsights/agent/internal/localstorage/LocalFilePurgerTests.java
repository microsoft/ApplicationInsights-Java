package com.microsoft.applicationinsights.agent.internal.localstorage;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Collection;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class LocalFilePurgerTests {

  @TempDir File tempFolder;

  @Test
  public void testPurgedExpiredFiles() throws InterruptedException {
    String text = "hello world";
    LocalFileCache cache = new LocalFileCache();
    LocalFileWriter writer = new LocalFileWriter(cache, tempFolder);

    // run purge task every second to delete files that are 5 seconds old
    new LocalFilePurger(1L, 5L, tempFolder);

    // persist 100 files to disk
    for (int i = 0; i < 100; i++) {
      writer.writeToDisk(singletonList(ByteBuffer.wrap(text.getBytes(UTF_8))));
    }

    Collection<File> files = FileUtils.listFiles(tempFolder, new String[] {"trn"}, false);
    assertThat(files.size()).isEqualTo(100);

    Thread.sleep(10000); // wait 10 seconds

    files = FileUtils.listFiles(tempFolder, new String[] {"trn"}, false);
    assertThat(files.size()).isEqualTo(0);
  }
}
