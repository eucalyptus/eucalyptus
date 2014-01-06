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
package com.eucalyptus.cloudformation.ws;

import static com.eucalyptus.auth.principal.TemporaryAccessKey.TemporaryKeyType;
import java.util.EnumSet;

import org.jboss.netty.channel.ChannelPipeline;

import com.eucalyptus.cloudformation.CloudFormation;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.ws.protocol.RequiredQueryParams;
import com.eucalyptus.ws.server.QueryPipeline;


@ComponentPart(CloudFormation.class)
public class CloudFormationQueryPipeline extends QueryPipeline {
  
  public CloudFormationQueryPipeline( ) {
    super(
        "cloudformation-query-pipeline",
        "/services/CloudFormation",
        EnumSet.allOf( TemporaryKeyType.class ),
        EnumSet.of( RequiredQueryParams.Version ) );
  } 
    
  @Override
  public ChannelPipeline addHandlers( final ChannelPipeline pipeline ) {
    super.addHandlers( pipeline );
    pipeline.addLast( "cloudformation-query-binding", new CloudFormationQueryBinding() );
    return pipeline;
  }
}
