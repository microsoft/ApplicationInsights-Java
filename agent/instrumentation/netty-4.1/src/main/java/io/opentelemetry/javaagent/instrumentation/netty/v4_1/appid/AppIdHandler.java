// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.netty.v4_1.appid;

import com.microsoft.applicationinsights.agent.bootstrap.AiAppId;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpResponse;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.instrumentation.netty.v4_1.AttributeKeys;

public class AppIdHandler extends ChannelOutboundHandlerAdapter {
  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise prm) {
    Context context = ctx.channel().attr(AttributeKeys.SERVER_CONTEXT).get();
    if (context == null || !(msg instanceof HttpResponse)) {
      ctx.write(msg, prm);
      return;
    }

    HttpResponse response = (HttpResponse) msg;
    String appId = AiAppId.getAppId();
    if (!appId.isEmpty()) {
      response.headers().set(AiAppId.RESPONSE_HEADER_NAME, "appId=" + appId);
    }
    ctx.write(msg, prm);
  }
}
