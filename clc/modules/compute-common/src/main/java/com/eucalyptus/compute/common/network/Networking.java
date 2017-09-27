package com.eucalyptus.compute.common.network;

import java.util.ServiceLoader;
import java.util.Set;
import javax.annotation.Nonnull;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;

/**
 * Client for networking service (with majesty)
 */
public class Networking {

  private static final Networking networking = new Networking( );
  private final NetworkingService service =
      Iterators.get( ServiceLoader.load( NetworkingService.class ).iterator( ), 0 );

  public static Networking getInstance( ) {
    return networking;
  }

  public boolean supports( final NetworkingFeature feature ) {
    return describeFeatures( ).contains( feature );
  }

  @Nonnull
  public Set<NetworkingFeature> describeFeatures( ) {
    Set<NetworkingFeature> features = Sets.newHashSet( );
    DescribeNetworkingFeaturesResponseType response = service.describeFeatures( new DescribeNetworkingFeaturesType( ) );
    if ( response != null &&
        response.getDescribeNetworkingFeaturesResult( ) != null &&
        response.getDescribeNetworkingFeaturesResult( ).getNetworkingFeatures( ) != null ) {
      features.addAll( response.getDescribeNetworkingFeaturesResult( ).getNetworkingFeatures( ) );
    }
    return features;
  }

  public PrepareNetworkResourcesResultType prepare( final PrepareNetworkResourcesType request ) {
    return service.prepare( request ).getPrepareNetworkResourcesResultType( );
  }

  public void release( final ReleaseNetworkResourcesType releaseNetworkResourcesType ) {
    service.release( releaseNetworkResourcesType );
  }

  public boolean update( final UpdateInstanceResourcesType updateInstanceResourcesType ) {
    boolean updated = false;
    UpdateInstanceResourcesResponseType response = service.update( updateInstanceResourcesType );
    if ( response != null && response.getUpdated( ) != null ) {
      updated = response.getUpdated( );
    }

    return updated;
  }
}
