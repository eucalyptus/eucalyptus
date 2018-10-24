/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.ws.server;

import java.util.Set;
import java.util.regex.Pattern;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.util.Strings;
import com.eucalyptus.ws.Handlers;
import com.eucalyptus.ws.WebServices;
import com.eucalyptus.ws.WebServicesException;
import com.eucalyptus.ws.handlers.BindingHandler;
import com.eucalyptus.ws.handlers.MessageStackHandler;
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
    if ( !WebServices.isSoapEnabled( component ) ) {
      pipeline.addLast( "disabled-soap-fault", new MessageStackHandler( ){
        @Override
        public void incomingMessage( final MessageEvent event ) throws Exception {
          throw new WebServicesException( "Service not available" );
        }
      } );
    }
    pipeline.addLast( "deserialize", Handlers.soapMarshalling( ) );
    getAuthenticationStage( ).unrollStage( pipeline );
    pipeline.addLast( "build-soap-envelope", Handlers.soapHandler( ) );
    pipeline.addLast( "binding",
        new BindingHandler( BindingHandler.context(
            BindingManager.getBinding( defaultNamespace, component ),
            Pattern.compile( namespacePattern ),
            component ) ) );
    return pipeline;
  }

  protected UnrollableStage getAuthenticationStage( ) {
    return auth;
  }

}
