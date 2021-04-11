/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.ws;

import java.util.EnumSet;
import org.jboss.netty.channel.ChannelPipeline;
import com.eucalyptus.auth.principal.TemporaryAccessKey;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.rds.common.Rds;
import com.eucalyptus.rds.service.config.RdsConfiguration;
import com.eucalyptus.ws.server.QueryPipeline;

/**
 *
 */
@ComponentPart(Rds.class)
public class RdsQueryPipeline extends QueryPipeline {

  public RdsQueryPipeline() {
    super(
        "rds-query",
        RdsConfiguration.SERVICE_PATH,
        EnumSet.allOf(TemporaryAccessKey.TemporaryKeyType.class));
  }

  @Override
  public ChannelPipeline addHandlers(final ChannelPipeline pipeline) {
    super.addHandlers(pipeline);
    pipeline.addLast("rds-query-binding", new RdsQueryBinding());
    return pipeline;
  }
}
