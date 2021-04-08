package com.microsoft.applicationinsights.agent.internal.sampling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.SamplingOverrideAttribute;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.SamplingOverride;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.MatchType;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import org.junit.*;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

public class SamplingOverridesTest {

    @Test
    public void shouldSampleByDefault() {
        // given
        List<SamplingOverride> overrides = new ArrayList<>();
        SamplingOverrides sampler = new SamplingOverrides(overrides);
        Attributes attributes = Attributes.empty();

        // expect
        assertNull(sampler.getOverride(attributes));
    }

    @Test
    public void shouldFilterStrictMatch() {
        // given
        List<SamplingOverride> overrides = singletonList(newOverride(0, newStrictAttribute("one", "1")));
        SamplingOverrides sampler = new SamplingOverrides(overrides);
        Attributes attributes = Attributes.of(AttributeKey.stringKey("one"), "1");

        // expect
        assertEquals(0, sampler.getOverride(attributes).getPercentage(), 0);
    }

    @Test
    public void shouldNotFilterStrictMatch() {
        // given
        List<SamplingOverride> overrides = singletonList(newOverride(0, newStrictAttribute("one", "1")));
        SamplingOverrides sampler = new SamplingOverrides(overrides);
        Attributes attributes = Attributes.of(AttributeKey.stringKey("one"), "2");

        // expect
        assertNull(sampler.getOverride(attributes));
    }

    @Test
    public void shouldNotFilterMissingStrictMatch() {
        // given
        List<SamplingOverride> overrides = singletonList(newOverride(0, newStrictAttribute("one", "1")));
        SamplingOverrides sampler = new SamplingOverrides(overrides);
        Attributes attributes = Attributes.of(AttributeKey.stringKey("two"), "1");

        // expect
        assertNull(sampler.getOverride(attributes));
    }

    @Test
    public void shouldFilterRegexpMatch() {
        // given
        List<SamplingOverride> overrides = singletonList(newOverride(0, newRegexpAttribute("one", "1.*")));
        SamplingOverrides sampler = new SamplingOverrides(overrides);
        Attributes attributes = Attributes.of(AttributeKey.stringKey("one"), "11");

        // expect
        assertEquals(0, sampler.getOverride(attributes).getPercentage(), 0);
    }

    @Test
    public void shouldNotFilterRegexpMatch() {
        // given
        List<SamplingOverride> overrides = singletonList(newOverride(0, newRegexpAttribute("one", "1.*")));
        SamplingOverrides sampler = new SamplingOverrides(overrides);
        Attributes attributes = Attributes.of(AttributeKey.stringKey("one"), "22");

        // expect
        assertNull(sampler.getOverride(attributes));
    }

    @Test
    public void shouldNotFilterMissingRegexpMatch() {
        // given
        List<SamplingOverride> overrides = singletonList(newOverride(0, newRegexpAttribute("one", "1.*")));
        SamplingOverrides sampler = new SamplingOverrides(overrides);
        Attributes attributes = Attributes.of(AttributeKey.stringKey("two"), "11");

        // expect
        assertNull(sampler.getOverride(attributes));
    }

    @Test
    public void shouldFilterMultiAttributes() {
        // given
        List<SamplingOverride> overrides = singletonList(newOverride(0, newStrictAttribute("one", "1"), newRegexpAttribute("two", "2.*")));
        SamplingOverrides sampler = new SamplingOverrides(overrides);
        Attributes attributes = Attributes.of(AttributeKey.stringKey("one"), "1", AttributeKey.stringKey("two"), "22");

        // expect
        assertEquals(0, sampler.getOverride(attributes).getPercentage(), 0);
    }

    @Test
    public void shouldNotFilterMultiAttributes() {
        // given
        List<SamplingOverride> overrides = singletonList(newOverride(0, newStrictAttribute("one", "1"), newRegexpAttribute("two", "2.*")));
        SamplingOverrides sampler = new SamplingOverrides(overrides);
        Attributes attributes = Attributes.of(AttributeKey.stringKey("one"), "2", AttributeKey.stringKey("two"), "22");

        // expect
        assertNull(sampler.getOverride(attributes));
    }

    @Test
    public void shouldFilterMultiConfigsBothMatch() {
        // given
        List<SamplingOverride> overrides = Arrays.asList(newOverride(0, newStrictAttribute("one", "1")), newOverride(0, newRegexpAttribute("two", "2.*")));
        SamplingOverrides sampler = new SamplingOverrides(overrides);
        Attributes attributes = Attributes.of(AttributeKey.stringKey("one"), "1", AttributeKey.stringKey("two"), "22");

        // expect
        assertEquals(0, sampler.getOverride(attributes).getPercentage(), 0);
    }

    @Test
    public void shouldFilterMultiConfigsOneMatch() {
        // given
        List<SamplingOverride> overrides = Arrays.asList(newOverride(0, newStrictAttribute("one", "1")), newOverride(0, newRegexpAttribute("two", "2.*")));
        SamplingOverrides sampler = new SamplingOverrides(overrides);
        Attributes attributes = Attributes.of(AttributeKey.stringKey("one"), "2", AttributeKey.stringKey("two"), "22");

        // expect
        assertEquals(0, sampler.getOverride(attributes).getPercentage(), 0);
    }

    @Test
    public void shouldNotFilterMultiConfigsNoMatch() {
        // given
        List<SamplingOverride> overrides = Arrays.asList(newOverride(0, newStrictAttribute("one", "1")), newOverride(0, newRegexpAttribute("two", "2.*")));
        SamplingOverrides sampler = new SamplingOverrides(overrides);
        Attributes attributes = Attributes.of(AttributeKey.stringKey("one"), "2", AttributeKey.stringKey("two"), "33");

        // expect
        assertNull(sampler.getOverride(attributes));
    }

    private SamplingOverride newOverride(double percentage, SamplingOverrideAttribute... attribute) {
        SamplingOverride override = new SamplingOverride();
        override.attributes = Arrays.asList(attribute);
        override.percentage = percentage;
        return override;
    }

    private static SamplingOverrideAttribute newStrictAttribute(String key, String value) {
        SamplingOverrideAttribute attribute = new SamplingOverrideAttribute();
        attribute.key = key;
        attribute.value = value;
        attribute.matchType = MatchType.strict;
        return attribute;
    }

    private static SamplingOverrideAttribute newRegexpAttribute(String key, String value) {
        SamplingOverrideAttribute attribute = new SamplingOverrideAttribute();
        attribute.key = key;
        attribute.value = value;
        attribute.matchType = MatchType.regexp;
        return attribute;
    }
}
