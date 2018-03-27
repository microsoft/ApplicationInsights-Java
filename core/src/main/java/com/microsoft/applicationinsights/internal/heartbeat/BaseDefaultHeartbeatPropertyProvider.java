package com.microsoft.applicationinsights.internal.heartbeat;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.util.PropertyHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class BaseDefaultHeartbeatPropertyProvider implements HeartBeatDefaultPayloadProviderInterface {

  private final List<String> defaultFields;

  private UUID uniqueProcessId;

  private final String name = "Base";

  public BaseDefaultHeartbeatPropertyProvider() {
    defaultFields = new ArrayList<>();
    initializeDefaultFields(defaultFields);
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public boolean isKeyword(String keyword) {
    return containsIgnoreCase(keyword, defaultFields);
  }

  @Override
  public Callable<Boolean> setDefaultPayload(final List<String> disableFields,
      final HeartBeatProviderInterface provider) {
    return new Callable<Boolean>() {

      volatile boolean hasSetValues = false;
      volatile List<String> enabledProperties = except(defaultFields, disableFields);
      @Override
      public Boolean call() {
        for (String fieldName : enabledProperties) {
          try {
            switch (fieldName) {
              case "jdkVersion":
                provider.addHeartBeatProperty(fieldName, getJdkVersion(), true);
                hasSetValues = true;
                break;
              case "sdk-version":
                provider.addHeartBeatProperty(fieldName, getSdkVersion(), true);
                hasSetValues = true;
                break;
              case "osType":
                provider.addHeartBeatProperty(fieldName, getOsType(), true);
                hasSetValues = true;
                break;
              case "processSessionId":
                provider.addHeartBeatProperty(fieldName, getProcessSessionId(), true);
                hasSetValues = true;
                break;
            }
          }
          catch (Exception e) {
           InternalLogger.INSTANCE.warn("Failed to obtain heartbeat property, stack trace"
               + "is: %s", ExceptionUtils.getStackTrace(e));
          }
        }
        return hasSetValues;
      }

      private List<String> except(List<String> list1, List<String> list2) {
        try {
          if (list1 == null || list2 == null) throw new IllegalArgumentException("Input is null");
          List<String> union = new ArrayList<>(list1);
          union.addAll(list2);
          List<String> intersection = new ArrayList<>(list1);
          intersection.retainAll(list2);
          union.removeAll(intersection);
          return union;
        }
        catch (Exception e) {
          InternalLogger.INSTANCE.warn("stack trace is %s", ExceptionUtils.getStackTrace(e));
        }
        finally{
          if (list1 != null) return list1;
          return list2;
        }
      }
    };
  }

  private void initializeDefaultFields(List<String> defaultFields) {

    if (defaultFields == null) {
      defaultFields = new ArrayList<>();
    }
    defaultFields.add("jdkVersion");
    defaultFields.add("sdk-version");
    defaultFields.add("osType");
    defaultFields.add("processSessionId");
  }

  private boolean containsIgnoreCase(String keyword, List<String> inputList) {
    try {
      if (keyword == null) throw new IllegalArgumentException("Keyword to compare is null");
      if (inputList == null) throw new IllegalArgumentException("List to compare is null");
      for (String key : inputList) {
        if (key.equalsIgnoreCase(keyword)) return true;
      }
      return false;
    }
    catch (Exception e) {
      InternalLogger.INSTANCE.warn("exception while comparision, stack trace is %s",
          ExceptionUtils.getStackTrace(e));
    }
    finally{
      //return true so we don't add property when comparison exception
      return true;
    }

  }

  private String getJdkVersion() {
    return System.getProperty("java.version");
  }

  private String getSdkVersion() {

    String sdkVersion = "java";

    Properties sdkVersionProps = PropertyHelper.getSdkVersionProperties();
    if (sdkVersionProps != null) {
      String version = sdkVersionProps.getProperty("version");
      sdkVersion = String.format("java:%s", version);
      return sdkVersion;
    }
    return sdkVersion + ":unknown-version";
  }


  private String getOsType() {
    return System.getProperty("os.name");
  }

  private String getProcessSessionId() {
    if (this.uniqueProcessId == null) {
      uniqueProcessId = UUID.randomUUID();
    }
    return uniqueProcessId.toString();
  }
}
