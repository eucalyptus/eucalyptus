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
package com.eucalyptus.ws.server;

import java.util.regex.Pattern;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.ws.handlers.BindingHandler;
import com.eucalyptus.ws.stages.SoapUserAuthenticationStage;
import com.eucalyptus.ws.stages.UnrollableStage;

/**
 *
 */
public abstract class SoapPipeline extends FilteredPipeline {
  private final String name;
  private final String servicePath;
  private final String defaultNamespace;
  private final String namespacePattern;
  private final UnrollableStage auth = new SoapUserAuthenticationStage( ); // default, see getAuthenticationStage

  protected SoapPipeline( final String name,
                          final String servicePath,
                          final String defaultNamespace,
                          final String namespacePattern ) {
    this.name = name;
    this.servicePath = servicePath;
    this.defaultNamespace = defaultNamespace;
    this.namespacePattern = namespacePattern;
  }

  @Override
  public boolean checkAccepts( final HttpRequest message ) {
    final boolean usesServicePath = message.getUri( ).endsWith( servicePath );
    final boolean noPath = message.getUri( ).isEmpty( ) || message.getUri( ).equals( "/" );
    return
        message.getHeaderNames().contains( "SOAPAction" ) &&
            ( usesServicePath ||
                ( noPath && resolvesByHost( message.getHeader( HttpHeaders.Names.HOST ) ) ) );
  }

  @Override
  public String getName( ) {
    return name;
  }

  @Override
  public ChannelPipeline addHandlers( ChannelPipeline pipeline ) {
    getAuthenticationStage( ).unrollStage( pipeline );
    pipeline.addLast( "binding",
        new BindingHandler(
            BindingManager.getBinding( defaultNamespace ),
            Pattern.compile( namespacePattern ) ) );
    return pipeline;
  }

  protected UnrollableStage getAuthenticationStage( ) {
    return auth;
  }

}
