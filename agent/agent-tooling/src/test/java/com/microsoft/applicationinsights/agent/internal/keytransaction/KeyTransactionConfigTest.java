package com.microsoft.applicationinsights.agent.internal.keytransaction;

import static com.microsoft.applicationinsights.agent.internal.keytransaction.KeyTransactionConfig.Operator.EQUALS;
import static org.assertj.core.api.Assertions.assertThat;

import com.azure.json.JsonProviders;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class KeyTransactionConfigTest {

  @Test
  void test() throws Exception {
    InputStream in = KeyTransactionConfigTest.class.getResourceAsStream("demo.json");
    NewResponse parsedResponse = NewResponse.fromJson(JsonProviders.createReader(in));

    assertThat(parsedResponse.getSdkConfigurations()).hasSize(2);

    SdkConfiguration earthOrbitSdkConfiguration = parsedResponse.getSdkConfigurations().get(0);
    assertThat(earthOrbitSdkConfiguration.getKey()).isEqualTo("Transaction");

    KeyTransactionConfig earthOrbitKeyTransactionConfig = earthOrbitSdkConfiguration.getValue();
    assertThat(earthOrbitKeyTransactionConfig.getName()).isEqualTo("EarthOrbit");

    assertThat(earthOrbitKeyTransactionConfig.getStartCriteria()).hasSize(1);
    KeyTransactionConfig.Criterion earthOrbitStartCriteria =
        earthOrbitKeyTransactionConfig.getStartCriteria().get(0);
    assertThat(earthOrbitStartCriteria.getField().getKey()).isEqualTo("http.path");
    assertThat(earthOrbitStartCriteria.getOperator()).isEqualTo(EQUALS);
    assertThat(earthOrbitStartCriteria.getValue()).isEqualTo("earth");

    assertThat(earthOrbitKeyTransactionConfig.getEndCriteria()).isEmpty();

    SdkConfiguration marsMissionSdkConfiguration = parsedResponse.getSdkConfigurations().get(1);
    assertThat(marsMissionSdkConfiguration.getKey()).isEqualTo("Transaction");

    KeyTransactionConfig marsMissionKeyTransactionConfig = marsMissionSdkConfiguration.getValue();
    assertThat(marsMissionKeyTransactionConfig.getName()).isEqualTo("MarsMission");

    assertThat(marsMissionKeyTransactionConfig.getStartCriteria()).hasSize(1);
    KeyTransactionConfig.Criterion marsMissionStartCriteria =
        marsMissionKeyTransactionConfig.getStartCriteria().get(0);
    assertThat(marsMissionStartCriteria.getField().getKey()).isEqualTo("http.path");
    assertThat(marsMissionStartCriteria.getOperator()).isEqualTo(EQUALS);
    assertThat(marsMissionStartCriteria.getValue()).isEqualTo("mars");

    assertThat(marsMissionKeyTransactionConfig.getEndCriteria()).hasSize(1);
    KeyTransactionConfig.Criterion marsMissionEndCriteria =
        marsMissionKeyTransactionConfig.getEndCriteria().get(0);
    assertThat(marsMissionEndCriteria.getField().getKey()).isEqualTo("messaging.todo");
    assertThat(marsMissionEndCriteria.getOperator()).isEqualTo(EQUALS);
    assertThat(marsMissionEndCriteria.getValue()).isEqualTo("todo");
  }
}
