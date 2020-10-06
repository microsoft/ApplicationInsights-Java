package com.microsoft.applicationinsights.agent.internal.processors;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
    private final SpanProcessorIncludeExclude include;
    private final SpanProcessorIncludeExclude exclude;
    private final List<SpanProcessorAction> insertActions;
    private final List<SpanProcessorAction> otherActions;

    public ExampleExporter(SpanProcessorConfig config, SpanExporter delegate) {
        this.include = config.include != null && config.include.isValid() ? config.include : null;
        this.exclude = config.exclude != null && config.exclude.isValid() ? config.exclude : null;
        this.insertActions = new ArrayList<>();
        this.otherActions = new ArrayList<>();
        if (config.actions != null) {
            for (SpanProcessorAction spanProcessorAction : config.actions) {
                if (spanProcessorAction.isValid()) {
                    if (spanProcessorAction.action == SpanProcessorActionType.INSERT) {
                        this.insertActions.add(spanProcessorAction);
                    } else {
                        this.otherActions.add(spanProcessorAction);
                    }
                }
            }
        }
        this.delegate = delegate;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        // we need to filter attributes before passing on to delegate
        List<SpanData> copy = new ArrayList<>();
        for (SpanData span : spans) {
            copy.add(process(span));
        }
        return delegate.export(copy);
    }

    private SpanData process(SpanData span) {

        String spanName = span.getName();
        ReadableAttributes existingSpanAttributes = span.getAttributes();
        SpanData updatedSpan = null;
        boolean includeFlag = true;//Flag to check if the span is included in processing
        boolean excludeFlag = false;//Flag to check if the span is excluded in processing
        if (this.include != null) {
            includeFlag = checkIncludes(this.include, existingSpanAttributes, spanName);
        }
        if (this.exclude != null) {
            excludeFlag = checkExcludes(this.exclude, existingSpanAttributes, spanName);
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

    private boolean checkExcludes(SpanProcessorIncludeExclude excludes, ReadableAttributes existingSpanAttributes, String spanName) {

        if (excludes.matchType == SpanProcessorMatchType.STRICT) {
            boolean matchFound = false;
            if (excludes.spanNames != null && !excludes.spanNames.isEmpty()) {
                if (excludes.spanNames.contains(spanName)) {
                    //match found
                    matchFound = true;
                }
            } else {
                // if span_names is empty default to true
                matchFound = true;
            }

            if (excludes.attributes != null) {
                for (SpanProcessorAttribute attribute : excludes.attributes) {


                    if (matchFound && existingSpanAttributes.get(attribute.key) != null) {
                        //found a match
                        if (attribute.value != null) {

                            AttributeValue existingAttributeValue = existingSpanAttributes.get(attribute.key);
                            return existingAttributeValue.getType() == Type.STRING && existingAttributeValue.getStringValue().equals(attribute.value);
                            // found a match with value
                        } else {
                            return true;
                        }
                    }

                }
            }

            return matchFound;

        } else if (excludes.matchType == SpanProcessorMatchType.REGEXP) {
            boolean matchFound = false;
            if (excludes.spanNames != null) {
                for (String service : excludes.spanNames) {
                    if (spanName.matches(service)) {
                        matchFound = true;
                        break;
                    }
                }
            } else {
                matchFound = true;
            }

            if (excludes.attributes != null) {
                for (SpanProcessorAttribute attribute : excludes.attributes) {


                    if (matchFound && existingSpanAttributes.get(attribute.key) != null) {
                        //found a match
                        if (attribute.value != null) {

                            AttributeValue existingAttributeValue = existingSpanAttributes.get(attribute.key);
                            return existingAttributeValue.getType() == Type.STRING && existingAttributeValue.getStringValue().equals(attribute.value);
                            // found a match with value
                        } else {
                            return true;
                        }
                    }

                }
            }

            return matchFound;
        }
        return false;
    }

    private boolean checkIncludes(SpanProcessorIncludeExclude includes, ReadableAttributes existingSpanAttributes, String spanName) {

        if (includes.matchType == SpanProcessorMatchType.STRICT) {
            if (includes.spanNames != null) {
                if (!includes.spanNames.contains(spanName)) {
                    //did not find a match
                    return false;
                }
            }
            if (includes.attributes != null) {
                for (SpanProcessorAttribute attribute : includes.attributes) {


                    if (existingSpanAttributes.get(attribute.key) != null) {
                        //found a match
                        if (attribute.value != null) {

                            AttributeValue existingAttributeValue = existingSpanAttributes.get(attribute.key);
                            if (existingAttributeValue.getType() == Type.STRING && existingAttributeValue.getStringValue().equals(attribute.value)) {
                                // found a match with value
                                return true;
                            }
                        } else {
                            return true;
                        }
                    }

                }
                //did not find a match
                return false;
            }

        } else if (includes.matchType == SpanProcessorMatchType.REGEXP) {

            if (includes.spanNames != null) {
                boolean foundMatch = false;
                for (String service : includes.spanNames) {
                    if (spanName.matches(service)) {
                        foundMatch = true;
                        break;
                    }
                }
                // did not find a match
                if (!foundMatch) return false;
            }
            if (includes.attributes != null) {
                for (SpanProcessorAttribute attribute : includes.attributes) {


                    if (existingSpanAttributes.get(attribute.key) != null) {
                        //found a match
                        if (attribute.value != null) {

                            AttributeValue existingAttributeValue = existingSpanAttributes.get(attribute.key);
                            if (existingAttributeValue.getType() == Type.STRING && existingAttributeValue.getStringValue().equals(attribute.value)) {
                                // found a match with value
                                return true;
                            }
                        } else {
                            return true;
                        }
                    }

                }
                //did not find a match
                return false;
            }
        }

        return true;
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
