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
package com.eucalyptus.ws.server;

import java.util.Set;
import java.util.regex.Pattern;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.util.Strings;
import com.eucalyptus.ws.handlers.BindingHandler;
import com.eucalyptus.ws.stages.SoapUserAuthenticationStage;
import com.eucalyptus.ws.stages.UnrollableStage;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 *
 */
public abstract class SoapPipeline extends FilteredPipeline {
  private final String name;
  private final Class<? extends ComponentId> component;
  private final Set<String> servicePaths;
  private final String defaultNamespace;
  private final String namespacePattern;
  private final UnrollableStage auth = new SoapUserAuthenticationStage( ); // default, see getAuthenticationStage

  protected SoapPipeline( final String name,
                          final Class<? extends ComponentId> component,
                          final Set<String> servicePaths,
                          final String defaultNamespace,
                          final String namespacePattern ) {
    this.name = name;
    this.component = component;
    this.servicePaths = ImmutableSet.copyOf( servicePaths );
    this.defaultNamespace = defaultNamespace;
    this.namespacePattern = namespacePattern;
  }

  protected SoapPipeline( final String name,
                          final Class<? extends ComponentId> component,
                          final String servicePath,
                          final String defaultNamespace,
                          final String namespacePattern ) {
    this( name,
        component,
        ImmutableSet.of( servicePath ),
        defaultNamespace,
        namespacePattern );
  }

  protected SoapPipeline( final String name,
                          final String servicePath,
                          final String defaultNamespace,
                          final String namespacePattern ) {
    this( name, null, ImmutableSet.of( servicePath ), defaultNamespace, namespacePattern );
  }

  @Override
  public boolean checkAccepts( final HttpRequest message ) {
    final boolean usesServicePath = Iterables.any( servicePaths, Strings.isSuffixOf( message.getUri( ) ) );
    final boolean noPath = message.getUri( ).isEmpty( ) || message.getUri( ).equals( "/" ) || message.getUri( ).startsWith( "/?" );
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
            BindingManager.getBinding( defaultNamespace, component ),
            Pattern.compile( namespacePattern ),
            component ) );
    return pipeline;
  }

  protected UnrollableStage getAuthenticationStage( ) {
    return auth;
  }

}
