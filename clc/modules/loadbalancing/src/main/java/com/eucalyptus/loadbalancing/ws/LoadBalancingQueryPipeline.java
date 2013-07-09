/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.loadbalancing.ws;

import java.util.EnumSet;
import org.jboss.netty.channel.ChannelPipeline;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.ws.protocol.RequiredQueryParams;
import com.eucalyptus.ws.server.QueryPipeline;
import com.eucalyptus.loadbalancing.LoadBalancing;

/**
 * @author Chris Grzegorczyk <grze@eucalyptus.com>
 */
@ComponentPart(LoadBalancing.class)
public class LoadBalancingQueryPipeline extends QueryPipeline {

  public LoadBalancingQueryPipeline( ) {
    super( "loadbalancing-query-pipeline", "/services/LoadBalancing", true, EnumSet.of( RequiredQueryParams.Version ) );
  }

  @Override
  public ChannelPipeline addHandlers( final ChannelPipeline pipeline ) {
    super.addHandlers( pipeline );
    pipeline.addLast( "loadbalancing-query-binding", new LoadBalancingQueryBinding() );
    return pipeline;
  }
}