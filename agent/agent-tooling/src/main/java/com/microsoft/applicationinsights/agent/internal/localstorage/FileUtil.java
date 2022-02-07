package com.microsoft.applicationinsights.agent.internal.localstorage;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class FileUtil {

  public static Collection<File> listFiles(File directory, String[] extensions, boolean recursive) {
    return FileUtils.listFiles(directory, extensions, recursive);
  }

  public static String getBaseName(String fileName) {
    return FilenameUtils.getBaseName(fileName);
  }

  public static void moveFile(File srcFile, File destFile) throws IOException {
    FileUtils.moveFile(srcFile, destFile);
  }
}
