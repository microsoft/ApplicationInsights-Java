// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.configuration;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsHelper;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.status.StatusFile;
import com.microsoft.applicationinsights.agent.internal.common.FriendlyException;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Severity;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.annotation.Nullable;

// an assumption is made throughout this file that user will not explicitly use `null` value in json
// file
// TODO how to pre-process or generally be robust in the face of explicit `null` value usage?
public class Configuration {

  public String connectionString;
  // missing connection string will cause the agent to not start up, unless this is set,
  // in which case the agent will start, but it won't begin capturing any telemetry until the
  // connection string is configured later on
  public boolean connectionStringConfiguredAtRuntime =
      ConfigurationBuilder.inAzureFunctionsConsumptionWorker();
  public Role role = new Role();
  public Map<String, String> customDimensions = new HashMap<>();
  public Sampling sampling = new Sampling();
  public List<JmxMetric> jmxMetrics = new ArrayList<>();
  public Instrumentation instrumentation = new Instrumentation();
  public Heartbeat heartbeat = new Heartbeat();
  public Proxy proxy = new Proxy();
  public SelfDiagnostics selfDiagnostics = new SelfDiagnostics();
  // applies to perf counters, default custom metrics, jmx metrics, and micrometer metrics
  // not sure if we'll be able to have different metric intervals in future OpenTelemetry metrics
  // world, so safer to only allow single interval for now
  public int metricIntervalSeconds = 60;
  public PreviewConfiguration preview = new PreviewConfiguration();
  public InternalConfiguration internal = new InternalConfiguration();

  // this is just here to detect if using old format in order to give a helpful error message
  public Map<String, Object> instrumentationSettings;

  private static boolean isEmpty(@Nullable String str) {
    return str == null || str.trim().isEmpty();
  }

  public void validate() {
    instrumentation.logging.getSeverityThreshold();
    preview.validate();
  }

  @Deprecated
  public enum SpanKind {
    @JsonProperty("server")
    SERVER(io.opentelemetry.api.trace.SpanKind.SERVER),
    @JsonProperty("client")
    CLIENT(io.opentelemetry.api.trace.SpanKind.CLIENT),
    @JsonProperty("consumer")
    CONSUMER(io.opentelemetry.api.trace.SpanKind.CONSUMER),
    @JsonProperty("producer")
    PRODUCER(io.opentelemetry.api.trace.SpanKind.PRODUCER),
    @JsonProperty("internal")
    INTERNAL(io.opentelemetry.api.trace.SpanKind.INTERNAL);

    public final io.opentelemetry.api.trace.SpanKind otelSpanKind;

    SpanKind(io.opentelemetry.api.trace.SpanKind otelSpanKind) {
      this.otelSpanKind = otelSpanKind;
    }
  }

  public enum SamplingTelemetryType {
    // restricted to telemetry types that are supported by SamplingOverrides
    @JsonProperty("request")
    REQUEST,
    @JsonProperty("dependency")
    DEPENDENCY,
    @JsonProperty("trace")
    TRACE,
    @JsonProperty("exception")
    EXCEPTION
  }

  public enum MatchType {
    @JsonProperty("strict")
    STRICT,
    @JsonProperty("regexp")
    REGEXP
  }

  public enum ProcessorActionType {
    @JsonProperty("insert")
    INSERT,
    @JsonProperty("update")
    UPDATE,
    @JsonProperty("delete")
    DELETE,
    @JsonProperty("hash")
    HASH,
    @JsonProperty("extract")
    EXTRACT,
    @JsonProperty("mask")
    MASK
  }

  public enum ProcessorType {
    @JsonProperty("attribute")
    ATTRIBUTE("an attribute"),
    @JsonProperty("log")
    LOG("a log"),
    @JsonProperty("span")
    SPAN("a span"),
    @JsonProperty("metric-filter")
    METRIC_FILTER("a metric-filter");

    private final String anX;

    ProcessorType(String anX) {
      this.anX = anX;
    }
  }

  private enum IncludeExclude {
    INCLUDE,
    EXCLUDE;

    @Override
    public String toString() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  public static class Role {

    public String name;
    public String instance;
  }

  public static class Sampling {

    // fixed percentage of requests
    @Nullable public Double percentage;

    // default is 5 requests per second (set in ConfigurationBuilder if neither percentage nor
    // requestsPerSecond was configured)
    @Nullable public Double requestsPerSecond;

    // this config option only existed in one BETA release (3.4.0-BETA)
    @Deprecated @Nullable public Double limitPerSecond;
  }

  public static class SamplingPreview {

    // this is not the default for now at least, because
    //
    // parent not-sampled -> child not-sampled (always, to avoid broken traces)
    // parent sampled -> child will still sample at its desired rate
    //                   note: this is just sample rate of child, not sample rate of parent times
    //                         sample rate of child, as both are using the same trace-id hash
    //
    // ??? if child sample rate is higher than the parent sample rate
    //     parent sampled --> child sampled
    //     parent not-sampled --> child not-sampled
    //     which means that child has same effective sample rate as parent, and so its item count
    //           will be wrong
    //
    // AND SO: if want to use parent-based sampler, then need to propagate the sample rate,
    //         otherwise can end up with incorrect math
    //
    // Another (lesser) reason is because .NET SDK always propagates trace flags "00" (not
    // sampled)
    //
    // future goal: make parentBased sampling the default if item count is received via tracestate
    public boolean parentBased;

    public List<SamplingOverride> overrides = new ArrayList<>();
  }

  public static class JmxMetric {

    public String name;
    public String objectName;
    public String attribute;
  }

  public static class Instrumentation {

    public EnabledByDefaultInstrumentation azureSdk = new EnabledByDefaultInstrumentation();
    public EnabledByDefaultInstrumentation cassandra = new EnabledByDefaultInstrumentation();
    public DatabaseInstrumentationWithMasking jdbc = new DatabaseInstrumentationWithMasking();
    public EnabledByDefaultInstrumentation jms = new EnabledByDefaultInstrumentation();
    public EnabledByDefaultInstrumentation kafka = new EnabledByDefaultInstrumentation();
    public LoggingInstrumentation logging = new LoggingInstrumentation();
    public MicrometerInstrumentation micrometer = new MicrometerInstrumentation();
    public DatabaseInstrumentationWithMasking mongo = new DatabaseInstrumentationWithMasking();
    public EnabledByDefaultInstrumentation quartz = new EnabledByDefaultInstrumentation();
    public EnabledByDefaultInstrumentation rabbitmq = new EnabledByDefaultInstrumentation();
    public EnabledByDefaultInstrumentation redis = new EnabledByDefaultInstrumentation();
    public EnabledByDefaultInstrumentation springScheduling = new EnabledByDefaultInstrumentation();
  }

