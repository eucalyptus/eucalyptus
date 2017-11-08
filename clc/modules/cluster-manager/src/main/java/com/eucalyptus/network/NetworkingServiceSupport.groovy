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
package com.eucalyptus.network

import com.eucalyptus.address.Address
import com.eucalyptus.address.Addresses
import com.eucalyptus.compute.common.network.NetworkResource
import com.eucalyptus.compute.common.network.NetworkingService
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesResponseType
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesType
import com.eucalyptus.compute.common.network.PublicIPResource
import com.eucalyptus.compute.common.network.ReleaseNetworkResourcesType
import com.eucalyptus.network.config.NetworkConfigurations
import com.eucalyptus.records.Logs
import com.google.common.collect.Lists
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.apache.log4j.Logger

/**
 *
 */
@CompileStatic
@PackageScope
abstract class NetworkingServiceSupport implements NetworkingService {

  private final Logger logger;

  NetworkingServiceSupport( final Logger logger ) {
    this.logger = logger
  }

  @Override
  PrepareNetworkResourcesResponseType prepare( final PrepareNetworkResourcesType request ) {
    final ArrayList<NetworkResource> resources = Lists.newArrayList( )
    try {
      prepareWithRollback( request, resources )
    } catch ( Exception e ) {
      resources.each{ NetworkResource resource -> resource.ownerId = null }
      release( new ReleaseNetworkResourcesType(
          vpc: request.vpc,
          resources: resources ) );
      throw e
    }
  }

  protected abstract PrepareNetworkResourcesResponseType prepareWithRollback( final PrepareNetworkResourcesType request,
                                                                              final List<NetworkResource> resources );

  protected Collection<NetworkResource> preparePublicIp( final PrepareNetworkResourcesType request,
                                                         final PublicIPResource publicIPResource ) {
    String address = null
    if ( publicIPResource.value ) { // handle restore
      String restoreQualifier = ''
      try {
        try {
          final Address addr = Addresses.getInstance( ).lookupActiveAddress( publicIPResource.value )
          if ( addr.reallyAssigned && addr.instanceId == publicIPResource.ownerId ) {
            address = publicIPResource.value
          }
        } catch ( NoSuchElementException ignored ) { // Address disabled
          restoreQualifier = "(from disabled) "
          address = Addresses.getInstance( ).allocateSystemAddress( publicIPResource.value ).displayName;
        }
      } catch ( final Exception e ) {
        logger.error( "Failed to restore address state ${restoreQualifier}${publicIPResource.value}" +
            " for instance ${publicIPResource.ownerId} because of: ${e.message}" );
        Logs.extreme( ).error( e, e );
      }
    } else {
      address = Addresses.getInstance( ).allocateSystemAddress( ).displayName
    }

    address ?
        [ new PublicIPResource(  value: address, ownerId: publicIPResource.ownerId ) ] :
        [ ]
  }

  protected static String mac( final String identifier ) {
    String.format( "${NetworkConfigurations.macPrefix}:%s:%s:%s:%s",
        identifier.substring( 2, 4 ),
        identifier.substring( 4, 6 ),
        identifier.substring( 6, 8 ),
        identifier.substring( 8, 10 ) ).toLowerCase( );
  }



}
