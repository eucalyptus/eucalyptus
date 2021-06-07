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
package com.eucalyptus.compute.vpc;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.address.Address;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.compute.common.internal.util.ResourceAllocationException;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vm.VmInstance.VmStateSet;
import com.eucalyptus.compute.common.internal.vm.VmNetworkConfig;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaceAssociation;
import com.eucalyptus.compute.common.network.NetworkResource;
import com.eucalyptus.compute.common.network.Networking;
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesResultType;
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesType;
import com.eucalyptus.compute.common.network.PublicIPResource;
import com.eucalyptus.compute.common.network.ReleaseNetworkResourcesType;
import com.eucalyptus.compute.common.network.VpcNetworkInterfaceResource;
import com.eucalyptus.network.config.NetworkConfigurations;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.dns.DomainNames;
import com.eucalyptus.vm.VmInstances;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 *
 */
public class NetworkInterfaceHelper {

  private static final Logger logger = Logger.getLogger( NetworkInterfaceHelper.class );

  public static String mac( final String identifier ) {
    return String.format( "%s:%s:%s:%s:%s",
        NetworkConfigurations.getMacPrefix( ),
        identifier.substring( 4, 6 ),
        identifier.substring( 6, 8 ),
        identifier.substring( 8, 10 ),
        identifier.substring( 10, 12 ) ).toLowerCase( );
  }

  public static String allocate(
      final String vpcId,
      final String subnetId,
      final String networkInterfaceId,
      final String mac,
      final String privateIp
  ) throws ResourceAllocationException {
    try {
      PrepareNetworkResourcesResultType result = Networking.getInstance( ).prepare( new PrepareNetworkResourcesType(
          vpcId,
          subnetId,
          Lists.newArrayList( new VpcNetworkInterfaceResource( networkInterfaceId, networkInterfaceId, mac, privateIp ) )
      ) );

      VpcNetworkInterfaceResource resource = null;
      if ( result != null && result.getResources( ) != null && !result.getResources( ).isEmpty( ) ) {
        resource = (VpcNetworkInterfaceResource) result.getResources( ).get( 0 );
      }

      String allocatedIp = null;
      if ( resource != null ) {
        allocatedIp = resource.getPrivateIp( );
      }

      if ( allocatedIp == null ) {
        throw new ResourceAllocationException( "Address (" + privateIp + ") not available" );
      }

      return allocatedIp;
    } catch ( final Exception e ) {
      Exceptions.findAndRethrow( e, ResourceAllocationException.class );
      throw Exceptions.toUndeclared( e );
    }
  }

  /**
   * Caller must have open transaction on ENI
   */
  public static void associate( final Address address, final NetworkInterface networkInterface ) {
    associate( address, networkInterface, Optional.fromNullable( networkInterface.getInstance( ) ) );
  }

  /**
   * Caller must have open transaction on ENI / instance
   */
  public static void associate(
      final Address address,
      final NetworkInterface networkInterface,
      final Optional<VmInstance> instanceOption
  ) {
    Addresses.getInstance( ).assign( address, networkInterface );
    if ( instanceOption.isPresent( ) && VmStateSet.RUN.apply( instanceOption.get( ) ) ) {
      Addresses.getInstance( ).start( address, instanceOption.get( ) );
    }

    networkInterface.associate( NetworkInterfaceAssociation.create(
        address.getAssociationId( ),
        address.getAllocationId( ),
        address.getOwnerAccountNumber( ),
        address.getDisplayName( ),
        networkInterface.getVpc( ).getDnsHostnames( ) ?
            VmInstances.dnsName(
                address.getDisplayName( ),
                VmInstances.dnsPublicPrefix( ),
                DomainNames.externalSubdomain( ) ) :
            null ) );
    if ( instanceOption.isPresent( ) &&
        networkInterface.isAttached( ) && networkInterface.getAttachment( ).getDeviceIndex( ) == 0 ) {
      VmInstances.updatePublicAddress( instanceOption.get( ), address.getDisplayName( ) );
    }
  }

  public static void releasePublic( final NetworkInterface networkInterface ) {
    try {
      ArrayList<NetworkResource> resources = Lists.newArrayList( );

      if ( networkInterface.isAssociated( ) ) try {
        Address address = Addresses.getInstance( ).lookupActiveAddress( networkInterface.getAssociation( ).getPublicIp( ) );
        try {
          if ( address.isStarted( ) ) {
            Addresses.getInstance( ).stop( address );
          }

        } catch ( final Exception e1 ) {
          logger.error( "Error stopping address \'" + networkInterface.getAssociation( ).getPublicIp( ) + "\' for interface \'" + networkInterface.getDisplayName( ) + "\' clean up.", e1 );
        }

        try {
          Addresses.getInstance( ).unassign( address, null );
        } catch ( final Exception e2 ) {
          logger.error( "Error unassiging address \'" + networkInterface.getAssociation( ).getPublicIp( ) + "\' for interface \'" + networkInterface.getDisplayName( ) + "\'.", e2 );
        }

        if ( address.isSystemOwned( ) ) {
          resources.add( new PublicIPResource( networkInterface.getAssociation( ).getPublicIp( ) ) );
        }

      } catch ( NoSuchElementException e ) {
        logger.warn( "Address \'" + networkInterface.getAssociation( ).getPublicIp( ) + "\' not found when releasing \'" + networkInterface.getDisplayName( ) + "\'" );
      }

      if ( !resources.isEmpty( ) ) {
        Networking.getInstance( ).release( new ReleaseNetworkResourcesType(
            networkInterface.getVpc( ).getDisplayName( ),
            resources
        ) );
      }

    } catch ( final Exception ex ) {
      logger.error( "Error releasing public address \'" + networkInterface.getPrivateIpAddress( ) + "\' for interface \'" + networkInterface.getDisplayName( ) + "\'.", ex );
    }
  }