  public static class DatabaseInstrumentationWithMasking {
    public boolean enabled = true;
    public DatabaseMaskingConfiguration masking = new DatabaseMaskingConfiguration();
  }

  public static class DatabaseMaskingConfiguration {
    public boolean enabled = true;
  }

  public static class LoggingInstrumentation {
    public String level = "INFO";

    public int getSeverityThreshold() {
      return getSeverityThreshold(level);
    }

    public static int getSeverityThreshold(String level) {
      switch (level.toUpperCase(Locale.ROOT)) {
        case "OFF":
          return Integer.MAX_VALUE;
        case "FATAL":
          return Severity.FATAL.getSeverityNumber();
        case "ERROR":
        case "SEVERE":
          return Severity.ERROR.getSeverityNumber();
        case "WARN":
        case "WARNING":
          return Severity.WARN.getSeverityNumber();
        case "INFO":
          return Severity.INFO.getSeverityNumber();
        case "CONFIG":
        case "DEBUG":
        case "FINE":
        case "FINER":
          return Severity.DEBUG.getSeverityNumber();
        case "TRACE":
        case "FINEST":
        case "ALL":
          return Severity.TRACE.getSeverityNumber();
        default:
          throw new FriendlyException(
              "Invalid logging instrumentation level: " + level, "Please provide a valid level.");
      }
    }
  }

  public static class MicrometerInstrumentation {
    public boolean enabled = true;
    // this is just here to detect if using this old undocumented setting in order to give a helpful
    // error message
    @Deprecated public int reportingIntervalSeconds = 60;
  }

  public static class Heartbeat {
    public long intervalSeconds = MINUTES.toSeconds(15);
  }

  public static class Statsbeat {
    // disabledAll is used internally as an emergency kill-switch to turn off Statsbeat completely
    // when something goes wrong.
    public boolean disabledAll = false;

    public String instrumentationKey;
    public String endpoint;
    public long shortIntervalSeconds = MINUTES.toSeconds(15); // default to 15 minutes
    public long longIntervalSeconds = DAYS.toSeconds(1); // default to daily
  }

  public static class PreAggregatedStandardMetricsConfiguration {
    public boolean enabled = true; // pre-aggregated standard metrics are on by default
  }

  public static class Proxy {

    public String host;
    public int port = 80;
    // password in json file is not secure, use APPLICATIONINSIGHTS_PROXY
    public String username;
    public String password;
  }

  public static class PreviewConfiguration {

    public SamplingPreview sampling = new SamplingPreview();
    public List<ProcessorConfig> processors = new ArrayList<>();
    // this is just here to detect if using this old setting in order to give a helpful message
    @Deprecated public boolean openTelemetryApiSupport;
    public PreviewInstrumentation instrumentation = new PreviewInstrumentation();
    // these are just here to detect if using this old setting in order to give a helpful message
    @Deprecated public int metricIntervalSeconds = 60;
    @Deprecated public Boolean ignoreRemoteParentNotSampled;
    public boolean captureControllerSpans;
    // this is just here to detect if using this old setting in order to give a helpful message
    @Deprecated public boolean httpMethodInOperationName;
    public LiveMetrics liveMetrics = new LiveMetrics();
    public LegacyRequestIdPropagation legacyRequestIdPropagation = new LegacyRequestIdPropagation();
    // this is needed to unblock customer, but is not the ideal long-term solution
    // https://portal.microsofticm.com/imp/v3/incidents/details/266992200/home
    public boolean disablePropagation;
    public boolean captureHttpServer4xxAsError = true;

    // LoggingLevel is no longer sent by default since 3.3.0, since the data is already available
    // under SeverityLevel. This configuration is provided as a temporary measure for customers
    // who are unable to update their alerts/dashboards at the same time that they are updating
    // their Javaagent version
    // Note: this configuration option will be removed in 4.0.0
    public boolean captureLoggingLevelAsCustomDimension;

    public boolean captureLogbackCodeAttributes;

    public boolean captureLogbackMarker;

    public boolean captureLog4jMarker;

    // this is to support interoperability with other systems
    // intentionally not allowing the removal of w3c propagator since that is key to many Azure
    // integrated experiences
    public List<String> additionalPropagators = new ArrayList<>();

    public List<InheritedAttribute> inheritedAttributes = new ArrayList<>();

    public HttpHeadersConfiguration captureHttpServerHeaders = new HttpHeadersConfiguration();
    public HttpHeadersConfiguration captureHttpClientHeaders = new HttpHeadersConfiguration();

    public ProfilerConfiguration profiler = new ProfilerConfiguration();
    public GcEventConfiguration gcEvents = new GcEventConfiguration();
    public AadAuthentication authentication = new AadAuthentication();
    public PreviewStatsbeat statsbeat = new PreviewStatsbeat();

    public List<ConnectionStringOverride> connectionStringOverrides = new ArrayList<>();
    public List<RoleNameOverride> roleNameOverrides = new ArrayList<>();

    @Deprecated
    public List<InstrumentationKeyOverride> instrumentationKeyOverrides = new ArrayList<>();

    public int generalExportQueueCapacity = 2048;
    // metrics get flooded every 60 seconds by default, so need larger queue size to avoid dropping
    // telemetry (they are much smaller so a larger queue size is ok)
    public int metricsExportQueueCapacity = 65536;

    // disk persistence has a default capacity of 50MB
    public int diskPersistenceMaxSizeMb = 50;

    // unfortunately the Java SDK behavior has always been to report the "% Processor Time" number
    // as "normalized" (divided by # of CPU cores), even though it should be non-normalized
    // we cannot change this existing behavior as it would break existing customers' alerts, but at
    // least this configuration gives users a way to opt in to the correct behavior
    //
    // note: the normalized value is now separately reported under a different metric
    // "% Processor Time Normalized"
    public boolean useNormalizedValueForNonNormalizedCpuPercentage = true;

