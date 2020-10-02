package com.microsoft.applicationinsights.agent.internal.processors;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.SpanProcessorAction;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.SpanProcessorAttribute;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.SpanProcessorConfig;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.SpanProcessorIncludeExclude;
import io.opentelemetry.common.AttributeValue;
import io.opentelemetry.common.AttributeValue.Type;
import io.opentelemetry.common.Attributes;
import io.opentelemetry.common.Attributes.Builder;
import io.opentelemetry.common.ReadableAttributes;
import io.opentelemetry.common.ReadableKeyValuePairs.KeyValueConsumer;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class ExampleExporter implements SpanExporter {

    private final SpanProcessorConfig config;
    private final SpanExporter delegate;

    public static ExampleExporter create(SpanProcessorConfig config, SpanExporter delegate) {
        // optimize data structure
        optimizeSpanProcessorConfig(config);
        return new ExampleExporter(config,delegate);
    }

    private static void optimizeSpanProcessorConfig(SpanProcessorConfig config) {
        if(config!=null && config.actions!=null && config.actions.size()>0) {
            config.insertActions = new ArrayList<>();
            config.otherActions = new ArrayList<>();
            for(SpanProcessorAction spanProcessorAction:config.actions) {
                if(spanProcessorAction.action.equals("insert")) {
                    config.insertActions.add(spanProcessorAction);
                } else {
                    config.otherActions.add(spanProcessorAction);
                }
            }
        }
    }

    private ExampleExporter(SpanProcessorConfig config, SpanExporter delegate) {
        this.config = config;
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

        final String spanName = span.getName();
        final ReadableAttributes existingSpanAttributes = span.getAttributes();
        //Flag to check if the span is included in processing
        Boolean includeFlag = null;
        //Flag to check if the span is excluded in processing
        Boolean excludeFlag = null;
        if(config!=null) {
            SpanData updatedSpan = processOtherActions(config,span,includeFlag,excludeFlag,spanName,existingSpanAttributes);
            return processInsertActions(config,updatedSpan,includeFlag,excludeFlag,spanName,existingSpanAttributes);
        }

        return span;
    }

    private SpanData processInsertActions(SpanProcessorConfig config, SpanData span, Boolean includeFlag, Boolean excludeFlag, String spanName,
                                          ReadableAttributes existingSpanAttributes) {

        final Builder insertBuilder = Attributes.newBuilder();

        if(config.insertActions!=null && config.insertActions.size()>0) {
            if(includeFlag==null) {
                includeFlag = checkIncludes(config.include,existingSpanAttributes,spanName);
            }
            if(excludeFlag==null) {
                excludeFlag=checkExcludes(config.exclude,existingSpanAttributes,spanName);
            }
            if(includeFlag && !excludeFlag) {

                for(SpanProcessorAction actionObj:config.insertActions) {
                    if(actionObj.key!=null && actionObj.value!=null) {
                        insertBuilder.setAttribute(actionObj.key, actionObj.value);
                    }
                }
                span.getAttributes().forEach(new KeyValueConsumer<AttributeValue>() {
                    public void consume(String key, AttributeValue value) {
                        insertBuilder.setAttribute(key, value);
                    }
                });
                return new MySpanData(span, insertBuilder.build());
            }
        }

        return span;
    }

    private SpanData processOtherActions(final SpanProcessorConfig config, SpanData span, Boolean includeFlag, Boolean excludeFlag, String spanName,
                                     final ReadableAttributes existingSpanAttributes) {
        final Builder builder = Attributes.newBuilder();
        if(config.otherActions != null && config.otherActions.size() > 0 ) {
            if(includeFlag==null) {
                includeFlag = checkIncludes(config.include,existingSpanAttributes,spanName);
            }
            if(excludeFlag==null) {
                excludeFlag=checkExcludes(config.exclude,existingSpanAttributes,spanName);
            }
            if(includeFlag && !excludeFlag) {
                // loop over existing attributes
                span.getAttributes().forEach(new KeyValueConsumer<AttributeValue>() {

                    @Override
                    public void consume(String key, AttributeValue value) {
                        // flag to check if a attribute is updated
                        boolean updatedFlag=false;
                        for(SpanProcessorAction actionObj:config.otherActions) {
                            if(actionObj.key!=null && actionObj.action!=null && actionObj.key.equals(key)) {
                                switch (actionObj.action.toLowerCase()) {
                                    case "update":
                                        if(actionObj.value!=null) {
                                            builder.setAttribute(actionObj.key,actionObj.value);
                                            updatedFlag=true;
                                        } else if(actionObj.from_attribute!=null) {
                                            if(existingSpanAttributes.get(actionObj.from_attribute)!=null) {
                                                builder.setAttribute(actionObj.key,existingSpanAttributes.get(actionObj.from_attribute));
                                                updatedFlag=true;
                                            }
                                        }
                                        break;
                                    case "delete":
                                        // Return without copying the existing span attribute
                                        return;
                                    case "hash":
                                        if(existingSpanAttributes.get(actionObj.key)!=null) {
                                            AttributeValue existingValue = existingSpanAttributes.get(actionObj.key);
                                            // Currently we only support String
                                            if(existingValue.getType() == Type.STRING) {
                                                builder.setAttribute(actionObj.key, getSHA1(existingValue.getStringValue()));
                                                updatedFlag=true;
                                            }
                                        }
                                        break;
                                }
                            }
                        }
                        if(!updatedFlag) {
                            builder.setAttribute(key, value);
                        }
                    }
                });
                // loop through insert actions, if key is not in keys set then call builder.setAttribute()
                return new MySpanData(span, builder.build());
            }
        }
        return span;
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
        if(excludes == null || excludes.match_type==null) return false;
        else if(excludes.match_type.equalsIgnoreCase("strict")) {
            boolean matchFound = false;
            if(excludes.span_names!=null && excludes.span_names.size()>0) {
                if(excludes.span_names.contains(spanName)) {
                    //match found
                    matchFound = true;
                }
            } else {
                // if span_names is empty default to true
                matchFound = true;
            }

            if(excludes.attributes!=null && excludes.attributes.size()>0) {
                for(SpanProcessorAttribute attribute:excludes.attributes) {
                    if(matchFound && attribute.key!=null) {
                        //found a match
                        if(existingSpanAttributes.get(attribute.key)!=null) {
                            if(attribute.value!=null) {
                                // found a match with value
                                AttributeValue existingAttributeValue=existingSpanAttributes.get(attribute.key);
                                if(existingAttributeValue.getType() == Type.STRING && existingAttributeValue.getStringValue().equals(attribute.value)) {
                                    return true;
                                } else {
                                    return false;
                                }
                            } else {
                                return true;
                            }
                        }
                    }
                }
            }

            return matchFound;

        } else if(excludes.match_type.equalsIgnoreCase("regexp")) {
            boolean matchFound = false;
            if(excludes.span_names!=null && excludes.span_names.size()>0) {
                for(String service:excludes.span_names) {
                    if(spanName.matches(service)) {
                        matchFound = true;
                        break;
                    }
                }
            } else {
                matchFound = true;
            }

            if(excludes.attributes!=null && excludes.attributes.size()>0) {
                for(SpanProcessorAttribute attribute:excludes.attributes) {
                    if(matchFound && attribute.key!=null) {
                        //found a match
                        if(existingSpanAttributes.get(attribute.key)!=null) {
                            if(attribute.value!=null) {
                                // found a match with value
                                AttributeValue existingAttributeValue=existingSpanAttributes.get(attribute.key);
                                if(existingAttributeValue.getType() == Type.STRING && existingAttributeValue.getStringValue().equals(attribute.value)) {
                                    return true;
                                } else {
                                    return false;
                                }
                            } else {
                                return true;
                            }
                        }
                    }
                }
            }

            return matchFound;
        }
        return false;
    }

    private boolean checkIncludes(SpanProcessorIncludeExclude includes, ReadableAttributes existingSpanAttributes, String spanName) {
        if(includes==null || includes.match_type==null) return true;
        else if(includes.match_type.equalsIgnoreCase("strict")) {
            if(includes.span_names!=null && includes.span_names.size()>0) {
                if(!includes.span_names.contains(spanName)) {
                    //did not find a match
                    return false;
                }
            }
            if(includes.attributes!=null && includes.attributes.size()>0) {
                for(SpanProcessorAttribute attribute:includes.attributes) {
                    if(attribute.key!=null) {
                        //found a match
                        if(existingSpanAttributes.get(attribute.key)!=null) {
                            if(attribute.value!=null) {
                                // found a match with value
                                AttributeValue existingAttributeValue=existingSpanAttributes.get(attribute.key);
                                if(existingAttributeValue.getType() == Type.STRING && existingAttributeValue.getStringValue().equals(attribute.value)) {
                                    return true;
                                }
                            } else {
                                return true;
                            }
                        }
                    }
                }
                //did not find a match
                return false;
            }

        } else if(includes.match_type.equalsIgnoreCase("regexp")) {

            if(includes.span_names!=null && includes.span_names.size()>0) {
                boolean foundMatch=false;
                for(String service:includes.span_names) {
                    if(spanName.matches(service)) {
                        foundMatch=true;
                        break;
                    }
                }
                // did not find a match
                if(!foundMatch) return false;
            }
            if(includes.attributes!=null && includes.attributes.size()>0) {
                for(SpanProcessorAttribute attribute:includes.attributes) {
                    if(attribute.key!=null) {
                        //found a match
                        if(existingSpanAttributes.get(attribute.key)!=null) {
                            if(attribute.value!=null) {
                                // found a match with value
                                AttributeValue existingAttributeValue=existingSpanAttributes.get(attribute.key);
                                if(existingAttributeValue.getType() == Type.STRING && existingAttributeValue.getStringValue().equals(attribute.value)) {
                                    return true;
                                }
                            } else {
                                return true;
                            }
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
