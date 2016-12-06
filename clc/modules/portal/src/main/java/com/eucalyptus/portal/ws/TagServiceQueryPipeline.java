/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.portal.ws;

import java.util.EnumSet;
import org.jboss.netty.channel.ChannelPipeline;
import com.eucalyptus.auth.principal.TemporaryAccessKey;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.portal.common.Tag;
import com.eucalyptus.portal.config.TagServiceConfiguration;
import com.eucalyptus.ws.server.QueryPipeline;
import com.eucalyptus.ws.util.HmacUtils;

/**
 *
 */
@SuppressWarnings( "unused" )
@ComponentPart( Tag.class )
public class TagServiceQueryPipeline extends QueryPipeline {

  public TagServiceQueryPipeline( ) {
    super(
        "tagservice-query",
        TagServiceConfiguration.SERVICE_PATH,
        EnumSet.allOf( TemporaryAccessKey.TemporaryKeyType.class ),
        EnumSet.of( HmacUtils.SignatureVersion.SignatureV4 ) ) ;
  }

  @Override
  public ChannelPipeline addHandlers( final ChannelPipeline pipeline ) {
    super.addHandlers( pipeline );
    pipeline.addLast( "tagservice-query-binding", new TagServiceQueryBinding( ) );
    return pipeline;
  }
}
