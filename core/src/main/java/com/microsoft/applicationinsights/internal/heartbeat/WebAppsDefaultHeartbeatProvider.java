package com.microsoft.applicationinsights.internal.heartbeat;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class WebAppsDefaultHeartbeatProvider implements HeartBeatDefaultPayloadProviderInterface {

  private final String name = "webapps";

  private final Set<String> defaultFields;

  private final Map<String, String> environmentMap;

  public WebAppsDefaultHeartbeatProvider() {
    defaultFields = new HashSet<>();
    environmentMap = System.getenv();
    initializeDefaultFields(defaultFields);
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public Callable<Boolean> setDefaultPayload(final List<String> disableFields,
      final HeartBeatProviderInterface provider) {
    return new Callable<Boolean>() {
      volatile boolean hasSetValues = false;
      volatile Set<String> enabledProperties = MiscUtils.except(defaultFields, disableFields);
      @Override
      public Boolean call() {
        for (String fieldName : enabledProperties) {
          try {
            switch (fieldName) {
              case "website_site_name":
                String webSiteName = getWebsiteSiteName();
                if (webSiteName == null) {
                  InternalLogger.INSTANCE.trace("Web site name not available, probably not a web app");
                  break;
                }
                provider.addHeartBeatProperty(fieldName, webSiteName, true);
                hasSetValues = true;
                break;
              case "website_home_name":
                String webSiteHostName = getWebsiteHostName();
                if (webSiteHostName == null) {
                  InternalLogger.INSTANCE.trace("web site host name not available, probably not a web app");
                  break;
                }
                provider.addHeartBeatProperty(fieldName, webSiteHostName, true);
                hasSetValues = true;
                break;
              default:
                InternalLogger.INSTANCE.trace("Unknown web apps property encountered");
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
    };
  }

  private void initializeDefaultFields(Set<String> defaultFields) {
    if (defaultFields == null) {
      defaultFields = new HashSet<>();
    }

    defaultFields.add("website_site_name");
    defaultFields.add("website_home_name");

  }

  private String getWebsiteSiteName() {
    if (environmentMap.containsKey("WEBSITE_SITE_NAME")) {
      return environmentMap.get("WEBSITE_SITE_NAME");
    }
    return null;
  }

  private String getWebsiteHostName() {
    if (environmentMap.containsKey("WEBSITE_HOME_STAMPNAME")) {
      return environmentMap.get("WEBSITE_HOME_STAMPNAME");
    }
    return null;
  }
}
