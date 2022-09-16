// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.sampling;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.agent.internal.SamplingTestUtil;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.MatchType;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.SamplingOverride;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.SamplingOverrideAttribute;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class SamplingOverridesTest {

  @Test
  void shouldSampleByDefault() {
    // given
    List<SamplingOverride> overrides = new ArrayList<>();
    SamplingOverrides samplingOverrides = new SamplingOverrides(overrides);
    Attributes attributes = Attributes.empty();

    // when
    Sampler sampler = samplingOverrides.getOverride(attributes);

    // expect
    assertThat(sampler).isNull();
  }

  @Test
  void shouldFilterInRequest() {
    // given
    List<SamplingOverride> overrides = singletonList(newOverride(25));
    SamplingOverrides samplingOverrides = new SamplingOverrides(overrides);
    Attributes attributes = Attributes.empty();

    // when
    Sampler sampler = samplingOverrides.getOverride(attributes);

    // expect
    assertThat(sampler).isNotNull();
    assertThat(SamplingTestUtil.getCurrentSamplingPercentage(sampler)).isEqualTo(25);
  }

  @Test
  void shouldFilterStrictMatch() {
    // given
    List<SamplingOverride> overrides =
        singletonList(newOverride(25, newStrictAttribute("one", "1")));
    SamplingOverrides samplingOverrides = new SamplingOverrides(overrides);
    Attributes attributes = Attributes.of(AttributeKey.stringKey("one"), "1");

    // when
    Sampler sampler = samplingOverrides.getOverride(attributes);

    // expect
    assertThat(sampler).isNotNull();
    assertThat(SamplingTestUtil.getCurrentSamplingPercentage(sampler)).isEqualTo(25);
  }

  @Test
  void shouldNotFilterStrictMatch() {
    // given
    List<SamplingOverride> overrides =
        singletonList(newOverride(25, newStrictAttribute("one", "1")));
    SamplingOverrides samplingOverrides = new SamplingOverrides(overrides);
    Attributes attributes = Attributes.of(AttributeKey.stringKey("one"), "2");

    // when
    Sampler sampler = samplingOverrides.getOverride(attributes);

    // expect
    assertThat(sampler).isNull();
  }

  @Test
  void shouldNotFilterMissingStrictMatch() {
    // given
    List<SamplingOverride> overrides =
        singletonList(newOverride(25, newStrictAttribute("one", "1")));
    SamplingOverrides samplingOverrides = new SamplingOverrides(overrides);
    Attributes attributes = Attributes.of(AttributeKey.stringKey("two"), "1");

    // when
    Sampler sampler = samplingOverrides.getOverride(attributes);

    // expect
    assertThat(sampler).isNull();
  }

  @Test
  void shouldFilterRegexpMatch() {
    // given
    List<SamplingOverride> overrides =
        singletonList(newOverride(25, newRegexpAttribute("one", "1.*")));
    SamplingOverrides samplingOverrides = new SamplingOverrides(overrides);
    Attributes attributes = Attributes.of(AttributeKey.stringKey("one"), "11");

    // when
    Sampler sampler = samplingOverrides.getOverride(attributes);

    // expect
    assertThat(sampler).isNotNull();
    assertThat(SamplingTestUtil.getCurrentSamplingPercentage(sampler)).isEqualTo(25);
  }

  @Test
  void shouldNotFilterRegexpMatch() {
    // given
    List<SamplingOverride> overrides =
        singletonList(newOverride(25, newRegexpAttribute("one", "1.*")));
    SamplingOverrides samplingOverrides = new SamplingOverrides(overrides);
    Attributes attributes = Attributes.of(AttributeKey.stringKey("one"), "22");

    // when
    Sampler sampler = samplingOverrides.getOverride(attributes);

    // expect
    assertThat(sampler).isNull();
  }

  @Test
  void shouldNotFilterMissingRegexpMatch() {
    // given
    List<SamplingOverride> overrides =
        singletonList(newOverride(25, newRegexpAttribute("one", "1.*")));
    SamplingOverrides samplingOverrides = new SamplingOverrides(overrides);
    Attributes attributes = Attributes.of(AttributeKey.stringKey("two"), "11");

    // when
    Sampler sampler = samplingOverrides.getOverride(attributes);

    // expect
    assertThat(sampler).isNull();
  }

  @Test
  void shouldFilterKeyOnlyMatch() {
    // given
    List<SamplingOverride> overrides = singletonList(newOverride(25, newKeyOnlyAttribute("one")));
    SamplingOverrides samplingOverrides = new SamplingOverrides(overrides);
    Attributes attributes = Attributes.of(AttributeKey.stringKey("one"), "11");

    // when
    Sampler sampler = samplingOverrides.getOverride(attributes);

    // expect
    assertThat(sampler).isNotNull();
    assertThat(SamplingTestUtil.getCurrentSamplingPercentage(sampler)).isEqualTo(25);
  }

  @Test
  void shouldNotFilterKeyOnlyMatch() {
    // given
    List<SamplingOverride> overrides = singletonList(newOverride(25, newKeyOnlyAttribute("one")));
    SamplingOverrides samplingOverrides = new SamplingOverrides(overrides);
    Attributes attributes = Attributes.of(AttributeKey.stringKey("two"), "22");

    // when
    Sampler sampler = samplingOverrides.getOverride(attributes);

    // expect
    assertThat(sampler).isNull();
  }

  @Test
  void shouldFilterMultiAttributes() {
    // given
    List<SamplingOverride> overrides =
        singletonList(
            newOverride(25, newStrictAttribute("one", "1"), newRegexpAttribute("two", "2.*")));
    SamplingOverrides samplerOverride = new SamplingOverrides(overrides);
    Attributes attributes =
        Attributes.of(AttributeKey.stringKey("one"), "1", AttributeKey.stringKey("two"), "22");

    // when
    Sampler sampler = samplerOverride.getOverride(attributes);

    // expect
    assertThat(sampler).isNotNull();
    assertThat(SamplingTestUtil.getCurrentSamplingPercentage(sampler)).isEqualTo(25);
  }

  @Test
  void shouldNotFilterMultiAttributes() {
    // given
    List<SamplingOverride> overrides =
        singletonList(
            newOverride(25, newStrictAttribute("one", "1"), newRegexpAttribute("two", "2.*")));
    SamplingOverrides samplingOverrides = new SamplingOverrides(overrides);
    Attributes attributes =
        Attributes.of(AttributeKey.stringKey("one"), "2", AttributeKey.stringKey("two"), "22");

    // when
    Sampler sampler = samplingOverrides.getOverride(attributes);

    // expect
    assertThat(sampler).isNull();
  }

  @Test
  void shouldFilterMultiConfigsBothMatch() {
    // given
    List<SamplingOverride> overrides =
        Arrays.asList(
            newOverride(25, newStrictAttribute("one", "1")),
            newOverride(0, newRegexpAttribute("two", "2.*")));
    SamplingOverrides samplingOverrides = new SamplingOverrides(overrides);
    Attributes attributes =
        Attributes.of(AttributeKey.stringKey("one"), "1", AttributeKey.stringKey("two"), "22");

    // when
    Sampler sampler = samplingOverrides.getOverride(attributes);

    // expect
    assertThat(sampler).isNotNull();
    assertThat(SamplingTestUtil.getCurrentSamplingPercentage(sampler)).isEqualTo(25);
  }

  @Test
  void shouldFilterMultiConfigsOneMatch() {
    // given
    List<SamplingOverride> overrides =
        Arrays.asList(
            newOverride(50, newStrictAttribute("one", "1")),
            newOverride(25, newRegexpAttribute("two", "2.*")));
    SamplingOverrides samplingOverrides = new SamplingOverrides(overrides);
    Attributes attributes =
        Attributes.of(AttributeKey.stringKey("one"), "2", AttributeKey.stringKey("two"), "22");

    // when
    Sampler sampler = samplingOverrides.getOverride(attributes);

    // expect
    assertThat(sampler).isNotNull();
    assertThat(SamplingTestUtil.getCurrentSamplingPercentage(sampler)).isEqualTo(25);
  }

  @Test
  void shouldNotFilterMultiConfigsNoMatch() {
    // given
    List<SamplingOverride> overrides =
        Arrays.asList(
            newOverride(50, newStrictAttribute("one", "1")),
            newOverride(25, newRegexpAttribute("two", "2.*")));
    SamplingOverrides samplingOverrides = new SamplingOverrides(overrides);
    Attributes attributes =
        Attributes.of(AttributeKey.stringKey("one"), "2", AttributeKey.stringKey("two"), "33");

    // when
    Sampler sampler = samplingOverrides.getOverride(attributes);

    // expect
    assertThat(sampler).isNull();
  }

  private static SamplingOverride newOverride(
      double percentage, SamplingOverrideAttribute... attribute) {
    SamplingOverride override = new SamplingOverride();
    override.attributes = Arrays.asList(attribute);
    override.percentage = percentage;
    return override;
  }

  private static SamplingOverrideAttribute newStrictAttribute(String key, String value) {
    SamplingOverrideAttribute attribute = new SamplingOverrideAttribute();
    attribute.key = key;
    attribute.value = value;
    attribute.matchType = MatchType.STRICT;
    return attribute;
  }

  private static SamplingOverrideAttribute newRegexpAttribute(String key, String value) {
    SamplingOverrideAttribute attribute = new SamplingOverrideAttribute();
    attribute.key = key;
    attribute.value = value;
    attribute.matchType = MatchType.REGEXP;
    return attribute;
  }

  private static SamplingOverrideAttribute newKeyOnlyAttribute(String key) {
    SamplingOverrideAttribute attribute = new SamplingOverrideAttribute();
    attribute.key = key;
    return attribute;
  }
}
