/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
