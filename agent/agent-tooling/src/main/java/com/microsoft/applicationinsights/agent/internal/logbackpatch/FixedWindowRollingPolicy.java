// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

// Includes work from:
/*
 * Logback: the reliable, generic, fast and flexible logging framework. Copyright (C) 1999-2015,
 * QOS.ch. All rights reserved.
 *
 * <p>This program and the accompanying materials are dual-licensed under either the terms of the
 * Eclipse Public License v1.0 as published by the Eclipse Foundation
 *
 * <p>or (per the licensee's choosing)
 *
 * <p>under the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation.
 */

package com.microsoft.applicationinsights.agent.internal.logbackpatch;

import static ch.qos.logback.core.CoreConstants.CODES_URL;

import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.rolling.RollingPolicyBase;
import ch.qos.logback.core.rolling.RolloverFailure;
import ch.qos.logback.core.rolling.helper.CompressionMode;
import ch.qos.logback.core.rolling.helper.Compressor;
import ch.qos.logback.core.rolling.helper.FileFilterUtil;
import ch.qos.logback.core.rolling.helper.IntegerTokenConverter;
import ch.qos.logback.core.rolling.helper.RenameUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.util.Date;

// copied from ch.qos.logback.core.rolling.FixedWindowRollingPolicy
// in order to support weird file directories like
@SuppressWarnings({"MethodCanBeStatic", "EmptyBlockTag", "ThrowsUncheckedException"})
public class FixedWindowRollingPolicy extends RollingPolicyBase {

  FileNamePattern fileNamePattern;
  FileNamePattern zipEntryFileNamePattern;

  static final String FNP_NOT_SET =
      "The \"FileNamePattern\" property must be set before using FixedWindowRollingPolicy. ";
  static final String PRUDENT_MODE_UNSUPPORTED =
      "See also " + CODES_URL + "#tbr_fnp_prudent_unsupported";
  static final String SEE_PARENT_FN_NOT_SET =
      "Please refer to " + CODES_URL + "#fwrp_parentFileName_not_set";
  int maxIndex;
  int minIndex;
  RenameUtil util = new RenameUtil();
  Compressor compressor;

  public static final String ZIP_ENTRY_DATE_PATTERN = "yyyy-MM-dd_HHmm";

  /** It's almost always a bad idea to have a large window size, say over 20. */
  private static final int MAX_WINDOW_SIZE = 20;

  public FixedWindowRollingPolicy() {
    minIndex = 1;
    maxIndex = 7;
  }

  @Override
  public void start() {
    util.setContext(this.context);

    if (fileNamePatternStr != null) {
      fileNamePattern = new FileNamePattern(fileNamePatternStr, this.context);
      determineCompressionMode();
    } else {
      addError(FNP_NOT_SET);
      addError(CoreConstants.SEE_FNP_NOT_SET);
      throw new IllegalStateException(FNP_NOT_SET + CoreConstants.SEE_FNP_NOT_SET);
    }

    if (isParentPrudent()) {
      addError("Prudent mode is not supported with FixedWindowRollingPolicy.");
      addError(PRUDENT_MODE_UNSUPPORTED);
      throw new IllegalStateException("Prudent mode is not supported.");
    }

    if (getParentsRawFileProperty() == null) {
      addError("The File name property must be set before using this rolling policy.");
      addError(SEE_PARENT_FN_NOT_SET);
      throw new IllegalStateException("The \"File\" option must be set.");
    }

    if (maxIndex < minIndex) {
      addWarn("MaxIndex (" + maxIndex + ") cannot be smaller than MinIndex (" + minIndex + ").");
      addWarn("Setting maxIndex to equal minIndex.");
      maxIndex = minIndex;
    }

    int maxWindowSize = getMaxWindowSize();
    if ((maxIndex - minIndex) > maxWindowSize) {
      addWarn("Large window sizes are not allowed.");
      maxIndex = minIndex + maxWindowSize;
      addWarn("MaxIndex reduced to " + maxIndex);
    }

    IntegerTokenConverter itc = fileNamePattern.getIntegerTokenConverter();

    if (itc == null) {
      throw new IllegalStateException(
          "FileNamePattern ["
              + fileNamePattern.getPattern()
              + "] does not contain a valid IntegerToken");
    }

    if (compressionMode == CompressionMode.ZIP) {
      String zipEntryFileNamePatternStr = transformFileNamePatternFromInt2Date(fileNamePatternStr);
      zipEntryFileNamePattern = new FileNamePattern(zipEntryFileNamePatternStr, context);
    }
    compressor = new Compressor(compressionMode);
    compressor.setContext(this.context);
    super.start();
  }

  /**
   * Subclasses can override this method to increase the max window size, if required. This is to
   * address LOGBACK-266.
   */
  protected int getMaxWindowSize() {
    return MAX_WINDOW_SIZE;
  }

  private String transformFileNamePatternFromInt2Date(String fileNamePatternStr) {
    String slashified = FileFilterUtil.slashify(fileNamePatternStr);
    String stemOfFileNamePattern = FileFilterUtil.afterLastSlash(slashified);
    return stemOfFileNamePattern.replace("%i", "%d{" + ZIP_ENTRY_DATE_PATTERN + "}");
  }

  @Override
  @SuppressFBWarnings(
      value = "SECPTI", // Potential Path Traversal
      justification =
          "The constructed file path cannot be controlled by an end user of the instrumented application")
  public void rollover() throws RolloverFailure {

    // Inside this method it is guaranteed that the hereto active log file is
    // closed.
    // If maxIndex <= 0, then there is no file renaming to be done.
    if (maxIndex >= 0) {
      // Delete the oldest file, to keep Windows happy.
      File file = new File(fileNamePattern.convertInt(maxIndex));

      if (file.exists()) {
        file.delete();
      }

      // Map {(maxIndex - 1), ..., minIndex} to {maxIndex, ..., minIndex+1}
      for (int i = maxIndex - 1; i >= minIndex; i--) {
        String toRenameStr = fileNamePattern.convertInt(i);
        File toRename = new File(toRenameStr);
        // no point in trying to rename an inexistent file
        if (toRename.exists()) {
          util.rename(toRenameStr, fileNamePattern.convertInt(i + 1));
        } else {
          addInfo("Skipping roll-over for inexistent file " + toRenameStr);
        }
      }

      // move active file name to min
      switch (compressionMode) {
        case NONE:
          util.rename(getActiveFileName(), fileNamePattern.convertInt(minIndex));
          break;
        case GZ:
          compressor.compress(getActiveFileName(), fileNamePattern.convertInt(minIndex), null);
          break;
        case ZIP:
          compressor.compress(
              getActiveFileName(),
              fileNamePattern.convertInt(minIndex),
              zipEntryFileNamePattern.convert(new Date()));
          break;
      }
    }
  }

  /** Return the value of the parent's RawFile property. */
  @Override
  public String getActiveFileName() {
    return getParentsRawFileProperty();
  }

  public int getMaxIndex() {
    return maxIndex;
  }

  public int getMinIndex() {
    return minIndex;
  }

  public void setMaxIndex(int maxIndex) {
    this.maxIndex = maxIndex;
  }

  public void setMinIndex(int minIndex) {
    this.minIndex = minIndex;
  }
}
