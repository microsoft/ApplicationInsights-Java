/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.azure.monitor.opentelemetry.exporter;

import com.azure.core.annotation.ReturnType;
import com.azure.core.annotation.ServiceClient;
import com.azure.core.annotation.ServiceMethod;
import com.azure.core.http.rest.Response;
import com.azure.core.util.Context;
import com.azure.monitor.opentelemetry.exporter.implementation.models.ExportResult;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import java.util.List;

/**
 * This class contains synchronous operations to interact with the Azure Monitor Exporter service.
 */
@ServiceClient(builder = AzureMonitorExporterBuilder.class)
class MonitorExporterClient {

  private final MonitorExporterAsyncClient asyncClient;

  MonitorExporterClient(MonitorExporterAsyncClient asyncClient) {
    this.asyncClient = asyncClient;
  }

  /**
   * The list of telemetry items that will be sent to the Azure Monitor Exporter service. The
   * response contains the status of number of telemetry items successfully accepted and the number
   * of items that failed along with the error code for all the failed items.
   *
   * @param telemetryItems The list of telemetry items to send.
   * @return The response containing the number of successfully accepted items and error details of
   *     items that were rejected.
   */
  @ServiceMethod(returns = ReturnType.SINGLE)
  ExportResult export(List<TelemetryItem> telemetryItems) {
    return asyncClient.export(telemetryItems).block();
  }

  /**
   * The list of telemetry items that will be sent to the Azure Monitor Exporter service. The
   * response contains the status of number of telemetry items successfully accepted and the number
   * of items that failed along with the error code for all the failed items.
   *
   * @param telemetryItems The list of telemetry items to send.
   * @param context Additional context that is passed through the Http pipeline during the service
   *     call.
   * @return The response containing the number of successfully accepted items and error details of
   *     items that were rejected.
   */
  @ServiceMethod(returns = ReturnType.SINGLE)
  Response<ExportResult> exportWithResponse(List<TelemetryItem> telemetryItems, Context context) {
    return asyncClient.exportWithResponse(telemetryItems, context).block();
  }
}
