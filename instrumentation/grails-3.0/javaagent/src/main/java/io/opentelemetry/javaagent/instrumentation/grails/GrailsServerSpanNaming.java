/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.grails;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.servlet.ServletContextPath;
import java.util.function.Supplier;
import org.grails.web.mapping.mvc.GrailsControllerUrlMappingInfo;

public class GrailsServerSpanNaming {

  public static Supplier<String> getServerSpanNameSupplier(
      Context context, GrailsControllerUrlMappingInfo info) {
    return () -> getServerSpanName(context, info);
  }

  private static String getServerSpanName(Context context, GrailsControllerUrlMappingInfo info) {
    String action =
        info.getActionName() != null
            ? info.getActionName()
            : info.getControllerClass().getDefaultAction();
    return ServletContextPath.prepend(context, "/" + info.getControllerName() + "/" + action);
  }
}
