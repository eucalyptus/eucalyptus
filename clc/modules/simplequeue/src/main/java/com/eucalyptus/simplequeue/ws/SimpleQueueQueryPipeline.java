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
package com.eucalyptus.simplequeue.ws;

import com.eucalyptus.auth.euare.DelegatingUserPrincipal;
import com.eucalyptus.auth.login.AuthenticationException;
import com.eucalyptus.auth.principal.PolicyVersion;
import com.eucalyptus.auth.principal.PolicyVersions;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.TemporaryAccessKey;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.util.SecurityHeader;
import com.eucalyptus.crypto.util.SecurityParameter;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.simplequeue.SimpleQueue;
import com.eucalyptus.simplequeue.config.SimpleQueueConfiguration;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import com.eucalyptus.ws.protocol.OperationParameter;
import com.eucalyptus.ws.server.QueryPipeline;
import com.eucalyptus.ws.stages.UnrollableStage;
import com.eucalyptus.ws.util.HmacUtils;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;

import javax.annotation.Nonnull;
import javax.security.auth.Subject;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Chris Grzegorczyk <grze@eucalyptus.com>
 */
@ComponentPart(SimpleQueue.class)
public class SimpleQueueQueryPipeline extends QueryPipeline {

  private final SimpleQueueAuthenticationStage auth = new SimpleQueueAuthenticationStage( super.getAuthenticationStage() );

  private static final Logger LOG = Logger.getLogger(SimpleQueueQueryPipeline.class);

  private static final Pattern pathPattern = Pattern.compile("/[0-9]{12}/[A-Za-z0-9_-]+");

  public SimpleQueueQueryPipeline(final String name) {
    super(
      name,
      SimpleQueueConfiguration.SERVICE_PATH,
      EnumSet.allOf( TemporaryAccessKey.TemporaryKeyType.class )
    );
  }

  public SimpleQueueQueryPipeline() {
    this("simplequeue-query-pipeline");
  }

  @Override
  public ChannelPipeline addHandlers( final ChannelPipeline pipeline ) {
    super.addHandlers( pipeline );
    pipeline.addLast( "simplequeue-query-binding", new SimpleQueueQueueUrlQueryBinding(pipeline) );
    return pipeline;
  }

  @Override
  protected boolean validPathForService( final String path ) {
    return super.validPathForService( path ) || pathPattern.matcher( path ).matches( );
  }

  @Override
  protected UnrollableStage getAuthenticationStage() {
    return auth;
  }


  private static class SimpleQueueAuthenticationStage implements UnrollableStage {
    private final UnrollableStage standardAuthenticationStage;

    private SimpleQueueAuthenticationStage(final UnrollableStage standardAuthenticationStage) {
      this.standardAuthenticationStage = standardAuthenticationStage;
    }

    @Override
    public void unrollStage( final ChannelPipeline pipeline ) {
      pipeline.addLast(
        "simplequeue-authentication-method-selector",
        new SimpleQueueAuthenticationHandler( standardAuthenticationStage ) );
    }

    @Override
    public String getName() {
      return "simplequeue-user-authentication";
    }

    @Override
    public int compareTo( UnrollableStage o ) {
      return this.getName( ).compareTo( o.getName( ) );
    }
  }

  public static class SimpleQueueAuthenticationHandler extends MessageStackHandler {
    private final UnrollableStage standardAuthenticationStage;

    public SimpleQueueAuthenticationHandler(final UnrollableStage standardAuthenticationStage) {
      this.standardAuthenticationStage = standardAuthenticationStage;
    }

    @Override
    public void incomingMessage( final MessageEvent event ) throws Exception {

      if ( event.getMessage( ) instanceof MappingHttpRequest) {
        boolean isHmac = true;
        final MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );
        isHmac = !HmacUtils.signatureVariantOption(httpRequest).isEmpty();

        final ChannelPipeline stagePipeline = Channels.pipeline();
        if ( isHmac ) {
          standardAuthenticationStage.unrollStage( stagePipeline );
        } else {
          stagePipeline.addLast( "simplequeue-anonymous", new AnonymousRequestHandler( ) );
        }

        final ChannelPipeline pipeline = event.getChannel().getPipeline();
        String addAfter = "simplequeue-authentication-method-selector";
        for ( final Map.Entry<String,ChannelHandler> handlerEntry : stagePipeline.toMap().entrySet() ) {
          pipeline.addAfter( addAfter, handlerEntry.getKey(), handlerEntry.getValue() );
          addAfter = handlerEntry.getKey();
        }
      }

    }
  }

  public static class AnonymousRequestHandler extends MessageStackHandler {
    @Override
    public void incomingMessage( final MessageEvent event ) throws Exception {
      if ( event.getMessage( ) instanceof MappingHttpRequest ) {
        final MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );
        final Context context = Contexts.lookup( httpRequest.getCorrelationId( ) );
        final Subject subject = new Subject( );
        final UserPrincipal principal = new DelegatingUserPrincipal( Principals.nobodyUser( ) ) {
          @Nonnull
          @Override
          public List<PolicyVersion> getPrincipalPolicies( ) {
            return ImmutableList.of( PolicyVersions.getAdministratorPolicy( ) );
          }
        };
        subject.getPrincipals( ).add( principal );
        context.setUser( principal );
        context.setSubject( subject );
      }
    }
  }
}