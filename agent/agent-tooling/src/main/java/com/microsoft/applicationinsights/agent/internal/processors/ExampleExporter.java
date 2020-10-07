package com.microsoft.applicationinsights.agent.internal.processors;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.SpanProcessorAction;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.SpanProcessorActionType;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.SpanProcessorAttribute;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.SpanProcessorConfig;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.SpanProcessorIncludeExclude;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.SpanProcessorMatchType;
import io.opentelemetry.common.AttributeValue;
import io.opentelemetry.common.AttributeValue.Type;
import io.opentelemetry.common.Attributes;
import io.opentelemetry.common.Attributes.Builder;
import io.opentelemetry.common.ReadableAttributes;
import io.opentelemetry.common.ReadableKeyValuePairs.KeyValueConsumer;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class ExampleExporter implements SpanExporter {

    private final SpanExporter delegate;
    private final boolean validConfigFlag;
    private SpanProcessorIncludeExclude include;
    private SpanProcessorIncludeExclude exclude;
    private List<SpanProcessorAction> insertActions;
    private List<SpanProcessorAction> otherActions;
    private List<Pattern> SpanProcessorIncludePatternMatchers;
    private List<Pattern> SpanProcessorExcludePatternMatchers;


    public ExampleExporter(SpanProcessorConfig config, SpanExporter delegate) {
        this.validConfigFlag = config.isValid();
        this.delegate = delegate;
        if (this.validConfigFlag) {
            this.include = config.include != null ? normalizeSpanProcessorIncludeExclude(config.include) : getDefaultSpanProcessorIncludeExclude();
            this.exclude = config.exclude != null ? normalizeSpanProcessorIncludeExclude(config.exclude) : getDefaultSpanProcessorIncludeExclude();
            this.insertActions = new ArrayList<>();
            this.otherActions = new ArrayList<>();
            if (config.actions != null) {
                for (SpanProcessorAction spanProcessorAction : config.actions) {

                    if (spanProcessorAction.action == SpanProcessorActionType.INSERT) {
                        this.insertActions.add(spanProcessorAction);
                    } else {
                        this.otherActions.add(spanProcessorAction);
                    }

                }
            }

            if (this.include.matchType == SpanProcessorMatchType.REGEXP) {
                this.SpanProcessorIncludePatternMatchers = new ArrayList<>();
                for (String regex : this.include.spanNames) {
                    this.SpanProcessorIncludePatternMatchers.add(Pattern.compile(regex));
                }
            }
            if (this.exclude.matchType == SpanProcessorMatchType.REGEXP) {
                this.SpanProcessorExcludePatternMatchers = new ArrayList<>();
                for (String regex : this.exclude.spanNames) {
                    this.SpanProcessorExcludePatternMatchers.add(Pattern.compile(regex));
                }
            }
        }
    }

    private SpanProcessorIncludeExclude getDefaultSpanProcessorIncludeExclude() {
        SpanProcessorIncludeExclude includeExcludeObj = new SpanProcessorIncludeExclude();
        includeExcludeObj.matchType = SpanProcessorMatchType.UNDEFINED;
        includeExcludeObj.spanNames = new ArrayList<>();
        includeExcludeObj.attributes = new ArrayList<>();
        return includeExcludeObj;
    }

    private SpanProcessorIncludeExclude normalizeSpanProcessorIncludeExclude(SpanProcessorIncludeExclude includeExcludeFromConfig) {
        SpanProcessorIncludeExclude includeExcludeObj = new SpanProcessorIncludeExclude();
        includeExcludeObj.matchType = includeExcludeFromConfig.matchType;
        if (includeExcludeFromConfig.spanNames != null) {
            includeExcludeObj.spanNames = includeExcludeFromConfig.spanNames;
        } else {
            includeExcludeObj.spanNames = new ArrayList<>();
        }
        if (includeExcludeFromConfig.attributes != null) {
            includeExcludeObj.attributes = includeExcludeFromConfig.attributes;
        } else {
            includeExcludeObj.attributes = new ArrayList<>();
        }
        return includeExcludeObj;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        // we need to filter attributes before passing on to delegate
        if (this.validConfigFlag) {
            List<SpanData> copy = new ArrayList<>();
            for (SpanData span : spans) {
                copy.add(process(span));
            }
            return delegate.export(copy);
        } else {
            return delegate.export(spans);
        }
    }

    private SpanData process(SpanData span) {

        String spanName = span.getName();
        ReadableAttributes existingSpanAttributes = span.getAttributes();
        SpanData updatedSpan = null;
        boolean includeFlag = true;//Flag to check if the span is included in processing
        boolean excludeFlag = false;//Flag to check if the span is excluded in processing

        if (this.include.matchType != SpanProcessorMatchType.UNDEFINED) {
            //includeFlag = checkIncludes(this.include, existingSpanAttributes, spanName);
            includeFlag = checkIncludesExcludes(this.include, existingSpanAttributes, spanName, true);
        }

        if (!includeFlag) return span;//If Not included we can skip further processing

        if (this.exclude.matchType != SpanProcessorMatchType.UNDEFINED) {
            // excludeFlag = checkExcludes(this.exclude, existingSpanAttributes, spanName);
            excludeFlag = checkIncludesExcludes(this.exclude, existingSpanAttributes, spanName, false);
        }

        if (includeFlag && !excludeFlag) {
            updatedSpan = processOtherActions(span, existingSpanAttributes);
            updatedSpan = processInsertActions(updatedSpan, existingSpanAttributes);
        }
        if (updatedSpan != null) {
            return updatedSpan;
        }
        return span;
    }

    private SpanData processInsertActions(SpanData span, ReadableAttributes existingSpanAttributes) {

        final Builder insertBuilder = Attributes.newBuilder();
        boolean insertedFlag = false;
        for (SpanProcessorAction actionObj : this.insertActions) {
            if (actionObj.value != null) {
                insertBuilder.setAttribute(actionObj.key, actionObj.value);
                insertedFlag = true;
            } else {
                if (existingSpanAttributes.get(actionObj.fromAttribute) != null) {
                    insertBuilder.setAttribute(actionObj.key, existingSpanAttributes.get(actionObj.fromAttribute));
                    insertedFlag = true;
                }
                insertBuilder.setAttribute(actionObj.key, actionObj.value);
            }
        }
        if (insertedFlag) {
            span.getAttributes().forEach(new KeyValueConsumer<AttributeValue>() {
                public void consume(String key, AttributeValue value) {
                    insertBuilder.setAttribute(key, value);
                }
            });
            return new MySpanData(span, insertBuilder.build());
        }
        return span;
    }

    private SpanData processOtherActions(SpanData span, ReadableAttributes existingSpanAttributes) {
        final Builder builder = Attributes.newBuilder();
        List<SpanProcessorAction> _otherActions = this.otherActions;
        // loop over existing attributes
        span.getAttributes().forEach(new KeyValueConsumer<AttributeValue>() {

            @Override
            public void consume(String key, AttributeValue value) {
                boolean updatedFlag = false;// flag to check if a attribute is updated
                for (SpanProcessorAction actionObj : _otherActions) {
                    if (actionObj.key.equals(key)) {
                        switch (actionObj.action) {
                            case UPDATE:
                                if (actionObj.value != null) {
                                    builder.setAttribute(actionObj.key, actionObj.value);
                                    updatedFlag = true;
                                } else if (actionObj.fromAttribute != null) {
                                    AttributeValue existingSpanAttributeValue = existingSpanAttributes.get(actionObj.fromAttribute);
                                    if (existingSpanAttributeValue != null) {
                                        builder.setAttribute(actionObj.key, existingSpanAttributeValue);
                                        updatedFlag = true;
                                    }
                                }
                                break;
                            case DELETE:
                                // Return without copying the existing span attribute
                                return;
                            case HASH:
                                AttributeValue existingSpanAttributeValue = existingSpanAttributes.get(actionObj.key);
                                if (existingSpanAttributeValue != null) {
                                    // Currently we only support String
                                    if (existingSpanAttributeValue.getType() == Type.STRING) {
                                        builder.setAttribute(actionObj.key, getSHA1(existingSpanAttributeValue.getStringValue()));
                                        updatedFlag = true;
                                    }
                                }
                                break;
                        }
                    }
                }
                if (!updatedFlag) {
                    builder.setAttribute(key, value);
                }
            }
        });
        // loop through insert actions, if key is not in keys set then call builder.setAttribute()
        return new MySpanData(span, builder.build());
    }

    private String getSHA1(String value) {
        MessageDigest mDigest = null;
        try {
            mDigest = MessageDigest.getInstance("SHA1");
            byte[] result = mDigest.digest(value.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < result.length; i++) {
                sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return value;
        }
    }

    private boolean checkAttributes(SpanProcessorIncludeExclude includeExclude, ReadableAttributes existingSpanAttributes, String spanName) {
        for (SpanProcessorAttribute attribute : includeExclude.attributes) {
            //All of these attributes must match exactly for a match to occur.
            if (existingSpanAttributes.get(attribute.key) != null) {
                //found a match
                if (attribute.value != null) {
                    AttributeValue existingAttributeValue = existingSpanAttributes.get(attribute.key);
                    if (existingAttributeValue.getType() != Type.STRING || !existingAttributeValue.getStringValue().equals(attribute.value)) {
                        return false;
                        // value mismatch found.
                    }
                }
            } else {
                return false;
            }
        }
        return true;
    }

    private boolean checkIncludesExcludes(SpanProcessorIncludeExclude includeExclude, ReadableAttributes existingSpanAttributes, String spanName, boolean isIncludeObject) {
        if (includeExclude.matchType == SpanProcessorMatchType.STRICT) {
            if (!includeExclude.spanNames.isEmpty()) {
                if (!includeExclude.spanNames.contains(spanName)) {
                    //match found
                    return false;
                }
            }
            return checkAttributes(includeExclude, existingSpanAttributes, spanName);
        } else if (includeExclude.matchType == SpanProcessorMatchType.REGEXP) {
            boolean matchFound = false;
            List<Pattern> patternMatchers = isIncludeObject ? this.SpanProcessorIncludePatternMatchers : this.SpanProcessorExcludePatternMatchers;
            if (patternMatchers != null) {
                for (Pattern pattern : patternMatchers) {
                    if (pattern.matcher(spanName).find()) {
                        matchFound = true;
                        break;
                    }
                }
            }
            if (!matchFound) return false;
            return checkAttributes(includeExclude, existingSpanAttributes, spanName);
        }
        return isIncludeObject;
    }


    @Override
    public CompletableResultCode flush() {
        return delegate.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
        return delegate.shutdown();
    }
}
