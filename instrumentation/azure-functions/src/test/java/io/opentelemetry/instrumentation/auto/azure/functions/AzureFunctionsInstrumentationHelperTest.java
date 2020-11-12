/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.instrumentation.auto.azure.functions;

import com.google.common.base.Strings;
import io.opentelemetry.instrumentation.api.aiconnectionstring.AiConnectionString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AzureFunctionsInstrumentationHelperTest {

  /**
   * Lazily Set Connection String For Linux Consumption Plan:
   *
   *    Term	    LazySetOptIn	 ConnectionString	    EnableAgent	      LazySet
   *    Preview	  FALSE	         VALID           	    TRUE	            Enabled
	 *	                           VALID                FALSE	            Disabled
	 *                             VALID                NULL	            Disabled
	 *	                           NULL	                TRUE/FALSE/NULL	  Disabled
   *     GA	      TRUE	         VALID	              TRUE	            Enabled
   *                             VALID	              FALSE             Disabled
   *	                           VALID	              NULL              Enabled
	 *                             NULL	                TRUE/FALSE/NULL	  Disabled
   */

  private static final String CONNECTION_STRING = "InstrumentationKey=00000000-0000-0000-0000-0FEEDDADBEEF;IngestionEndpoint=http://fakeingestion:60606/";
  private static final String INSTRUMENTATION_KEY = "00000000-0000-0000-0000-0FEEDDADBEEF";

  @Test
  @DisplayName("LazySetOptIn is FALSE, ConnectionString is valid and EnableAgent is TRUE")
  public void enableLazySetWithLazySetOptInOffEnableAgentOn() {
    assertTrue(AzureFunctionsInstrumentationHelper.shouldSetConnectionString(false, "true"));
  }

  @Test
  @DisplayName("LazySetOptIn is FALSE, ConnectionString is valid and EnableAgent is FALSE")
  public void disableLazySetWithLazySetOptInOffEnableAgentOff() {
    assertFalse(AzureFunctionsInstrumentationHelper.shouldSetConnectionString(false, "false"));
  }

  @Test
  @DisplayName("LazySetOptIn is FALSE, ConnectionString is valid and EnableAgent is NULL")
  public void disableLazySetWithLazySetOptInOffEnableAgentNull() {
    assertFalse(AzureFunctionsInstrumentationHelper.shouldSetConnectionString(false, null));
  }

  @Test
  @DisplayName("LazySetOptIn is FALSE, ConnectionString is NULL, InstrumentationKey is NULL, and EnableAgent is TRUE")
  public void disableLazySetWithLazySetOptInOffConnectionStringNullInstrumentationKeyNull() {
    assertTrue(AzureFunctionsInstrumentationHelper.shouldSetConnectionString(false, "true"));
    MockedAccessor accessor = new MockedAccessor();
    AiConnectionString.setAccessor(accessor);
    AzureFunctionsInstrumentationHelper.setConnectionString(null, null);
    assertNull(accessor.connectionString);
  }

  @Test
  @DisplayName("LazySetOptIn is FALSE, ConnectionString is NULL, InstrumentationKey is valid, and EnableAgent is TRUE")
  public void enableLazySetWithLazySetOptInOffConnectionStringNullInstrumentationKeyNotNull() {
    assertTrue(AzureFunctionsInstrumentationHelper.shouldSetConnectionString(false, "true"));
    MockedAccessor accessor = new MockedAccessor();
    AiConnectionString.setAccessor(accessor);
    AzureFunctionsInstrumentationHelper.setConnectionString(null, INSTRUMENTATION_KEY);
    assertEquals(accessor.connectionString, "InstrumentationKey=" + INSTRUMENTATION_KEY);
  }

  @Test
  @DisplayName("LazySetOptIn is TRUE, ConnectionString is valid and EnableAgent is TRUE")
  public void enableLazySetWithLazySetOptInOnEnableAgentOn() {
    assertTrue(AzureFunctionsInstrumentationHelper.shouldSetConnectionString(true, "true"));
  }

  @Test
  @DisplayName("LazySetOptIn is TRUE, ConnectionString is valid and EnableAgent is FALSE")
  public void disableLazySetWithLazySetOptInOnEnableAgentOff() {
    assertFalse(AzureFunctionsInstrumentationHelper.shouldSetConnectionString(true, "false"));
  }

  @Test
  @DisplayName("LazySetOptIn is TRUE, ConnectionString is valid and EnableAgent is NULL")
  public void enableLazySetWithLazySetOptInOnEnableAgentNull() {
    assertTrue(AzureFunctionsInstrumentationHelper.shouldSetConnectionString(true, null));
  }

  @Test
  @DisplayName("LazySetOptIn is TRUE, ConnectionString is NULL, InstrumentationKey is NULL, and EnableAgent is TRUE")
  public void disableLazySetWithLazySetOptInOnConnectionStringNullAndInstrumentationKeyNull() {
    assertTrue(AzureFunctionsInstrumentationHelper.shouldSetConnectionString(true, "true"));
    MockedAccessor accessor = new MockedAccessor();
    AiConnectionString.setAccessor(accessor);
    AzureFunctionsInstrumentationHelper.setConnectionString(null, null);
    assertNull(accessor.connectionString);
  }

  @Test
  @DisplayName("LazySetOptIn is TRUE, ConnectionString is NULL, InstrumentationKey is valid, and EnableAgent is TRUE")
  public void enableLazySetWithLazySetOptInOnConnectionStringNullInstrumentationKeyNotNull() {
    assertTrue(AzureFunctionsInstrumentationHelper.shouldSetConnectionString(false, "true"));
    MockedAccessor accessor = new MockedAccessor();
    AiConnectionString.setAccessor(accessor);
    AzureFunctionsInstrumentationHelper.setConnectionString(null, INSTRUMENTATION_KEY);
    assertEquals(accessor.connectionString, "InstrumentationKey=" + INSTRUMENTATION_KEY);
  }

  private class MockedAccessor implements AiConnectionString.Accessor {
    private String connectionString;

    @Override
    public boolean hasValue() {
      return !Strings.isNullOrEmpty(connectionString);
    }

    @Override
    public void setValue(String value) {
      connectionString = value;
    }
  }
}

