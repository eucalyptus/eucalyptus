/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.portal.ws;

import com.eucalyptus.auth.principal.TemporaryAccessKey;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.portal.common.Ec2Reports;
import com.eucalyptus.portal.config.Ec2ReportsServiceConfiguration;
import com.eucalyptus.ws.server.QueryPipeline;
import com.eucalyptus.ws.util.HmacUtils;
import org.jboss.netty.channel.ChannelPipeline;

import java.util.EnumSet;

@SuppressWarnings( "unused" )
@ComponentPart( Ec2Reports.class )
public class Ec2ReportsServiceQueryPipeline extends QueryPipeline {
  public Ec2ReportsServiceQueryPipeline() {
    super(
            "ec2reportsservice-query",
            Ec2ReportsServiceConfiguration.SERVICE_PATH,
            EnumSet.allOf( TemporaryAccessKey.TemporaryKeyType.class ),
            EnumSet.of( HmacUtils.SignatureVersion.SignatureV4 ) ) ;
  }

  @Override
  public ChannelPipeline addHandlers(final ChannelPipeline pipeline ) {
    super.addHandlers( pipeline );
    pipeline.addLast( "ec2reportsservice-query-binding", new Ec2ReportsServiceQueryBinding( ) );
    return pipeline;
  }
}
