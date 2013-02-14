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
package com.eucalyptus.cloudwatch.ws;

import java.util.regex.Pattern;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpRequest;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.cloudwatch.CloudWatch;
import com.eucalyptus.ws.handlers.BindingHandler;
import com.eucalyptus.ws.server.FilteredPipeline;
import com.eucalyptus.ws.stages.SoapUserAuthenticationStage;
import com.eucalyptus.ws.stages.UnrollableStage;

@ComponentId.ComponentPart( CloudWatch.class )
public class CloudWatchSoapPipeline extends FilteredPipeline {
  private static final String DEFAULT_CLOUDWATCH_SOAP_NAMESPACE = "http://monitoring.amazonaws.com/doc/2010-08-01/";
  private final UnrollableStage auth = new SoapUserAuthenticationStage( );

  @Override
  public boolean checkAccepts( final HttpRequest message ) {
    return message.getUri( ).endsWith( "/services/CloudWatch" ) && message.getHeaderNames().contains( "SOAPAction" );
  }

  @Override
  public String getName( ) {
    return "cloudwatch-soap";
  }

  @Override
  public ChannelPipeline addHandlers( ChannelPipeline pipeline ) {
    auth.unrollStage( pipeline );
    pipeline.addLast( "binding",
        new BindingHandler(
            BindingManager.getBinding( DEFAULT_CLOUDWATCH_SOAP_NAMESPACE ),
            Pattern.compile( "http://monitoring.amazonaws.com/doc/\\d\\d\\d\\d-\\d\\d-\\d\\d/" ) )
    );
    return pipeline;
  }
}
