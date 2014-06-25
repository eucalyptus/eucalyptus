/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.simpleworkflow.ws;

import static com.eucalyptus.auth.principal.TemporaryAccessKey.TemporaryKeyType;
import java.util.EnumSet;
import java.util.Set;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflow;
import com.eucalyptus.util.Strings;
import com.eucalyptus.ws.server.FilteredPipeline;
import com.eucalyptus.ws.stages.HmacUserAuthenticationStage;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 *
 */
@ComponentPart(SimpleWorkflow.class)
public class SimpleWorkflowPipeline extends FilteredPipeline {
  private final HmacUserAuthenticationStage auth;
  private final Set<String> servicePathPrefixes = ImmutableSet.of( "/services/SimpleWorkflow" );

  public SimpleWorkflowPipeline( ) {
    auth = new HmacUserAuthenticationStage( EnumSet.allOf( TemporaryKeyType.class ) );
  }

  @Override
  public String getName() {
    return "simpleworkflow-query-pipeline";
  }

  @Override
  public ChannelPipeline addHandlers( final ChannelPipeline pipeline ) {
    auth.unrollStage( pipeline );
    pipeline.addLast( "simpleworkflow-binding", new SimpleWorkflowBinding( ) );
    return pipeline;
  }

  @Override
  public boolean checkAccepts( final HttpRequest message ) {
    if ( message instanceof MappingHttpRequest ) {
      final boolean targetHeaderIsSwf = Strings.startsWith( "SimpleWorkflowService." ).apply( message.getHeader( "X-Amz-Target" ) );
      final boolean usesServicePath = Iterables.any( servicePathPrefixes, Strings.isPrefixOf( message.getUri() ) );
      final boolean noPath = message.getUri( ).isEmpty( ) || message.getUri( ).equals( "/" ) || message.getUri( ).startsWith( "/?" );
      return
          targetHeaderIsSwf &&
              ( usesServicePath ||
                  ( noPath && resolvesByHost( message.getHeader( HttpHeaders.Names.HOST ) ) ) );
    }
    return false;

  }
}
