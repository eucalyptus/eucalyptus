package com.eucalyptus.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.address.Address;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.compute.common.internal.util.NotEnoughResourcesException;
import com.eucalyptus.compute.common.network.NetworkResource;
import com.eucalyptus.compute.common.network.NetworkingService;
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesResponseType;
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesType;
import com.eucalyptus.compute.common.network.PublicIPResource;
import com.eucalyptus.compute.common.network.ReleaseNetworkResourcesType;
import com.eucalyptus.network.config.NetworkConfigurations;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.Lists;

/**
 *
 */
public abstract class NetworkingServiceSupport implements NetworkingService {

  private final Logger logger;

  NetworkingServiceSupport( final Logger logger ) {
    this.logger = logger;
  }

  @Override
  public PrepareNetworkResourcesResponseType prepare( final PrepareNetworkResourcesType request ) {
    final ArrayList<NetworkResource> resources = Lists.newArrayList( );
    try {
      return prepareWithRollback( request, resources );
    } catch ( Exception e ) {
      resources.forEach( resource -> resource.setOwnerId( null ) );
      release( new ReleaseNetworkResourcesType( request.getVpc( ), resources ) );
      throw Exceptions.toUndeclared( e );
    }
  }

  protected abstract PrepareNetworkResourcesResponseType prepareWithRollback(
      final PrepareNetworkResourcesType request,
      final List<NetworkResource> resources
  ) throws NotEnoughResourcesException;

  @SuppressWarnings( "unused" )
  Collection<NetworkResource> preparePublicIp(
      final PrepareNetworkResourcesType request,
      final PublicIPResource publicIPResource
  ) throws NotEnoughResourcesException {
    String address = null;
    if ( publicIPResource.getValue( ) != null ) {// handle restore
      String restoreQualifier = "";
      try {
        try {
          final Address addr = Addresses.getInstance( ).lookupActiveAddress( publicIPResource.getValue( ) );
          //noinspection ConstantConditions
          if ( addr.isReallyAssigned( ) && addr.getInstanceId( ).equals( publicIPResource.getOwnerId( ) ) ) {
            address = publicIPResource.getValue( );
          }

        } catch ( NoSuchElementException ignored ) { // Address disabled
          restoreQualifier =  "(from disabled) ";
          address = Addresses.getInstance( ).allocateSystemAddress( publicIPResource.getValue( ) ).getDisplayName( );
        }

      } catch ( final Exception e ) {
        logger.error( "Failed to restore address state " + restoreQualifier + publicIPResource.getValue( ) + " for instance " + publicIPResource.getOwnerId( ) + " because of: " + e.getMessage( ) );
        Logs.extreme( ).error( e, e );
      }

    } else {
      address = Addresses.getInstance( ).allocateSystemAddress( ).getDisplayName( );
    }

    return address != null ?
        Lists.newArrayList( new PublicIPResource( publicIPResource.getOwnerId( ), address ) ) :
        Lists.newArrayList( );
  }

  protected static String mac( final String identifier ) {
    return String.format( "%s:%s:%s:%s:%s",
        NetworkConfigurations.getMacPrefix( ),
        identifier.substring( 2, 4 ),
        identifier.substring( 4, 6 ),
        identifier.substring( 6, 8 ),
        identifier.substring( 8, 10 ) ).toLowerCase( );
  }
}
