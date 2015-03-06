/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.auth.euare.identity.ws;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import com.eucalyptus.auth.euare.common.identity.Identity;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.ws.Handlers;
import com.eucalyptus.ws.StackConfiguration;
import com.eucalyptus.ws.handlers.MessageStackHandler;

/**
 *
 */
@ComponentPart( Identity.class )
public class IdentityClientPipeline implements ChannelPipelineFactory {

  @Override
  public ChannelPipeline getPipeline( ) throws Exception {
    final ChannelPipeline pipeline = Channels.pipeline();
    for ( Map.Entry<String, ChannelHandler> e : Handlers.channelMonitors( TimeUnit.SECONDS, StackConfiguration.CLIENT_INTERNAL_TIMEOUT_SECS ).entrySet( ) ) {
      pipeline.addLast( e.getKey( ), e.getValue( ) );
    }
    pipeline.addLast( "decoder", Handlers.newHttpResponseDecoder( ) );
    pipeline.addLast( "aggregator", Handlers.newHttpChunkAggregator( ) );
    pipeline.addLast( "encoder", Handlers.httpRequestEncoder( ) );
    pipeline.addLast( "serializer", Handlers.soapMarshalling( ) );
    pipeline.addLast( "wssec", Handlers.internalWsSecHandler( ) );
    pipeline.addLast( "addressing", Handlers.addressingHandler( ) );
    pipeline.addLast( "soap", Handlers.soapHandler( ) );
    pipeline.addLast( "binding", Handlers.bindingHandler( "www_eucalyptus_com_ns_identity_2015_03_01" ) ); //TODO:STEVE: ? new BindingHandler( BindingManager.getBinding( "www_eucalyptus_com_ns_identity_2015_03_01"  ) )
    pipeline.addLast( "remote", new RemotePathHandler( ) );
    return pipeline;
  }

  public static final class RemotePathHandler extends MessageStackHandler {
    @Override
    public void outgoingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
      if ( event.getMessage( ) instanceof MappingHttpRequest ) {
        MappingHttpRequest httpMessage = (MappingHttpRequest) event.getMessage();
        httpMessage.setServicePath( ComponentIds.lookup( Identity.class ).getServicePath() );
        httpMessage.setUri( URI.create( httpMessage.getUri( ) ).resolve( httpMessage.getServicePath( ) ).toString( ) );
      }
    }
  }
}
