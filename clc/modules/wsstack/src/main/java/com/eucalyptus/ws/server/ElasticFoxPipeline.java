/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.ws.server;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpRequest;
import com.eucalyptus.component.ComponentId.ComponentPart;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.ws.stages.ElasticFoxMangleStage;
import com.eucalyptus.ws.stages.UnrollableStage;

@ComponentPart( Eucalyptus.class )
public class ElasticFoxPipeline extends EucalyptusQueryPipeline {

  private final UnrollableStage mangle = new ElasticFoxMangleStage( );
  
  

  @Override
  public boolean checkAccepts( HttpRequest message ) {
    if ( message instanceof MappingHttpRequest ) {
      MappingHttpRequest httpRequest = ( MappingHttpRequest ) message;
      //FIXME: newest firefox breaks...
      String userAgent = httpRequest.getHeader( "User-Agent" );
      if( userAgent != null && userAgent.toLowerCase( ).contains( "elasticfox" ) ){
        httpRequest.setServicePath( httpRequest.getServicePath( ).replaceAll( "Eucalyptus/", "Eucalyptus" ) );
        return true;
      }
    }
    return false;
  }

  @Override
  public String getName( ) {
    return "elasticfox-"+super.getName( );
  }

  @Override
  public ChannelPipeline addHandlers( ChannelPipeline pipeline ) {
    super.addHandlers( pipeline );
    mangle.unrollStage( pipeline );
    return pipeline;
  }
  
}
