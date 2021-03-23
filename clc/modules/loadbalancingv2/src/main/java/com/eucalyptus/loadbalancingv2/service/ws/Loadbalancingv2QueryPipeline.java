/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.ws;

import com.eucalyptus.auth.principal.TemporaryAccessKey;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.loadbalancing.common.LoadBalancing;
import com.eucalyptus.loadbalancing.ws.LoadBalancingQueryBinding;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2;
import com.eucalyptus.loadbalancingv2.service.config.Loadbalancingv2Configuration;
import com.eucalyptus.ws.Handlers;
import com.eucalyptus.ws.protocol.RequiredQueryParams;
import com.eucalyptus.ws.server.QueryPipeline;
import java.util.EnumSet;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;

/**
 *
 */
@ComponentPart(Loadbalancingv2.class)
public class Loadbalancingv2QueryPipeline extends QueryPipeline {

  public Loadbalancingv2QueryPipeline() {
    super(
        "loadbalancingv2-query",
        Loadbalancingv2Configuration.SERVICE_PATH,
        EnumSet.allOf(TemporaryAccessKey.TemporaryKeyType.class));
  }

  @Override
  public ChannelPipeline addHandlers(final ChannelPipeline pipeline) {
    super.addHandlers(pipeline);
    pipeline.addLast("loadbalancingv2-query-binding-switch", new BindingVersionSwitchHandler());
    pipeline.addLast("loadbalancingv2-query-binding", new Loadbalancingv2QueryBinding());
    return pipeline;
  }

  @Override
  public int getOrder() {
    return DEFAULT_ORDER - 1000;
  }

  private static final class BindingVersionSwitchHandler implements ChannelUpstreamHandler {
    @Override
    public void handleUpstream(
        final ChannelHandlerContext ctx,
        final ChannelEvent event
    ) {
      if (event instanceof MessageEvent){
        final MessageEvent messageEvent = (MessageEvent) event;
        if (messageEvent.getMessage() instanceof MappingHttpRequest) {
          final MappingHttpRequest httpRequest = (MappingHttpRequest) messageEvent.getMessage();
          final String version = httpRequest.getParameters().get(RequiredQueryParams.Version.toString());
          if (version == null || "2012-06-01".equals(version)) {
            ctx.getPipeline().replace(
                "loadbalancingv2-query-binding",
                "loadbalancingv1-query-binding",
                new LoadBalancingQueryBinding());
            //TODO:STEVE: better api for this
            ctx.getPipeline().remove("msg-component-check");
            Handlers.addComponentHandlers(LoadBalancing.class, ctx.getPipeline());
            final ChannelHandler componentCheck = ctx.getPipeline().remove("msg-component-check");
            ctx.getPipeline().addAfter("loadbalancingv1-query-binding", "msg-component-check", componentCheck);
          }
        }
      }
      ctx.sendUpstream( event );
    }
  }
}
