/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
package com.eucalyptus.ws.handlers;

import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import com.amazonaws.ReadLimitInfo;
import com.amazonaws.SignableRequest;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.http.HttpMethodName;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.tokens.SecurityTokenAWSCredentialsProvider;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.LockResource;
import com.eucalyptus.util.Pair;
import com.eucalyptus.ws.IoMessage;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;

/**
 * Outbound hmac signature v4 for internal use.
 *
 * If we switch to netty 4.x for inbound then this could extend HmacHandler /
 * IoHmacHandler and also cover inbound.
 */
@Sharable
public class IoInternalHmacHandler extends ChannelOutboundHandlerAdapter {

  private static final long SESSION_DURATION = TimeUnit.MINUTES.toMillis( 15 );
  private static final long PRE_EXPIRY = TimeUnit.MINUTES.toMillis( 5 );
  private static final long EXPIRY_OFFSET = TimeUnit.MINUTES.toMillis( 1 );

  private final Lock credentialsRefreshLock = new ReentrantLock( );
  private final AtomicReference<Pair<Long, AWSCredentials>> credentialsRef = new AtomicReference<>( );
  private final Supplier<User> systemUserSupplier =
      Suppliers.memoizeWithExpiration( systemUserSupplier( ), 1, TimeUnit.MINUTES );
  private final SecurityTokenAWSCredentialsProvider provider =
      new SecurityTokenAWSCredentialsProvider( systemUserSupplier, (int)SESSION_DURATION/1000, 0 );

  @Override
  public void write(
      final ChannelHandlerContext ctx,
      final Object msg,
      final ChannelPromise promise
  ) throws Exception {
    if ( msg instanceof IoMessage && ((IoMessage)msg).isRequest( ) ) {
      final IoMessage ioMessage = (IoMessage) msg;
      final FullHttpRequest httpRequest = (FullHttpRequest) ioMessage.getHttpMessage( );
      sign( httpRequest );
    }
    super.write( ctx, msg, promise );
  }

  private void sign( final FullHttpRequest request ) {
    final AWS4Signer signer = new AWS4Signer( );
    signer.setRegionName( "eucalyptus" );
    signer.setServiceName( "internal" );
    signer.sign( wrapRequest( request ), credentials( ) );
  }

  private SignableRequest<?> wrapRequest( final FullHttpRequest request ) {
    return new SignableRequest( ) {
      @Override
      public Map<String, String> getHeaders( ) {
        return request.headers( ).entries( ).stream( )
            .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) );
      }

      @Override
      public String getResourcePath( ) {
        return request.getUri( );
      }

      @Override
      public Map<String, List<String>> getParameters( ) {
        return Collections.emptyMap( );
      }

      @Override
      public URI getEndpoint( ) {
        return URI.create( "http://" + request.headers().get( HttpHeaders.Names.HOST ) );
      }

      @Override
      public HttpMethodName getHttpMethod( ) {
        return HttpMethodName.fromValue( request.getMethod( ).name( ) );
      }

      @Override
      public int getTimeOffset( ) {
        return 0;
      }

      @Override
      public InputStream getContent( ) {
        return new ByteBufInputStream( request.content( ).slice( ) );
      }

      @Override
      public InputStream getContentUnwrapped( ) {
        return getContent( );
      }

      @Override
      public ReadLimitInfo getReadLimitInfo( ) {
        return ( ) -> request.content( ).readableBytes( );
      }

      @Override
      public Object getOriginalRequestObject( ) {
        throw new RuntimeException( "Not supported" );
      }

      @Override
      public void addHeader( final String s, final String s1 ) {
        request.headers( ).set( s, s1 );
      }

      @Override
      public void addParameter( final String s, final String s1 ) {
        throw new RuntimeException( "Not supported" );
      }

      @Override
      public void setContent( final InputStream inputStream ) {
        throw new RuntimeException( "Not supported" );
      }
    };
  }

  private AWSCredentials credentials() {
    final Pair<Long, AWSCredentials> credentialsPair = credentialsRef.get( );
    if ( credentialsPair == null || credentialsPair.getLeft( ) < expiry(EXPIRY_OFFSET) ) {
      // no credentials or they have expired, must wait for new
      try ( final LockResource lock = LockResource.lock( credentialsRefreshLock ) ) {
        return perhapsRefreshCredentials( );
      }
    } else if ( credentialsPair.getLeft( ) < expiry(PRE_EXPIRY) ) {
      // credentials pre-expired, refresh if no one else is doing so
      try ( final LockResource lock = LockResource.tryLock( credentialsRefreshLock ) ) {
        if ( lock.isLocked( ) ) {
          return perhapsRefreshCredentials( );
        } else {
          return credentialsPair.getRight( );
        }
      }
    } else {
      return credentialsPair.getRight( );
    }
  }

  /**
   * Caller must hold credentials lock.
   */
  private AWSCredentials perhapsRefreshCredentials() {
    final Pair<Long, AWSCredentials> credentialsPair = credentialsRef.get( );
    if ( credentialsPair != null && credentialsPair.getLeft( ) > expiry(EXPIRY_OFFSET) ) {
      return credentialsPair.getRight( );
    } else {
      provider.refresh( );
      final Pair<Long, AWSCredentials> newCredentialsPair =
          Pair.of( System.currentTimeMillis( ) + SESSION_DURATION, provider.getCredentials( ) );
      credentialsRef.set( newCredentialsPair );
      return newCredentialsPair.getRight( );
    }
  }

  private long expiry( final long offset ) {
    return System.currentTimeMillis( ) + offset;
  }

  private Supplier<User> systemUserSupplier( ) {
    return ( ) -> {
      try {
        return Accounts.lookupSystemAccountByAlias( AccountIdentifiers.SYSTEM_ACCOUNT );
      } catch ( final Exception e ) {
        throw Exceptions.toUndeclared( e );
      }
    };
  }
}
