/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.tokens.ws;

import static com.eucalyptus.auth.principal.TemporaryAccessKey.TemporaryKeyType;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.security.auth.Subject;
import javax.security.auth.login.CredentialExpiredException;
import javax.security.auth.login.LoginException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.auth.euare.DelegatingUserPrincipal;
import com.eucalyptus.auth.login.AccountUsernamePasswordCredentials;
import com.eucalyptus.auth.login.SecurityContext;
import com.eucalyptus.auth.principal.PolicyVersion;
import com.eucalyptus.auth.principal.PolicyVersions;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.component.id.Tokens;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.crypto.util.SecurityParameter;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import com.eucalyptus.crypto.util.SecurityHeader;
import com.eucalyptus.ws.protocol.OperationParameter;
import com.eucalyptus.ws.server.NioServerHandler;
import com.eucalyptus.ws.server.QueryPipeline;
import com.eucalyptus.ws.stages.UnrollableStage;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;

/**
 *
 */
@ComponentPart( Tokens.class )
public class TokensQueryPipeline extends QueryPipeline {
  private final TokensAuthenticationStage auth = new TokensAuthenticationStage( super.getAuthenticationStage() );

  public TokensQueryPipeline() {
    super(
        "tokens-query-pipeline",
        "/services/Tokens",
        EnumSet.of( TemporaryKeyType.Access, TemporaryKeyType.Role ) );
  }

  @Override
  public ChannelPipeline addHandlers( final ChannelPipeline pipeline ) {
    super.addHandlers( pipeline );
    pipeline.addLast( "tokens-query-binding", new TokensQueryBinding( ) );
    return pipeline;
  }

  @Override
  protected UnrollableStage getAuthenticationStage() {
    return auth;
  }

  private static class TokensAuthenticationStage implements UnrollableStage {
    private final UnrollableStage standardAuthenticationStage;

    private TokensAuthenticationStage(final UnrollableStage standardAuthenticationStage) {
      this.standardAuthenticationStage = standardAuthenticationStage;
    }

    @Override
    public void unrollStage( final ChannelPipeline pipeline ) {
      pipeline.addLast(
          "tokens-authentication-method-selector",
          new TokensAuthenticationHandler( standardAuthenticationStage ) );
    }

    @Override
    public String getName() {
      return "tokens-user-authentication";
    }

    @Override
    public int compareTo( UnrollableStage o ) {
      return this.getName( ).compareTo( o.getName( ) );
    }
  }

  public static class TokensAuthenticationHandler extends MessageStackHandler {
    private final UnrollableStage standardAuthenticationStage;

    public TokensAuthenticationHandler(final UnrollableStage standardAuthenticationStage) {
      this.standardAuthenticationStage = standardAuthenticationStage;
    }

    @Override
    public void incomingMessage( final MessageEvent event ) throws Exception {
      boolean usePasswordAuth = false;
      boolean isAnonymous = false;

      if ( event.getMessage( ) instanceof MappingHttpRequest) {
        final MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );
        isAnonymous =
            "AssumeRoleWithWebIdentity".equals( OperationParameter.getParameter( httpRequest.getParameters( ) ) );
        usePasswordAuth =
            !httpRequest.getParameters().containsKey( SecurityParameter.SignatureVersion.parameter() ) &&
            !httpRequest.getParameters().containsKey( SecurityParameter.X_Amz_Algorithm.parameter() ) &&
            !SecurityHeader.Value.AWS4_HMAC_SHA256.matches( httpRequest.getHeader( HttpHeaders.Names.AUTHORIZATION ) ) ;
      }

      final ChannelPipeline stagePipeline = Channels.pipeline();
      if ( isAnonymous ) {
          stagePipeline.addLast( "tokens-anonymous", new AnonymousRequestHandler( ) );
      } else if ( usePasswordAuth ) {
          stagePipeline.addLast( "tokens-password-authentication", new AccountUsernamePasswordHandler() );
      } else {
          standardAuthenticationStage.unrollStage( stagePipeline );
      }