    public List<CustomInstrumentation> customInstrumentation = new ArrayList<>();

    private static final Set<String> VALID_ADDITIONAL_PROPAGATORS =
        new HashSet<>(asList("b3", "b3multi"));

    public void validate() {
      for (SamplingOverride samplingOverride : sampling.overrides) {
        samplingOverride.validate();
      }
      for (Configuration.ConnectionStringOverride connectionStringOverride :
          connectionStringOverrides) {
        connectionStringOverride.validate();
      }
      for (Configuration.RoleNameOverride roleNameOverride : roleNameOverrides) {
        roleNameOverride.validate();
      }
      for (Configuration.InstrumentationKeyOverride instrumentationKeyOverride :
          instrumentationKeyOverrides) {
        instrumentationKeyOverride.validate();
      }
      for (ProcessorConfig processorConfig : processors) {
        processorConfig.validate();
      }
      authentication.validate();

      for (String additionalPropagator : additionalPropagators) {
        if (!VALID_ADDITIONAL_PROPAGATORS.contains(additionalPropagator)) {
          throw new FriendlyException(
              "The \"additionalPropagators\" configuration contains an invalid entry: "
                  + additionalPropagator,
              "Please provide only valid values for \"additionalPropagators\" configuration.");
        }
      }
    }
  }

  public static class InheritedAttribute {
    public String key;
    public AttributeType type;

    public AttributeKey<?> getAttributeKey() {
      switch (type) {
        case STRING:
          return AttributeKey.stringKey(key);
        case BOOLEAN:
          return AttributeKey.booleanKey(key);
        case LONG:
          return AttributeKey.longKey(key);
        case DOUBLE:
          return AttributeKey.doubleKey(key);
        case STRING_ARRAY:
          return AttributeKey.stringArrayKey(key);
        case BOOLEAN_ARRAY:
          return AttributeKey.booleanArrayKey(key);
        case LONG_ARRAY:
          return AttributeKey.longArrayKey(key);
        case DOUBLE_ARRAY:
          return AttributeKey.doubleArrayKey(key);
      }
      throw new IllegalStateException("Unexpected attribute key type: " + type);
    }
  }

  public static class HttpHeadersConfiguration {
    public List<String> requestHeaders = new ArrayList<>();
    public List<String> responseHeaders = new ArrayList<>();
  }

  public enum AttributeType {
    @JsonProperty("string")
    STRING,
    @JsonProperty("boolean")
    BOOLEAN,
    @JsonProperty("long")
    LONG,
    @JsonProperty("double")
    DOUBLE,
    @JsonProperty("string-array")
    STRING_ARRAY,
    @JsonProperty("boolean-array")
    BOOLEAN_ARRAY,
    @JsonProperty("long-array")
    LONG_ARRAY,
    @JsonProperty("double-array")
    DOUBLE_ARRAY
  }

  public static class LegacyRequestIdPropagation {
    public boolean enabled;
  }

  public static class InternalConfiguration {
    // This is used for collecting internal stats
    public Statsbeat statsbeat = new Statsbeat();
    public PreAggregatedStandardMetricsConfiguration preAggregatedStandardMetrics =
        new PreAggregatedStandardMetricsConfiguration();
  }

  public static class PreviewInstrumentation {

    public DisabledByDefaultInstrumentation play = new DisabledByDefaultInstrumentation();

    public DisabledByDefaultInstrumentation akka = new DisabledByDefaultInstrumentation();

    public DisabledByDefaultInstrumentation apacheCamel = new DisabledByDefaultInstrumentation();

    // this is just here to detect if using this old setting in order to give a helpful message
    @Deprecated
    public DisabledByDefaultInstrumentation azureSdk = new DisabledByDefaultInstrumentation();

    public DisabledByDefaultInstrumentation grizzly = new DisabledByDefaultInstrumentation();

    // this is just here to detect if using this old setting in order to give a helpful message
    @Deprecated
    public DisabledByDefaultInstrumentation javaHttpClient = new DisabledByDefaultInstrumentation();

    // this is just here to detect if using this old setting in order to give a helpful message
    @Deprecated
    public DisabledByDefaultInstrumentation jaxws = new DisabledByDefaultInstrumentation();

    // this is just here to detect if using this old setting in order to give a helpful message
    @Deprecated
    public DisabledByDefaultInstrumentation quartz = new DisabledByDefaultInstrumentation();

    // this is just here to detect if using this old setting in order to give a helpful message
    @Deprecated
    public DisabledByDefaultInstrumentation rabbitmq = new DisabledByDefaultInstrumentation();

    public DisabledByDefaultInstrumentation springIntegration =
        new DisabledByDefaultInstrumentation();

    public DisabledByDefaultInstrumentation vertx = new DisabledByDefaultInstrumentation();

    // this is opt-in because it can cause startup slowness due to expensive matchers
    public DisabledByDefaultInstrumentation jaxrsAnnotations =
        new DisabledByDefaultInstrumentation();
  }

  public static class PreviewStatsbeat {
    // disabled is used by customer to turn off non-essential Statsbeat, e.g. disk persistence
    // operation status, optional network statsbeat, other endpoints except Breeze, etc.
    public boolean disabled = false;
  }

  public static class ConnectionStringOverride {
    public String httpPathPrefix;
    public String connectionString;

    public void validate() {
      if (httpPathPrefix == null) {
        // TODO add doc and go link, similar to telemetry processors
        throw new FriendlyException(
            "A connection string override configuration is missing an \"httpPathPrefix\".",
            "Please provide an \"httpPathPrefix\" for the connection string override configuration.");
      }
      if (connectionString == null) {
        // TODO add doc and go link, similar to telemetry processors
        throw new FriendlyException(
            "A connection string override configuration is missing an \"connectionString\".",
            "Please provide an \"instrumentationKey\" for the connection string override configuration.");
      }
    }
  }

  public static class RoleNameOverride {
    public String httpPathPrefix;
    public String roleName;

