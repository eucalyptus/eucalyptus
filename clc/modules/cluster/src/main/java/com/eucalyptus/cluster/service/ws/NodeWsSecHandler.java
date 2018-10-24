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
package com.eucalyptus.cluster.service.ws;

import java.util.Collection;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPConstants;
import org.apache.log4j.Logger;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSEncryptionPart;
import com.eucalyptus.cluster.common.Cluster;
import com.eucalyptus.cluster.common.ClusterRegistry;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.ws.handlers.IoWsSecHandler;
import com.eucalyptus.ws.util.CredentialProxy;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;
import com.google.common.collect.Lists;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.vavr.collection.Stream;
import io.vavr.control.Option;

/**
 *
 */
@ChannelHandler.Sharable
public class NodeWsSecHandler extends IoWsSecHandler {
  private static final Logger logger = Logger.getLogger( NodeWsSecHandler.class );

  private static final String WSA_NAMESPACE = "http://www.w3.org/2005/08/addressing";

  private static final String CACHE_SPEC =
      System.getProperty( "com.eucalyptus.cluster.service.ws.node.cacheSpec", "expireAfterWrite=60s" );

  private static final Cache<String,CredentialProxy> credentialsByPartition =
      CacheBuilder.from( CacheBuilderSpec.parse( CACHE_SPEC ) ).build( );

  public NodeWsSecHandler( ) {
    super( NodeWsSecHandler::lookupCredentials );
  }

  @Override
  public Collection<WSEncryptionPart> getSignatureParts( ) {
    return Lists.newArrayList( new WSEncryptionPart( "To", WSA_NAMESPACE, "Content" ),
        new WSEncryptionPart( "MessageID", WSA_NAMESPACE, "Content" ),
        new WSEncryptionPart( "Action", WSA_NAMESPACE, "Content" ),
        new WSEncryptionPart( WSConstants.TIMESTAMP_TOKEN_LN, WSConstants.WSU_NS, "Content" ),
        new WSEncryptionPart( SOAPConstants.BODY_LOCAL_NAME, SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI, "Content" ) );
  }

  @Override
  public boolean shouldTimeStamp( ) {
    return true;
  }

  private static CredentialProxy lookupCredentials( final ChannelHandlerContext ctx ) {
    final Option<Cluster> clusterOption = Stream.ofAll( ClusterRegistry.getInstance( ).listValues( ) )
            .appendAll( ClusterRegistry.getInstance( ).listDisabledValues( ) )
            .find( cluster -> cluster.getConfiguration( ).isHostLocal( ) );
    if ( clusterOption.isDefined( ) ) {
      final String partition = clusterOption.get( ).getPartition( );
      try {
        return credentialsByPartition.get(
            partition, ( ) -> new CredentialProxy( clusterOption.get( ).lookupPartition( ) ) );
      } catch ( Exception e ) {
        logger.error( "Error getting credentials for partition " + partition + " " + e,
            logger.isDebugEnabled( ) ? e : null );
      }
    }
    return new CredentialProxy( Eucalyptus.class );
  }
}