  public static void start( final NetworkInterface networkInterface, final VmInstance instance ) {
    if ( networkInterface.isAssociated( ) ) try {
      Address address = Addresses.getInstance( ).lookupActiveAddress( networkInterface.getAssociation( ).getPublicIp( ) );
      try {
        Addresses.getInstance( ).start( address, instance );
      } catch ( final Exception e ) {
        logger.error( "Error starting address \'" + networkInterface.getAssociation( ).getPublicIp( ) + "\' for interface \'" + networkInterface.getDisplayName( ) + ", instance " + instance.getDisplayName( ) + "\'.", e );
      }

    } catch ( NoSuchElementException e ) {
      logger.warn( "Address \'" + networkInterface.getAssociation( ).getPublicIp( ) + "\' not found when stopping \'" + networkInterface.getDisplayName( ) + ", instance " + instance.getDisplayName( ) + "\'" );
    }
  }

  public static void stop( final NetworkInterface networkInterface ) {
    if ( networkInterface.isAssociated( ) ) try {
      Address address = Addresses.getInstance( ).lookupActiveAddress( networkInterface.getAssociation( ).getPublicIp( ) );
      try {
        if ( address.isStarted( ) ) {
          Addresses.getInstance( ).stop( address );
        }

      } catch ( final Exception e ) {
        logger.error( "Error stopping address \'" + networkInterface.getAssociation( ).getPublicIp( ) + "\' for interface \'" + networkInterface.getDisplayName( ) + "\'.", e );
      }

    } catch ( NoSuchElementException e ) {
      logger.warn( "Address \'" + networkInterface.getAssociation( ).getPublicIp( ) + "\' not found when stopping \'" + networkInterface.getDisplayName( ) + "\'" );
    }
  }

  public static void release( final NetworkInterface networkInterface ) {
    try {
      ArrayList<NetworkResource> resources = Lists.newArrayList( );

      if ( !Strings.isNullOrEmpty( networkInterface.getPrivateIpAddress( ) ) &&
          !VmNetworkConfig.DEFAULT_IP.equals( networkInterface.getPrivateIpAddress( ) ) ) {
        resources.add( new VpcNetworkInterfaceResource(
            null,
            networkInterface.getDisplayName( ),
            networkInterface.getMacAddress( ),
            networkInterface.getPrivateIpAddress( ) ) );
      }

      if ( networkInterface.isAssociated( ) ) try {
        Address address = Addresses.getInstance( ).lookupActiveAddress( networkInterface.getAssociation( ).getPublicIp( ) );
        try {
          if ( address.isStarted( ) ) {
            Addresses.getInstance( ).stop( address );
          }

        } catch ( final Exception e1 ) {
          logger.error( "Error stopping address \'" + networkInterface.getAssociation( ).getPublicIp( ) + "\' for interface \'" + networkInterface.getDisplayName( ) + "\' clean up.", e1 );
        }

        try {
          Addresses.getInstance( ).unassign( address, null );
        } catch ( final Exception e2 ) {
          logger.error( "Error unassiging address \'" + networkInterface.getAssociation( ).getPublicIp( ) + "\' for interface \'" + networkInterface.getDisplayName( ) + "\' clean up.", e2 );
        }

        if ( address.isSystemOwned( ) ) {
          resources.add( new PublicIPResource( networkInterface.getAssociation( ).getPublicIp( ) ) );
        }

      } catch ( NoSuchElementException e ) {
        logger.warn( "Address \'" + networkInterface.getAssociation( ).getPublicIp( ) + "\' not found when releasing \'" + networkInterface.getDisplayName( ) + "\'" );
      }


      if ( !resources.isEmpty( ) ) {
        Networking.getInstance( ).release( new ReleaseNetworkResourcesType(
            networkInterface.getVpc( ).getDisplayName( ),
            resources
        ) );
      }

    } catch ( final Exception ex ) {
      logger.error( "Error releasing private address \'" + networkInterface.getPrivateIpAddress( ) + "\' for interface \'" + networkInterface.getDisplayName( ) + "\' clean up.", ex );
    }
  }
}