    public void validate() {
      if (httpPathPrefix == null) {
        // TODO add doc and go link, similar to telemetry processors
        throw new FriendlyException(
            "A role name override configuration is missing an \"httpPathPrefix\".",
            "Please provide an \"httpPathPrefix\" for the role name override configuration.");
      }
      if (roleName == null) {
        // TODO add doc and go link, similar to telemetry processors
        throw new FriendlyException(
            "An role name override configuration is missing a \"roleName\".",
            "Please provide a \"roleName\" for the role name override configuration.");
      }
    }
  }

  @Deprecated
  public static class InstrumentationKeyOverride {
    public String httpPathPrefix;
    public String instrumentationKey;

    public void validate() {
      if (httpPathPrefix == null) {
        // TODO add doc and go link, similar to telemetry processors
        throw new FriendlyException(
            "An instrumentation key override configuration is missing an \"httpPathPrefix\".",
            "Please provide an \"httpPathPrefix\" for the instrumentation key override configuration.");
      }
      if (instrumentationKey == null) {
        // TODO add doc and go link, similar to telemetry processors
        throw new FriendlyException(
            "An instrumentation key override configuration is missing an \"instrumentationKey\".",
            "Please provide an \"instrumentationKey\" for the instrumentation key override configuration.");
      }
    }
  }

  public static class CustomInstrumentation {
    public String className;
    public String methodName;
  }

  public static class EnabledByDefaultInstrumentation {
    public boolean enabled = true;
  }

  public static class DisabledByDefaultInstrumentation {
    public boolean enabled;
  }

  public static class LiveMetrics {
    public boolean enabled = true;
  }

  public static class SelfDiagnostics {

    public String level = "info";
    public String destination = "file+console";
    public DestinationFile file = new DestinationFile();
  }

  public static class DestinationFile {

    private static final String DEFAULT_NAME = "applicationinsights.log";

    public String path = getDefaultPath();
    public int maxSizeMb = 5;
    public int maxHistory = 1;

    private static String getDefaultPath() {
      if (!DiagnosticsHelper.isRpIntegration()) {
        if (isRuntimeAttached()) { // With runtime attachment, the agent jar is located in a temp
          // folder that is dropped when the JVM shuts down
          String userDir = System.getProperty("user.dir");
          return userDir + File.separator + DEFAULT_NAME;
        }
        return DEFAULT_NAME; // this will be relative to the directory where agent jar is located
      }
      if (DiagnosticsHelper.useAppSvcRpIntegrationLogging()
          || DiagnosticsHelper.useFunctionsRpIntegrationLogging()) {
        return StatusFile.getLogDir() + "/" + DEFAULT_NAME;
      }
      // azure spring cloud
      return DEFAULT_NAME;
    }
  }

  private static boolean isRuntimeAttached() {
    return Boolean.getBoolean("applicationinsights.internal.runtime.attached");
  }

  public static class SamplingOverride {
    @Deprecated @Nullable public SpanKind spanKind;

    // TODO (trask) make this required when moving out of preview
    //   for now the default is both "request" and "dependency" for backwards compatibility
    @Nullable public SamplingTelemetryType telemetryType;

    // this config option existed in one GA release (3.4.0), and was then replaced by telemetryType
    @Deprecated @Nullable public SamplingTelemetryType telemetryKind;

    // this config option only existed in one BETA release (3.4.0-BETA)
    @Deprecated @Nullable public Boolean includingStandaloneTelemetry;

    // not using include/exclude, because you can still get exclude with this by adding a second
    // (exclude) override above it
    // (since only the first matching override is used)
    public List<SamplingOverrideAttribute> attributes = new ArrayList<>();
    public Double percentage;
    public String id; // optional, used for debugging purposes only

    public boolean isForRequestTelemetry() {
      return telemetryType == SamplingTelemetryType.REQUEST
          // this part is for backwards compatibility:
          || (telemetryType == null && spanKind != SpanKind.CLIENT);
    }

    public boolean isForDependencyTelemetry() {
      return telemetryType == SamplingTelemetryType.DEPENDENCY
          // this part is for backwards compatibility:
          || (telemetryType == null && spanKind != SpanKind.SERVER);
    }

    public void validate() {
      if (percentage == null) {
        // TODO add doc and go link, similar to telemetry processors
        throw new FriendlyException(
            "A sampling override configuration is missing a \"percentage\".",
            "Please provide a \"percentage\" for the sampling override configuration.");
      }
      if (percentage < 0 || percentage > 100) {
        // TODO add doc and go link, similar to telemetry processors
        throw new FriendlyException(
            "A sampling override configuration has a \"percentage\" that is not between 0 and 100.",
            "Please provide a \"percentage\" that is between 0 and 100 for the sampling override configuration.");
      }
      for (SamplingOverrideAttribute attribute : attributes) {
        attribute.validate();
      }
    }
  }

  public static class SamplingOverrideAttribute {
    public String key;
    @Nullable public String value;
    @Nullable public MatchType matchType;

    private void validate() {
      if (isEmpty(key)) {
        // TODO add doc and go link, similar to telemetry processors
        throw new FriendlyException(
            "A sampling override configuration has an attribute section that is missing a \"key\".",
            "Please provide a \"key\" under the attribute section of the sampling override configuration.");
      }
      if (matchType == null && value != null) {
        throw new FriendlyException(
            "A sampling override configuration has an attribute section with a \"value\" that is missing a \"matchType\".",
            "Please provide a \"matchType\" under the attribute section of the sampling override configuration.");
      }
      if (matchType == MatchType.REGEXP) {
        if (isEmpty(value)) {
          // TODO add doc and go link, similar to telemetry processors
          throw new FriendlyException(
              "Asampling override configuration has an attribute with matchType regexp that is missing a \"value\".",
              "Please provide a key under the attribute section of the sampling override configuration.");
        }
        validateRegex(value);
      }
    }

    private static void validateRegex(String value) {
      try {
        Pattern.compile(value);
      } catch (PatternSyntaxException e) {
        // TODO add doc and go link, similar to telemetry processors
        throw new FriendlyException(
            "A telemetry filter configuration has an invalid regex: " + value,
            "Please provide a valid regex in the telemetry filter configuration.",
            e);
      }
    }
  }

  public static class ProcessorConfig {
    public ProcessorType type;
    public ProcessorIncludeExclude include;
    public ProcessorIncludeExclude exclude;
    public List<ProcessorAction> actions =
        new ArrayList<>(); // specific for processor type "attributes"
    public NameConfig name; // specific for processor type "span"
    public NameConfig body; // specific for processor types "log"
    public String id; // optional, used for debugging purposes only

