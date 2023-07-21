// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.processors;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import org.junit.jupiter.api.Test;

class TelemetryProcessorMaskingValidationTest {

  @Test
  void anInvalidRegRxShouldThrowAnException() {

    String invalidRegEx = "\\\\.\\";

    String anyKey = null;
    String anyValue = null;
    String anyFromAttribute = null;
    String anyReplace = null;

    assertThatThrownBy(
            () ->
                new Configuration.ProcessorAction(
                    anyKey,
                    Configuration.ProcessorActionType.MASK,
                    anyValue,
                    anyFromAttribute,
                    invalidRegEx,
                    anyReplace))
        .hasMessageContaining("Telemetry processor configuration does not have valid regex");
  }

  @Test
  void noAttributeKeyShouldThrowAnException() {

    String validRegEx = ".*";
    String noAttributeKey = null;

    String anyValue = null;
    String anyFromAttribute = null;
    String anyReplace = null;

    assertThatThrownBy(
            () ->
                new Configuration.ProcessorAction(
                        noAttributeKey,
                        Configuration.ProcessorActionType.MASK,
                        anyValue,
                        anyFromAttribute,
                        validRegEx,
                        anyReplace)
                    .validate())
        .hasMessageContaining(
            "An attribute processor configuration has an action section that is missing a \"key\"");
  }

  @Test
  void noReplaceShouldThrowAnException() {
    String validRegEx = ".*";
    String anAttributeKey = "http.url";
    String noReplace = null;

    String anyValue = null;
    String anyFromAttribute = null;

    assertThatThrownBy(
            () ->
                new Configuration.ProcessorAction(
                        anAttributeKey,
                        Configuration.ProcessorActionType.MASK,
                        anyValue,
                        anyFromAttribute,
                        validRegEx,
                        noReplace)
                    .validate())
        .hasMessageContaining(
            "An attribute processor configuration has an mask action that is missing")
        .hasMessageContaining("\"replace\" section");
  }

  @Test
  void regexShouldContainAGroupNameToNotFail() {
    String validRegEx = "(?<groupName>.*)";
    String anAttributeKey = "http.url";
    String anyReplacementPattern = "";

    String anyValue = null;
    String anyFromAttribute = null;

    assertThatNoException()
        .isThrownBy(
            () ->
                new Configuration.ProcessorAction(
                        anAttributeKey,
                        Configuration.ProcessorActionType.MASK,
                        anyValue,
                        anyFromAttribute,
                        validRegEx,
                        anyReplacementPattern)
                    .validate());
  }

  @Test
  void
      shouldFailIfTheGroupGameDoesNotMatchTheReplacementValueIndicatedBetweenDollarCurlyBraceAndCurlyBrace() {
    String validRegEx = "(?<groupName>.*)";
    String anAttributeKey = "http.url";
    String replacementPattern = "${" + "replaceValueDifferentFromGroupName" + "}";

    String anyValue = null;
    String anyFromAttribute = null;

    assertThatThrownBy(
            () ->
                new Configuration.ProcessorAction(
                        anAttributeKey,
                        Configuration.ProcessorActionType.MASK,
                        anyValue,
                        anyFromAttribute,
                        validRegEx,
                        replacementPattern)
                    .validate())
        .hasMessageContaining(
            "Please make sure the replace value matches group names used in the `pattern` regex.");
  }
}
