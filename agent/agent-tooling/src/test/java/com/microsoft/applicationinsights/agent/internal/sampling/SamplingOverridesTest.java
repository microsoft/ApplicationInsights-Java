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

package com.microsoft.applicationinsights.agent.internal.sampling;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.MatchType;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.SamplingOverride;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.SamplingOverrideAttribute;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class SamplingOverridesTest {

  @Test
  void shouldSampleByDefault() {
    // given
    List<SamplingOverride> overrides = new ArrayList<>();
    SamplingOverrides sampler = new SamplingOverrides(overrides);
    Attributes attributes = Attributes.empty();

    // expect
    assertThat(sampler.getOverride(attributes)).isNull();
  }

  @Test
  void shouldFilterStrictMatch() {
    // given
    List<SamplingOverride> overrides =
        singletonList(newOverride(0, newStrictAttribute("one", "1")));
    SamplingOverrides sampler = new SamplingOverrides(overrides);
    Attributes attributes = Attributes.of(AttributeKey.stringKey("one"), "1");

    // expect
    assertThat(sampler.getOverride(attributes).getPercentage()).isEqualTo(0);
  }

  @Test
  void shouldNotFilterStrictMatch() {
    // given
    List<SamplingOverride> overrides =
        singletonList(newOverride(0, newStrictAttribute("one", "1")));
    SamplingOverrides sampler = new SamplingOverrides(overrides);
    Attributes attributes = Attributes.of(AttributeKey.stringKey("one"), "2");

    // expect
    assertThat(sampler.getOverride(attributes)).isNull();
  }

  @Test
  void shouldNotFilterMissingStrictMatch() {
    // given
    List<SamplingOverride> overrides =
        singletonList(newOverride(0, newStrictAttribute("one", "1")));
    SamplingOverrides sampler = new SamplingOverrides(overrides);
    Attributes attributes = Attributes.of(AttributeKey.stringKey("two"), "1");

    // expect
    assertThat(sampler.getOverride(attributes)).isNull();
  }

  @Test
  void shouldFilterRegexpMatch() {
    // given
    List<SamplingOverride> overrides =
        singletonList(newOverride(0, newRegexpAttribute("one", "1.*")));
    SamplingOverrides sampler = new SamplingOverrides(overrides);
    Attributes attributes = Attributes.of(AttributeKey.stringKey("one"), "11");

    // expect
    assertThat(sampler.getOverride(attributes).getPercentage()).isEqualTo(0);
  }

  @Test
  void shouldNotFilterRegexpMatch() {
    // given
    List<SamplingOverride> overrides =
        singletonList(newOverride(0, newRegexpAttribute("one", "1.*")));
    SamplingOverrides sampler = new SamplingOverrides(overrides);
    Attributes attributes = Attributes.of(AttributeKey.stringKey("one"), "22");

    // expect
    assertThat(sampler.getOverride(attributes)).isNull();
  }

  @Test
  void shouldNotFilterMissingRegexpMatch() {
    // given
    List<SamplingOverride> overrides =
        singletonList(newOverride(0, newRegexpAttribute("one", "1.*")));
    SamplingOverrides sampler = new SamplingOverrides(overrides);
    Attributes attributes = Attributes.of(AttributeKey.stringKey("two"), "11");

    // expect
    assertThat(sampler.getOverride(attributes)).isNull();
  }

  @Test
  void shouldFilterMultiAttributes() {
    // given
    List<SamplingOverride> overrides =
        singletonList(
            newOverride(0, newStrictAttribute("one", "1"), newRegexpAttribute("two", "2.*")));
    SamplingOverrides sampler = new SamplingOverrides(overrides);
    Attributes attributes =
        Attributes.of(AttributeKey.stringKey("one"), "1", AttributeKey.stringKey("two"), "22");

    // expect
    assertThat(sampler.getOverride(attributes).getPercentage()).isEqualTo(0);
  }

  @Test
  void shouldNotFilterMultiAttributes() {
    // given
    List<SamplingOverride> overrides =
        singletonList(
            newOverride(0, newStrictAttribute("one", "1"), newRegexpAttribute("two", "2.*")));
    SamplingOverrides sampler = new SamplingOverrides(overrides);
    Attributes attributes =
        Attributes.of(AttributeKey.stringKey("one"), "2", AttributeKey.stringKey("two"), "22");

    // expect
    assertThat(sampler.getOverride(attributes)).isNull();
  }

  @Test
  void shouldFilterMultiConfigsBothMatch() {
    // given
    List<SamplingOverride> overrides =
        Arrays.asList(
            newOverride(0, newStrictAttribute("one", "1")),
            newOverride(0, newRegexpAttribute("two", "2.*")));
    SamplingOverrides sampler = new SamplingOverrides(overrides);
    Attributes attributes =
        Attributes.of(AttributeKey.stringKey("one"), "1", AttributeKey.stringKey("two"), "22");

    // expect
    assertThat(sampler.getOverride(attributes).getPercentage()).isEqualTo(0);
  }

  @Test
  void shouldFilterMultiConfigsOneMatch() {
    // given
    List<SamplingOverride> overrides =
        Arrays.asList(
            newOverride(0, newStrictAttribute("one", "1")),
            newOverride(0, newRegexpAttribute("two", "2.*")));
    SamplingOverrides sampler = new SamplingOverrides(overrides);
    Attributes attributes =
        Attributes.of(AttributeKey.stringKey("one"), "2", AttributeKey.stringKey("two"), "22");

    // expect
    assertThat(sampler.getOverride(attributes).getPercentage()).isEqualTo(0);
  }

  @Test
  void shouldNotFilterMultiConfigsNoMatch() {
    // given
    List<SamplingOverride> overrides =
        Arrays.asList(
            newOverride(0, newStrictAttribute("one", "1")),
            newOverride(0, newRegexpAttribute("two", "2.*")));
    SamplingOverrides sampler = new SamplingOverrides(overrides);
    Attributes attributes =
        Attributes.of(AttributeKey.stringKey("one"), "2", AttributeKey.stringKey("two"), "33");

    // expect
    assertThat(sampler.getOverride(attributes)).isNull();
  }

  private static SamplingOverride newOverride(
      float percentage, SamplingOverrideAttribute... attribute) {
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
}