    public void validate() {
      if (type == null) {
        throw new FriendlyException(
            "A telemetry processor configuration is missing a \"type\".",
            "Please provide a \"type\" in the telemetry processor configuration. "
                + "Learn more about telemetry processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
      }
      if (include != null) {
        include.validate(type, IncludeExclude.INCLUDE);
      }
      if (exclude != null) {
        exclude.validate(type, IncludeExclude.EXCLUDE);
      }
      switch (type) {
        case ATTRIBUTE:
          validateAttributeProcessorConfig();
          return;
        case SPAN:
          validateSpanProcessorConfig();
          return;
        case LOG:
          validateLogProcessorConfig();
          return;
        case METRIC_FILTER:
          validateMetricFilterProcessorConfig();
          return;
      }
      throw new AssertionError("Unexpected processor type: " + type);
    }

    public void validateAttributeProcessorConfig() {
      if (actions.isEmpty()) {
        throw new FriendlyException(
            "An attribute processor configuration has no actions.",
            "Please provide at least one action in the attribute processor configuration. "
                + "Learn more about attribute processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
      }
      for (ProcessorAction action : actions) {
        action.validate();
      }

      validateSectionIsNull(name, "name");
      validateSectionIsNull(body, "body");
    }

    public void validateSpanProcessorConfig() {
      if (name == null) {
        throw new FriendlyException(
            "a span processor configuration is missing a \"name\" section.",
            "Please provide a \"name\" section in the span processor configuration. "
                + "Learn more about span processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
      }
      name.validate(type);

      validateActionsIsEmpty();
      validateSectionIsNull(body, "body");
    }

    public void validateLogProcessorConfig() {
      if (body == null) {
        throw new FriendlyException(
            "a log processor configuration is missing a \"body\" section.",
            "Please provide a \"body\" section in the log processor configuration. "
                + "Learn more about log processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
      }
      body.validate(type);

      validateActionsIsEmpty();
      validateSectionIsNull(name, "name");
    }

    public void validateMetricFilterProcessorConfig() {
      if (exclude == null) {
        throw new FriendlyException(
            "a metric-filter processor configuration is missing an \"exclude\" section.",
            "Please provide a \"exclude\" section in the metric-filter processor configuration. "
                + "Learn more about metric-filter processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
      }

      validateActionsIsEmpty();
      validateSectionIsNull(name, "name");
      validateSectionIsNull(body, "body");
    }

    private void validateActionsIsEmpty() {
      if (!actions.isEmpty()) {
        throwUnexpectedSectionFriendlyException("actions");
      }
    }

    private void validateSectionIsNull(@Nullable Object section, String sectionName) {
      if (section != null) {
        throwUnexpectedSectionFriendlyException(sectionName);
      }
    }

    private void throwUnexpectedSectionFriendlyException(String sectionName) {
      throw new FriendlyException(
          type.anX + " processor configuration has an unexpected section \"" + sectionName + "\".",
          "Please do not provide a \""
              + sectionName
              + "\" section in the "
              + type
              + " processor configuration. "
              + "Learn more about "
              + type
              + " processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
    }
  }

  public static class NameConfig {
    public List<String> fromAttributes = new ArrayList<>();
    public ToAttributeConfig toAttributes;
    public String separator;

    public void validate(ProcessorType processorType) {
      if (fromAttributes.isEmpty() && toAttributes == null) {
        // TODO different links for different processor types?
        throw new FriendlyException(
            processorType.anX
                + " processor configuration has \"name\" action with no \"fromAttributes\" and no \"toAttributes\".",
            "Please provide at least one of \"fromAttributes\" or \"toAttributes\" under the name section of the "
                + processorType
                + " processor configuration. "
                + "Learn more about "
                + processorType
                + " processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
      }
      if (toAttributes != null) {
        toAttributes.validate(processorType);
      }
    }
  }

  public static class ToAttributeConfig {
    public List<String> rules = new ArrayList<>();

    public void validate(ProcessorType processorType) {
      if (rules.isEmpty()) {
        throw new FriendlyException(
            processorType.anX
                + " processor configuration has \"toAttributes\" section with no \"rules\".",
            "Please provide at least one rule under the \"toAttributes\" section of the "
                + processorType
                + " processor configuration. "
                + "Learn more about "
                + processorType
                + " processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
      }
      for (String rule : rules) {
        validateRegex(rule, processorType);
      }
    }
  }

  public static class ProcessorIncludeExclude {
    public MatchType matchType;
    public List<String> spanNames = new ArrayList<>();
    public List<String> logBodies = new ArrayList<>();
    public List<String> metricNames = new ArrayList<>();
    public List<ProcessorAttribute> attributes = new ArrayList<>();

    public void validate(ProcessorType processorType, IncludeExclude includeExclude) {
      if (matchType == null) {
        throw new FriendlyException(
            processorType.anX
                + " processor configuration has an "
                + includeExclude
                + " section that is missing a \"matchType\".",
            "Please provide a \"matchType\" under the "
                + includeExclude
                + " section of the "
                + processorType
                + " processor configuration. "
                + "Learn more about "
                + processorType
                + " processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
      }
      for (ProcessorAttribute attribute : attributes) {
        if (isEmpty(attribute.key)) {
          throw new FriendlyException(
              processorType.anX
                  + " processor configuration has an "
                  + includeExclude
                  + " section that is missing a \"key\".",
              "Please provide a \"key\" under the "
                  + includeExclude
                  + " section of the "
                  + processorType
                  + " processor configuration. "
                  + "Learn more about "
                  + processorType
                  + " processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
        }
        if (matchType == MatchType.REGEXP && attribute.value != null) {
          validateRegex(attribute.value, processorType);
        }
      }

      switch (processorType) {
        case ATTRIBUTE:
          validAttributeProcessorIncludeExclude(includeExclude);
          return;
        case LOG:
          validateLogProcessorIncludeExclude(includeExclude);
          return;
        case SPAN:
          validateSpanProcessorIncludeExclude(includeExclude);
          return;
        case METRIC_FILTER:
          validateMetricFilterProcessorExclude(includeExclude);
          return;
      }
      throw new IllegalStateException("Unexpected processor type: " + processorType);
    }

    private void validAttributeProcessorIncludeExclude(IncludeExclude includeExclude) {
      if (attributes.isEmpty() && spanNames.isEmpty() && logBodies.isEmpty()) {
        throw new FriendlyException(
            "An attribute processor configuration has an "
                + includeExclude
                + " section with no \"spanNames\" and no \"attributes\" and no \"logBodies\".",
            "Please provide at least one of \"spanNames\" or \"attributes\" or \"logBodies\" under the "
                + includeExclude
                + " section of the attribute processor configuration. "
                + "Learn more about attribute processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
      }
      if (matchType == MatchType.REGEXP) {
        for (String spanName : spanNames) {
          validateRegex(spanName, ProcessorType.ATTRIBUTE);
        }

        for (String logBody : logBodies) {
          validateRegex(logBody, ProcessorType.ATTRIBUTE);
        }
      }

      validateSectionIsEmpty(metricNames, ProcessorType.ATTRIBUTE, includeExclude, "metricNames");
    }

    private void validateLogProcessorIncludeExclude(IncludeExclude includeExclude) {
      if (logBodies.isEmpty() && attributes.isEmpty()) {
        throw new FriendlyException(
            "A log processor configuration has an "
                + includeExclude
                + " section with no \"attributes\" and no \"logBodies\".",
            "Please provide \"attributes\" or \"logBodies\" under the "
                + includeExclude
                + " section of the log processor configuration. "
                + "Learn more about log processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
      }

      if (matchType == MatchType.REGEXP) {
        for (String logBody : logBodies) {
          validateRegex(logBody, ProcessorType.LOG);
        }
      }

      validateSectionIsEmpty(spanNames, ProcessorType.LOG, includeExclude, "spanNames");
      validateSectionIsEmpty(metricNames, ProcessorType.LOG, includeExclude, "metricNames");
    }

    private void validateSpanProcessorIncludeExclude(IncludeExclude includeExclude) {
      if (spanNames.isEmpty() && attributes.isEmpty()) {
        throw new FriendlyException(
            "A span processor configuration has "
                + includeExclude
                + " section with no \"spanNames\" and no \"attributes\".",
            "Please provide at least one of \"spanNames\" or \"attributes\" under the "
                + includeExclude
                + " section of the span processor configuration. "
                + "Learn more about span processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
      }
      if (matchType == MatchType.REGEXP) {
        for (String spanName : spanNames) {
          validateRegex(spanName, ProcessorType.SPAN);
        }
      }

      validateSectionIsEmpty(logBodies, ProcessorType.SPAN, includeExclude, "logBodies");
      validateSectionIsEmpty(metricNames, ProcessorType.SPAN, includeExclude, "metricNames");
    }

    private void validateMetricFilterProcessorExclude(IncludeExclude includeExclude) {
      if (includeExclude == IncludeExclude.INCLUDE) {
        throw new FriendlyException(
            "A metric-filter processor configuration has an include section.",
            "Please do not provide an \"include\" section in the metric-filter processor configuration. "
                + "Learn more about span processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
      }
      if (metricNames.isEmpty()) {
        throw new FriendlyException(
            "A metric-filter processor configuration has an exclude section with no \"metricNames\".",
            "Please provide a \"metricNames\" section under the exclude section of the metric-filter processor configuration. "
                + "Learn more about span processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
      }
      if (matchType == MatchType.REGEXP) {
        for (String metricName : metricNames) {
          validateRegex(metricName, ProcessorType.METRIC_FILTER);
        }
      }

      validateSectionIsEmpty(
          spanNames, ProcessorType.METRIC_FILTER, IncludeExclude.EXCLUDE, "spanNames");
    }

    private static void validateSectionIsEmpty(
        List<?> list, ProcessorType type, IncludeExclude includeExclude, String sectionName) {
      if (!list.isEmpty()) {
        throwUnexpectedSectionFriendlyException(type, includeExclude, sectionName);
      }
    }

    private static void throwUnexpectedSectionFriendlyException(
        ProcessorType type, IncludeExclude includeExclude, String sectionName) {
      throw new FriendlyException(
          type.anX
              + " processor configuration has "
              + includeExclude
              + " section with an unexpected section \""
              + sectionName
              + "\".",
          "Please do not provide a \""
              + sectionName
              + "\" section under the "
              + includeExclude
              + " section of the "
              + type
              + " processor configuration. "
              + "Learn more about "
              + type
              + " processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
    }
  }

  private static void validateRegex(String value, ProcessorType processorType) {
    try {
      Pattern.compile(value);
    } catch (PatternSyntaxException e) {
      // TODO different links for different processor types throughout?
      throw new FriendlyException(
          processorType.anX + " processor configuration has an invalid regex:" + value,
          "Please provide a valid regex in the "
              + processorType
              + " processor configuration. "
              + "Learn more about "
              + processorType
              + " processors here: https://go.microsoft.com/fwlink/?linkid=2151557",
          e);
    }
  }

  public static class ProcessorAttribute {
    public String key;
    public String value;
  }

  public static class ExtractAttribute {

    public final Pattern pattern;
    public final List<String> groupNames;

    // visible for testing
    public ExtractAttribute(Pattern pattern, List<String> groupNames) {
      this.pattern = pattern;
      this.groupNames = groupNames;
    }

    // TODO: Handle empty patterns or groupNames are not populated gracefully
    public void validate() {
      if (groupNames.isEmpty()) {
        throw new FriendlyException(
            "An attribute processor configuration does not have valid regex to extract attributes: "
                + pattern,
            "Please provide a valid regex of the form (?<name>X) where X is the usual regular expression. "
                + "Learn more about attribute processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
      }
    }
  }

  public static class MaskAttribute {
    private static final Pattern replacePattern = Pattern.compile("\\$\\{[A-Za-z1-9]*\\}*");
    public final Pattern pattern;
    public final List<String> groupNames;
    public final String replace;

    // visible for testing
    public MaskAttribute(Pattern pattern, List<String> groupNames, String replace) {
      this.pattern = pattern;
      this.groupNames = groupNames;
      this.replace = replace;
    }

    // TODO: Handle empty patterns or groupNames are not populated gracefully
    public void validate() {
      if (groupNames.isEmpty()) {
        throw new FriendlyException(
            "An attribute processor configuration does not have valid regex to mask attributes: "
                + pattern,
            "Please provide a valid regex of the form (?<name>X) where X is the usual regular expression. "
                + "Learn more about attribute processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
      }

      Matcher maskMatcher = replacePattern.matcher(replace);
      while (maskMatcher.find()) {
        String groupName = maskMatcher.group();
        String replacedString = "";
        if (groupName.length() > 3) {
          // to extract string of format ${foo}
          replacedString = groupName.substring(2, groupName.length() - 1);
        }
        if (replacedString.isEmpty()) {
          throw new FriendlyException(
              "An attribute processor configuration does not have valid `replace` value to mask attributes: "
                  + replace,
              "Please provide a valid replace value of the form (${foo}***${bar}). "
                  + "Learn more about attribute processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
        }
        if (!groupNames.contains(replacedString)) {
          throw new FriendlyException(
              "An attribute processor configuration does not have valid `replace` value to mask attributes: "
                  + replace,
              "Please make sure the replace value matches group names used in the `pattern` regex. "
                  + "Learn more about attribute processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
        }
      }
    }
  }

  public static class ProcessorAction {
    @Nullable public final AttributeKey<String> key;
    public final ProcessorActionType action;
    public final String value;
    @Nullable public final AttributeKey<String> fromAttribute;
    @Nullable public final ExtractAttribute extractAttribute;
    @Nullable public final MaskAttribute maskAttribute;

    @JsonCreator
    public ProcessorAction(
        // TODO (trask) should this take attribute type, e.g. "key:type"
        @JsonProperty("key") @Nullable String key,
        @JsonProperty("action") @Nullable ProcessorActionType action,
        @JsonProperty("value") @Nullable String value,
        // TODO (trask) should this take attribute type, e.g. "key:type"
        @JsonProperty("fromAttribute") @Nullable String fromAttribute,
        @JsonProperty("pattern") @Nullable String pattern,
        @JsonProperty("replace") @Nullable String replace) {
      this.key = isEmpty(key) ? null : AttributeKey.stringKey(key);
      this.action = action;
      this.value = value;
      this.fromAttribute = isEmpty(fromAttribute) ? null : AttributeKey.stringKey(fromAttribute);

      if (pattern == null) {
        extractAttribute = null;
        maskAttribute = null;
      } else {
        Pattern regexPattern;
        try {
          regexPattern = Pattern.compile(pattern);
        } catch (PatternSyntaxException e) {
          throw new FriendlyException(
              "Telemetry processor configuration does not have valid regex:" + pattern,
              "Please provide a valid regex in the telemetry processors configuration. "
                  + "Learn more about telemetry processors here: https://go.microsoft.com/fwlink/?linkid=2151557",
              e);
        }
        List<String> groupNames = Patterns.getGroupNames(pattern);
        if (replace != null) {
          extractAttribute = null;
          maskAttribute = new Configuration.MaskAttribute(regexPattern, groupNames, replace);
        } else {
          maskAttribute = null;
          extractAttribute = new Configuration.ExtractAttribute(regexPattern, groupNames);
        }
      }
    }

    public void validate() {

      if (key == null) {
        throw new FriendlyException(
            "An attribute processor configuration has an action section that is missing a \"key\".",
            "Please provide a \"key\" under the action section of the attribute processor configuration. "
                + "Learn more about attribute processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
      }
      if (action == null) {
        throw new FriendlyException(
            "An attribute processor configuration has an action section that is missing an \"action\".",
            "Please provide an \"action\" under the action section of the attribute processor configuration. "
                + "Learn more about attribute processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
      }
      if (action == ProcessorActionType.INSERT || action == ProcessorActionType.UPDATE) {
        if (isEmpty(value) && fromAttribute == null) {
          throw new FriendlyException(
              "An attribute processor configuration has an "
                  + action
                  + " action that is missing a \"value\" or a \"fromAttribute\".",
              "Please provide exactly one of \"value\" or \"fromAttributes\" under the "
                  + action
                  + " action. "
                  + "Learn more about attribute processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
        }
        if (!isEmpty(value) && fromAttribute != null) {
          throw new FriendlyException(
              "An attribute processor configuration has an "
                  + action
                  + " action that has both a \"value\" and a \"fromAttribute\".",
              "Please provide exactly one of \"value\" or \"fromAttributes\" under the "
                  + action
                  + " action. "
                  + "Learn more about attribute processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
        }
        if (extractAttribute != null) {
          throw new FriendlyException(
              "An attribute processor configuration has an "
                  + action
                  + " action with an \"pattern\" section.",
              "Please do not provide an \"pattern\" under the "
                  + action
                  + " action. "
                  + "Learn more about attribute processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
        }
        if (maskAttribute != null) {
          throw new FriendlyException(
              "An attribute processor configuration has an "
                  + action
                  + " action with an \"replace\" section.",
              "Please do not provide an \"replace\" under the "
                  + action
                  + " action. "
                  + "Learn more about attribute processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
        }
      }

      if (action == ProcessorActionType.EXTRACT) {
        if (extractAttribute == null) {
          throw new FriendlyException(
              "An attribute processor configuration has an extract action that is missing an \"pattern\" section.",
              "Please provide an \"pattern\" section under the extract action. "
                  + "Learn more about attribute processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
        }
        if (!isEmpty(value)) {
          throw new FriendlyException(
              "An attribute processor configuration has an " + action + " action with a \"value\".",
              "Please do not provide a \"value\" under the "
                  + action
                  + " action. "
                  + "Learn more about attribute processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
        }
        if (fromAttribute != null) {
          throw new FriendlyException(
              "An attribute processor configuration has an "
                  + action
                  + " action with a \"fromAttribute\".",
              "Please do not provide a \"fromAttribute\" under the "
                  + action
                  + " action. "
                  + "Learn more about attribute processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
        }
        extractAttribute.validate();
      }

      if (action == ProcessorActionType.MASK) {
        if (maskAttribute == null) {
          throw new FriendlyException(
              "An attribute processor configuration has an mask action that is missing an \"pattern\" or \"replace\" section.",
              "Please provide an \"pattern\" section and \"replace\" section under the mask action. "
                  + "Learn more about attribute processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
        }
        if (!isEmpty(value)) {
          throw new FriendlyException(
              "An attribute processor configuration has an " + action + " action with a \"value\".",
              "Please do not provide a \"value\" under the "
                  + action
                  + " action. "
                  + "Learn more about attribute processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
        }
        if (fromAttribute != null) {
          throw new FriendlyException(
              "An attribute processor configuration has an "
                  + action
                  + " action with a \"fromAttribute\".",
              "Please do not provide a \"fromAttribute\" under the "
                  + action
                  + " action. "
                  + "Learn more about attribute processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
        }
        maskAttribute.validate();
      }
    }
  }

  public enum RequestFilterType {
    @JsonProperty("name-regex")
    NAME_REGEX
  }

  public static class RequestFilter {
    public RequestFilterType type;
    public String value;
  }

  public static class RequestAggregationConfig {

    // Threshold in ms over which a span will consider to be a breach
    // Used by the breach ratio aggregation
    public int thresholdMillis = 5000;

    // Minimum number of samples that must have been collected in order for the aggregation to
    // produce data. Avoids volatile aggregation output on small sample sizes.
    public int minimumSamples = 0;
  }

  public enum RequestAggregationType {
    @JsonProperty("breach-ratio")
    BREACH_RATIO
  }

  public static class RequestAggregation {
    public RequestAggregationType type = RequestAggregationType.BREACH_RATIO;
    public long windowSizeMillis = 60000; // in ms
    public RequestAggregationConfig configuration = new RequestAggregationConfig();
  }

  public enum RequestTriggerThresholdType {
    @JsonProperty("greater-than")
    GREATER_THAN
  }

  public static class RequestTriggerThreshold {
    public RequestTriggerThresholdType type = RequestTriggerThresholdType.GREATER_THAN;

    // Threshold value applied to the output of the aggregation
    // i.e :
    //  - For the BreachRatio aggregation, 0.75 means this will trigger if 75% of sample breach the
    // threshold.
    //  - For a rolling average aggregation 0.75 will mean this will trigger if the average request
    // processing time
    //      breaches 0.75ms
    public float value = 0.75f;
  }

  public enum RequestTriggerThrottlingType {
    @JsonProperty("fixed-duration-cooldown")
    FIXED_DURATION_COOLDOWN
  }

  public static class RequestTriggerThrottling {
    public RequestTriggerThrottlingType type = RequestTriggerThrottlingType.FIXED_DURATION_COOLDOWN;
    public int value = 60; // in seconds
  }

  public enum RequestTriggerType {
    LATENCY
  }

  public static class RequestTrigger {
    public String name;
    public RequestTriggerType type = RequestTriggerType.LATENCY;
    public RequestFilter filter = new RequestFilter();
    public RequestAggregation aggregation = new RequestAggregation();
    public RequestTriggerThreshold threshold = new RequestTriggerThreshold();
    public RequestTriggerThrottling throttling = new RequestTriggerThrottling();
    public int profileDuration = 30; // in s
  }

  public static class ProfilerConfiguration {
    public int configPollPeriodSeconds = 60;
    public int periodicRecordingDurationSeconds = 120;
    public int periodicRecordingIntervalSeconds = 60 * 60;
    public boolean enabled = true;
    public String memoryTriggeredSettings = "profile-without-env-data";
    public String cpuTriggeredSettings = "profile-without-env-data";
    public String manualTriggeredSettings = "profile-without-env-data";
    @Nullable public String serviceProfilerFrontEndPoint = null;
    public boolean enableDiagnostics = false;
    public boolean enableRequestTriggering = false;
    public RequestTrigger[] requestTriggerEndpoints = {};
  }

  public static class GcEventConfiguration {
    public GcReportingLevel reportingLevel;
  }

  public static class AadAuthentication {
    public boolean enabled;
    public AuthenticationType type;
    public String clientId;
    public String tenantId;
    public String clientSecret;
    public String authorityHost;

    public void validate() {
      if (!enabled) {
        return;
      }
      if (type == null) {
        throw new FriendlyException(
            "AAD Authentication configuration is missing authentication \"type\".",
            "Please provide a valid authentication \"type\" under the \"authentication\" configuration. "
                + "Learn more about authentication configuration here: https://docs.microsoft.com/en-us/azure/azure-monitor/app/java-standalone-config");
      }

      if (type == AuthenticationType.UAMI) {
        if (isEmpty(clientId)) {
          throw new FriendlyException(
              "AAD Authentication configuration of type User Assigned Managed Identity is missing \"clientId\".",
              "Please provide a valid \"clientId\" under the \"authentication\" configuration. "
                  + "Learn more about authentication configuration here: https://docs.microsoft.com/en-us/azure/azure-monitor/app/java-standalone-config");
        }
      }

      if (type == AuthenticationType.CLIENTSECRET) {
        if (isEmpty(clientId)) {
          throw new FriendlyException(
              "AAD Authentication configuration of type Client Secret Identity is missing \"clientId\".",
              "Please provide a valid \"clientId\" under the \"authentication\" configuration. "
                  + "Learn more about authentication configuration here: https://docs.microsoft.com/en-us/azure/azure-monitor/app/java-standalone-config");
        }

        if (isEmpty(tenantId)) {
          throw new FriendlyException(
              "AAD Authentication configuration of type Client Secret Identity is missing \"tenantId\".",
              "Please provide a valid \"tenantId\" under the \"authentication\" configuration. "
                  + "Learn more about authentication configuration here: https://docs.microsoft.com/en-us/azure/azure-monitor/app/java-standalone-config");
        }

        if (isEmpty(clientSecret)) {
          throw new FriendlyException(
              "AAD Authentication configuration of type Client Secret Identity is missing \"clientSecret\".",
              "Please provide a valid \"clientSecret\" under the \"authentication\" configuration. "
                  + "Learn more about authentication configuration here: https://docs.microsoft.com/en-us/azure/azure-monitor/app/java-standalone-config");
        }
      }
    }
  }

  public enum AuthenticationType {
    // TODO (kyralama) should these use @JsonProperty to bind lowercase like other enums?
    UAMI,
    SAMI,
    VSCODE,
    CLIENTSECRET
  }
}
