# Triggers

We have the option to trigger based on resource usage (i.e., CPU and memory), as well as
the duration of span (e.g., a business transaction) data that is often automatically collected by
OpenTelemetry (otel). The resource triggers can be configured
from within the Application Insights UI under the performance->profiler->triggers menu.
Currently, the span triggers must be configured within the json configuration of your
process.

## Configuration

An example of a complete configuration is as follows:

```json
{
  "connectionString": ".....",
  "preview": {
    "profiler": {
      "enableRequestTriggering": true,
      "requestTriggerEndpoints": [
        {
          "name": "Users endpoint is responsive",
          "type": "latency",
          "filter": {
            "type": "name-regex",
            "value": "/users/get/.*"
          },
          "aggregation": {
            "configuration": {
              "thresholdMillis": 7000
            },
            "type": "breach-ratio",
            "windowSizeMillis": 60000
          },
          "threshold": {
            "value": 0.75
          },
          "profileDuration": 30,
          "throttling": {
            "value": 60
          }
        }
      ]
    }
  },
  "role": {
    "instance": "my-app",
    "name": "my-service"
  }
}
```

This example configuration states that for a rolling window of 60 seconds if more than 75% of the
transaction going through `/users/get/.*` take longer than 7 seconds, then that is a breach of SLA
and a diagnosis will be triggered. Specifically:

- Works on the latency/duration of spans.
- Filters spans that match the regex `/users/get/.*`.
- Calculates the ratio of requests that breach 7 seconds.
- If that ratio goes above 0.75 (i.e 75%) a profile it triggered. This is calculated in a rolling 60
  second window).
- On a breach a 30 second profile is gathered.
- After a profile is generated a 60 second cooldown is applied and a profile cannot be re-triggered.

The key aspects for triggering are the `enableRequestTriggering` and `requestTriggerEndpoints`
values.

- `enableRequestTriggering` - Enables the subsystem that monitors otel span data
- `requestTriggerEndpoints` - A list of SLA definitions, if any of these are breached a
  profile will be generated.

## SLA configuration

Each individual configuration is formed of:

- `aggregation` - An aggregation function that computes a metric over which the trigger
  will be evaluated, such as mean, max, min etc.
- `filter` - Filters the spans of interest for this SLA
- `name` - Name of this SLA trigger, this will be displayed within the UI
- `type` - The type of the metric that will be analysed (at time of writing latency is the only
  supported type).
- `profileDuration` - The duration in seconds of the profile to be collected when this SLA is
  breached.
- `threshold` - The threshould applied to the output of the aggregation, if this value is breached a
  profile will be triggered, i.e the `breach-ratio` aggregation outputs the percentage of requests
  that breach the SLA, a threshold of 0.95 would trigger a profile if 95% of requests breach the
  SLA.
- `throttling` - Configures a cooldown to prevent excessive triggering.

### `aggregation`

Currently, we support:

- `breach-ratio` - This calculates the ratio of samples that breached the configured value.
  - Configuration parameters:
    - `thresholdMs` - The threshold (in milliseconds) above which a span will be considered breached.
    - `minimumSamples` - The minimum number of samples that must be collected for the aggregation to
      produce data, this is to prevent triggering off of small sample sizes

```json
{
  "aggregation": {
    "configuration": {
      "thresholdMillis": 7000
    },
    "type": "breach-ratio"
  }
}
```

### `filter`

- `name-regex` - If the regular expression matches then the span is included

```json
{
  "filter": {
    "type": "name-regex",
    "value": "/users/get/[A-Za-z]+"
  }
}
```

### `threshold`

- `type` - One of: `greater-than`, `less-than`.
- `value` - value that will be applied to the output of the aggregation

```json
{
  "threshold": {
    "type": "greater-than",
    "value": 0.75
  }
}
```

### `throttling`

- `type` - Currently supports `fixed-duration-cooldown`
- `value` - Time in seconds during which a profile will not be triggered

```json
{
  "throttling": {
    "type": "fixed-duration-cooldown",
    "value": 30
  }
}
```

## Examples

### Monitor all requests

- 30 second profile
- Filters paths that match /.*
- Triggers when 75% of requests are greater than 7000 milliseconds
- Prevents re-triggering for 60 seconds

```json
{
  "preview": {
    "profiler": {
      "enableRequestTriggering": true,
      "requestTriggerEndpoints": [
        {
          "name": "All",
          "type": "latency",
          "profileDuration": 30,
          "filter": {
            "type": "name-regex",
            "value": "/.*"
          },
          "aggregation": {
            "configuration": {
              "thresholdMillis": 7000
            },
            "type": "breach-ratio"
          },
          "threshold": {
            "value": 0.75
          },
          "throttling": {
            "value": 60
          }
        }
      ]
    }
  }
}
```

### Monitor 2 endpoints

- /users/.* endpoints
  - Filters paths that match /users/.*
  - 30 second profile
  - Triggers when 75% of requests are greater than 7000 milliseconds
  - Prevents re-triggering for 60 seconds
- /index.html endpoint
  - Filters paths that match /index.html
  - 60 second profile
  - Triggers when 50% of requests are greater than 100 milliseconds
  - Prevents re-triggering for 60 seconds

```json
{
  "preview": {
    "profiler": {
      "enabled": true,
      "enableRequestTriggering": true,
      "requestTriggerEndpoints": [
        {
          "name": "Users",
          "type": "latency",
          "profileDuration": 30,
          "filter": {
            "type": "name-regex",
            "value": "/users/.*"
          },
          "aggregation": {
            "configuration": {
              "thresholdMillis": 7000
            },
            "type": "breach-ratio"
          },
          "threshold": {
            "value": 0.75
          },
          "throttling": {
            "value": 60
          }
        },
        {
          "name": "Index.html",
          "type": "latency",
          "profileDuration": 60,
          "filter": {
            "type": "name-regex",
            "value": "/index\\.html"
          },
          "aggregation": {
            "configuration": {
              "thresholdMillis": 100
            },
            "type": "breach-ratio"
          },
          "threshold": {
            "value": 0.5
          },
          "throttling": {
            "value": 60
          }
        }
      ]
    }
  }
}
```
