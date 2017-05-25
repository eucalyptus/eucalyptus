/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
import javaslang.collection.Stream;
import javaslang.control.Option;

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
            .find( cluster -> cluster.getConfiguration( ).isHostLocal( ) )
            .getOption( );
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
