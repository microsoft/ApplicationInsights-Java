package com.microsoft.applicationinsights.internal.heartbeat;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * <h1>WebApp Heartbeat Property Provider</h1>
 * <p>
 *   This class is a concrete implementation of {@link HeartBeatPayloadProviderInterface}
 *   It enables setting Web-apps Metadata to heartbeat payload.
 * </p>
 *
 * @author Dhaval Doshi
 */
public class WebAppsHeartbeatProvider implements HeartBeatPayloadProviderInterface {

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
  private Map<String, String> environmentMap;

  private final String WEBSITE_SITE_NAME = "appSrv_SiteName";

  private final String WEBSITE_HOSTNAME = "appSrv_wsHost";

  private final String WEBSITE_HOME_STAMPNAME = "appSrv_wsStamp";


  /**
   * Constructor that initializes fields and load environment variables
   */
  public WebAppsHeartbeatProvider() {
    defaultFields = new HashSet<>();
    environmentMap = System.getenv();
    initializeDefaultFields(defaultFields);
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public boolean isKeyword(String keyword) {
    return defaultFields.contains(keyword);
  }

  @Override
  public Callable<Boolean> setDefaultPayload(final List<String> disableFields,
      final HeartBeatProviderInterface provider) {
    return new Callable<Boolean>() {

      Set<String> enabledProperties = MiscUtils.except(disableFields, defaultFields);
      @Override
      public Boolean call() {

        boolean hasSetValues = false;
        //update environment variable to account for
        updateEnvironmentVariableMap();
        for (String fieldName : enabledProperties) {
          try {
            switch (fieldName) {
              case WEBSITE_SITE_NAME:
                String webSiteName = getWebsiteSiteName();
                if (webSiteName == null) {
                  break;
                }
                provider.addHeartBeatProperty(fieldName, webSiteName, true);
                hasSetValues = true;
                break;
              case WEBSITE_HOSTNAME:
                String webSiteHostName = getWebsiteHostName();
                if (webSiteHostName == null) {
                  break;
                }
                provider.addHeartBeatProperty(fieldName, webSiteHostName, true);
                hasSetValues = true;
                break;
              case WEBSITE_HOME_STAMPNAME:
                String websiteHomeStampName = getWebsiteHomeStampName();
                if (websiteHomeStampName == null) {
                  break;
                }
                provider.addHeartBeatProperty(fieldName, websiteHomeStampName, true);
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

    defaultFields.add(WEBSITE_SITE_NAME);
    defaultFields.add(WEBSITE_HOSTNAME);
    defaultFields.add(WEBSITE_HOME_STAMPNAME);

  }

  /**
   * Returns the name of the website by reading environment variable
   * @return website name
   */
  private String getWebsiteSiteName() {
    return environmentMap.get("WEBSITE_SITE_NAME");
  }

  /**
   * Returns the website host name by reading environment variable
   * @return WebSite Host Name
   */

  private String getWebsiteHostName() {
    return environmentMap.get("WEBSITE_HOSTNAME");
  }

  /**
   * Returns the website home stamp name by reading environment variable
   * @return website stamp host name
   */
  private String getWebsiteHomeStampName() {
    return environmentMap.get("WEBSITE_HOME_STAMPNAME");
  }

  /**
   * This method updates the environment variable at every call to add
   * the payload, to cover hotswap scenarios.
   */
  private void updateEnvironmentVariableMap() {
    environmentMap = System.getenv();
  }
}
