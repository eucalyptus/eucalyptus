/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service.ws;

import java.util.EnumSet;
import org.jboss.netty.channel.ChannelPipeline;
import com.eucalyptus.auth.principal.TemporaryAccessKey;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.route53.common.Route53;
import com.eucalyptus.route53.service.config.Route53Configuration;
import com.eucalyptus.ws.server.RestXmlPipeline;

/**
 *
 */
@ComponentPart(Route53.class)
public class Route53RestXmlPipeline extends RestXmlPipeline {

  public Route53RestXmlPipeline() {
    super(
        "route53-rest-xml",
        Route53Configuration.SERVICE_PATH,
        EnumSet.allOf(TemporaryAccessKey.TemporaryKeyType.class));
  }

  @Override
  public ChannelPipeline addHandlers(final ChannelPipeline pipeline) {
    super.addHandlers(pipeline);
    pipeline.addLast("route53-rest-xml-binding", new Route53RestXmlBinding());
    return pipeline;
  }
}
