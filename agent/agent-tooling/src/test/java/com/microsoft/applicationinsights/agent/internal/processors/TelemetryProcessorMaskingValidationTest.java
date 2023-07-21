// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.processors;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import org.junit.jupiter.api.Test;

class TelemetryProcessorMaskingValidationTest {

  @Test
  void an_invalid_reg_ex_should_throw_an_exception() {

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
  void no_attribute_key_should_throw_an_exception() {

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
  void no_replace_should_throw_an_exception() {
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
  void regex_should_contain_a_group_name_to_not_fail() {
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
      should_fail_if_the_group_name_does_not_match_the_replacement_value_indicated_between_dollar_curly_brace_and_curly_brace() {
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