      final ChannelPipeline pipeline = event.getChannel().getPipeline();
      String addAfter = "tokens-authentication-method-selector";
      for ( final Map.Entry<String,ChannelHandler> handlerEntry : stagePipeline.toMap().entrySet() ) {
        pipeline.addAfter( addAfter, handlerEntry.getKey(), handlerEntry.getValue() );
        addAfter = handlerEntry.getKey();
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

  public static class AccountUsernamePasswordHandler extends MessageStackHandler {
    private static class ChallengeException extends Exception {
      private static final long serialVersionUID = 1L;
    }

    @Override
    public void handleUpstream(final ChannelHandlerContext ctx, final ChannelEvent channelEvent) throws Exception {
      try {
        super.handleUpstream(ctx, channelEvent);
      } catch ( final ChallengeException e ) {
        sendResponse( channelEvent, "Unauthorized", HttpResponseStatus.UNAUTHORIZED, true );
      } catch ( final CredentialExpiredException e ) {
        sendResponse( channelEvent, "Expired credentials", HttpResponseStatus.FORBIDDEN, false );
      }
    }

    @Override
    public void incomingMessage( final MessageEvent event ) throws Exception {
      if ( event.getMessage( ) instanceof MappingHttpRequest) {
        final MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );

        boolean challenge = true;
        //AUTHORIZATION looks like:  username@account:password
        //CHANGEPASSWORD looks like:  username@account;password@newPassword
        // all fields before any delimiter are base64 encoded. The entire string is base64 encoded as well
        if ( httpRequest.containsHeader( HttpHeaders.Names.AUTHORIZATION ) ) {
          final String[] authorization = httpRequest.getHeader( HttpHeaders.Names.AUTHORIZATION ).split( " ", 2 );
          // in an effort to not break basic auth when doing a password change, we'll use a ';' instead of ':'
          // to indicate a new password is expected as well as encoded old password
          if ( authorization.length==2 && "basic".equalsIgnoreCase(authorization[0]) ) {
            final String unEncodedAuth = B64.standard.decString( authorization[1] );
            final boolean isChangePassword = isChangePassword( unEncodedAuth );
            final String[] basicUsernamePassword = unEncodedAuth.split( (isChangePassword?";":":"), 2 );
            final String[] encodedAccountUsername = basicUsernamePassword[0].split( "@" , 2 );

            if ( basicUsernamePassword.length==2 && encodedAccountUsername.length==2 ) {
              final String account = B64.standard.decString( encodedAccountUsername[1] );
              final String username = B64.standard.decString( encodedAccountUsername[0] );
              final String passwordSubstring = basicUsernamePassword[1];
              final String[] passwords = passwordSubstring.split( "@" , 2 );
              final String password = isChangePassword ? B64.standard.decString( passwords[0] ) : passwordSubstring;
              final String newPassword = ( isChangePassword && passwords.length == 2 ) ? passwords[1] : null;
              try {
                SecurityContext.getLoginContext(
                    new AccountUsernamePasswordCredentials( httpRequest.getCorrelationId( ), account, username, password, newPassword )
                ).login();

                challenge = false;
              } catch ( CredentialExpiredException e ){
                throw e;
              } catch ( LoginException e ){
                // Challenge user and try again
              }
            }
          }
        }

        if ( challenge ) {
          throw new ChallengeException();
        }
      }
    }

    @Override
    public void outgoingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {}

    private void sendResponse( final ChannelEvent channelEvent,
                               final String message,
                               final HttpResponseStatus status,
                               final boolean requestAuthenticate ) {
      final MappingHttpRequest httpRequest = ( MappingHttpRequest )((MessageEvent)channelEvent).getMessage( );
      final ChannelBuffer buffer = ChannelBuffers.wrappedBuffer( message.getBytes(Charsets.UTF_8) );
      final HttpResponse response =
          new DefaultHttpResponse( httpRequest.getProtocolVersion( ), status );
      response.setHeader( HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=utf8" );
      response.setHeader( HttpHeaders.Names.CONTENT_LENGTH, Integer.toString(buffer.readableBytes()) );
      if ( requestAuthenticate ) {
        response.setHeader( HttpHeaders.Names.WWW_AUTHENTICATE, "Basic realm=\"eucalyptus\"" );
      }
      response.setContent(buffer);
      final ChannelFuture writeFuture = channelEvent.getChannel().write( response );
      if ( !NioServerHandler.isPersistentConnection( httpRequest )  ) {
        writeFuture.addListener( ChannelFutureListener.CLOSE );
      }
    }

    static boolean isChangePassword( final String unEncodedAuth ) {
      boolean changePassword = false;
      if ( ( unEncodedAuth.indexOf( ";" ) > -1 )) {
        if ( unEncodedAuth.indexOf(":") == -1  || !(unEncodedAuth.indexOf(":") < unEncodedAuth.indexOf(";")))
          changePassword = true;
      }
      return changePassword;
    }
  }
}
