/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.ws;

import java.util.EnumSet;
import org.jboss.netty.channel.ChannelPipeline;
import com.eucalyptus.auth.principal.TemporaryAccessKey;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2;
import com.eucalyptus.loadbalancingv2.service.config.Loadbalancingv2Configuration;
import com.eucalyptus.ws.server.QueryPipeline;

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
    pipeline.addLast("loadbalancingv2-query-binding", new Loadbalancingv2QueryBinding());
    return pipeline;
  }
}
