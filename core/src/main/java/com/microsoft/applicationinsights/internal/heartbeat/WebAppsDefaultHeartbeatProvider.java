package com.microsoft.applicationinsights.internal.heartbeat;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * <h1>WebApp Heartbeat Property Provider</h1>
 * <p>
 *   This class is a concrete implementation of {@link com.microsoft.applicationinsights.internal.heartbeat.HeartBeatDefaultPayloadProviderInterface}
 *   It enables setting Web-apps Metadata to heartbeat payload.
 * </p>
 *
 * @author Dhaval Doshi
 * @since 03-30-2018
 */
public class WebAppsDefaultHeartbeatProvider implements HeartBeatDefaultPayloadProviderInterface {

  /**
   * Name of the provider
   */
  private final String name = "webapps";

  /**
   * Collection holding default properties for this default provider.
   */
  private final Set<String> defaultFields;

  /**
   * Map for storing environment variables
   */
  private final Map<String, String> environmentMap;

  /**
   * Constructor that initializes fields and load environment variables
   */
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
  public boolean isKeyWord(String keyword) {
    return defaultFields.contains(keyword);
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

  /**
   * Populates the default Fields with the properties
   * @param defaultFields
   */
  private void initializeDefaultFields(Set<String> defaultFields) {
    if (defaultFields == null) {
      defaultFields = new HashSet<>();
    }

    defaultFields.add("website_site_name");
    defaultFields.add("website_home_name");

  }

  /**
   * Returns the name of the website by reading environment variable
   * @return website name
   */
  private String getWebsiteSiteName() {
    if (environmentMap.containsKey("WEBSITE_SITE_NAME")) {
      return environmentMap.get("WEBSITE_SITE_NAME");
    }
    return null;
  }

  /**
   * Returns the website home stamp name by reading environment variable
   * @return website stamp host name
   */
  private String getWebsiteHostName() {
    if (environmentMap.containsKey("WEBSITE_HOME_STAMPNAME")) {
      return environmentMap.get("WEBSITE_HOME_STAMPNAME");
    }
    return null;
  }
}
