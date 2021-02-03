/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancing.service.persist;

import java.util.List;
import javax.annotation.Nullable;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.loadbalancing.common.LoadBalancingMetadata.LoadBalancerMetadata;
import com.eucalyptus.loadbalancing.common.msgs.AccessLog;
import com.eucalyptus.loadbalancing.common.msgs.ConnectionDraining;
import com.eucalyptus.loadbalancing.common.msgs.ConnectionSettings;
import com.eucalyptus.loadbalancing.common.msgs.CrossZoneLoadBalancing;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerAttributes;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancer;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerPolicyDescription;
import com.eucalyptus.loadbalancing.service.persist.views.ImmutableLoadBalancerAutoScalingGroupView;
import com.eucalyptus.loadbalancing.service.persist.views.ImmutableLoadBalancerBackendInstanceView;
import com.eucalyptus.loadbalancing.service.persist.views.ImmutableLoadBalancerBackendServerDescriptionFullView;
import com.eucalyptus.loadbalancing.service.persist.views.ImmutableLoadBalancerBackendServerDescriptionView;
import com.eucalyptus.loadbalancing.service.persist.views.ImmutableLoadBalancerFullView;
import com.eucalyptus.loadbalancing.service.persist.views.ImmutableLoadBalancerListenerFullView;
import com.eucalyptus.loadbalancing.service.persist.views.ImmutableLoadBalancerListenerView;
import com.eucalyptus.loadbalancing.service.persist.views.ImmutableLoadBalancerListenersView;
import com.eucalyptus.loadbalancing.service.persist.views.ImmutableLoadBalancerPolicyAttributeDescriptionView;
import com.eucalyptus.loadbalancing.service.persist.views.ImmutableLoadBalancerPolicyDescriptionFullView;
import com.eucalyptus.loadbalancing.service.persist.views.ImmutableLoadBalancerPolicyDescriptionView;
import com.eucalyptus.loadbalancing.service.persist.views.ImmutableLoadBalancerSecurityGroupRefView;
import com.eucalyptus.loadbalancing.service.persist.views.ImmutableLoadBalancerSecurityGroupView;
import com.eucalyptus.loadbalancing.service.persist.views.ImmutableLoadBalancerServoInstanceView;
import com.eucalyptus.loadbalancing.service.persist.views.ImmutableLoadBalancerView;
import com.eucalyptus.loadbalancing.service.persist.views.ImmutableLoadBalancerZoneFullView;
import com.eucalyptus.loadbalancing.service.persist.views.ImmutableLoadBalancerZoneView;
import com.eucalyptus.loadbalancing.service.persist.views.ImmutableLoadBalancerZonesView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerFullView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerListenersView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerPolicyDescriptionFullView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerZonesView;
import com.eucalyptus.util.CompatFunction;
import com.eucalyptus.util.RestrictedTypes.QuantityMetricFunction;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import io.vavr.collection.Stream;
import io.vavr.control.Option;


public interface LoadBalancers {

  CompatFunction<LoadBalancerPolicyDescription, LoadBalancerPolicyDescriptionFullView> POLICY_DESCRIPTION_FULL_VIEW =
      policy -> ImmutableLoadBalancerPolicyDescriptionFullView.builder()
          .policyDescription( ImmutableLoadBalancerPolicyDescriptionView.copyOf( policy ) )
          .policyAttributeDescriptions( Stream.ofAll( policy.getPolicyAttributeDescriptions( ) ).map( ImmutableLoadBalancerPolicyAttributeDescriptionView::copyOf ) )
          .build();

  CompatFunction<LoadBalancer, LoadBalancerView> CORE_VIEW = ImmutableLoadBalancerView::copyOf;

  CompatFunction<LoadBalancer, LoadBalancerZonesView> ZONES_VIEW = loadBalancer -> ImmutableLoadBalancerZonesView.builder()
      .loadBalancer( CORE_VIEW.apply( loadBalancer ) )
      .zones( Stream.ofAll( loadBalancer.getZones( ) ).map( zone -> ImmutableLoadBalancerZoneFullView.builder()
          .zone( ImmutableLoadBalancerZoneView.copyOf( zone ) )
          .backendInstances( Stream.ofAll( zone.getBackendInstances( ) ).map( ImmutableLoadBalancerBackendInstanceView::copyOf ) )
          .servoInstances( Stream.ofAll( zone.getServoInstances( ) ).map( ImmutableLoadBalancerServoInstanceView::copyOf ) )
          .build()) )
      .build();

  CompatFunction<LoadBalancer, LoadBalancerListenersView> LISTENERS_VIEW = loadBalancer -> ImmutableLoadBalancerListenersView.builder()
      .loadBalancer( CORE_VIEW.apply( loadBalancer ) )
      .listeners( Stream.ofAll( loadBalancer.getListeners( ) ).map(listener -> ImmutableLoadBalancerListenerFullView.builder()
          .listener( ImmutableLoadBalancerListenerView.copyOf( listener ) )
          .policyDescriptions( Stream.ofAll( listener.getPolicyDescriptions( ) ).map( ImmutableLoadBalancerPolicyDescriptionView::copyOf ) )
          .build() ) )
      .build();

  CompatFunction<LoadBalancer, LoadBalancerFullView> FULL_VIEW = loadBalancer -> ImmutableLoadBalancerFullView.builder()
      .loadBalancer( CORE_VIEW.apply( loadBalancer ) )
      .securityGroup( Option.of( loadBalancer.getSecurityGroup( ) ).map( ImmutableLoadBalancerSecurityGroupView::copyOf ).getOrNull( ) )
      .securityGroupRefs( Stream.ofAll( loadBalancer.getSecurityGroupRefs( ) ).map(ImmutableLoadBalancerSecurityGroupRefView::copyOf) )
      .autoScalingGroups( Stream.ofAll( loadBalancer.getAutoScaleGroups( ) ).map( ImmutableLoadBalancerAutoScalingGroupView::copyOf ) )
      .backendInstances( Stream.ofAll( loadBalancer.getBackendInstances( ) ).map( ImmutableLoadBalancerBackendInstanceView::copyOf ) )
      .backendServers( Stream.ofAll( loadBalancer.getBackendServers( ) ).map(backendServer -> ImmutableLoadBalancerBackendServerDescriptionFullView.builder()
          .backendServer( ImmutableLoadBalancerBackendServerDescriptionView.copyOf( backendServer ) )
          .policyDescriptions( Stream.ofAll( backendServer.getPolicyDescriptions() ).map( ImmutableLoadBalancerPolicyDescriptionView::copyOf ) )
          .build() ) )
      .listeners( Stream.ofAll( loadBalancer.getListeners( ) ).map(listener -> ImmutableLoadBalancerListenerFullView.builder()
          .listener( ImmutableLoadBalancerListenerView.copyOf( listener ) )
          .policyDescriptions( Stream.ofAll( listener.getPolicyDescriptions( ) ).map( ImmutableLoadBalancerPolicyDescriptionView::copyOf ) )
          .build() ) )
      .policies( Stream.ofAll( loadBalancer.getPolicyDescriptions( ) ).map( POLICY_DESCRIPTION_FULL_VIEW ) )
      .zones( Stream.ofAll( loadBalancer.getZones( ) ).map( ImmutableLoadBalancerZoneView::copyOf ) )
      .build();

  <T> T lookupByName( @Nullable OwnerFullName ownerFullName,
                      String name,
                      Predicate<? super LoadBalancer> filter,
                      Function<? super LoadBalancer,T> transform ) throws LoadBalancingMetadataException;

  <T> List<T> listByExample( LoadBalancer example,
                             Predicate<? super LoadBalancer> filter,
                             Function<? super LoadBalancer,T> transform ) throws LoadBalancingMetadataException;

  <T> T updateByExample( LoadBalancer example,
                         OwnerFullName ownerFullName,
                         String key,
                         Predicate<? super LoadBalancer> filter,
                         Function<? super LoadBalancer,T> updateTransform ) throws LoadBalancingMetadataException;

  LoadBalancer save(LoadBalancer loadBalancer) throws LoadBalancingMetadataException;

  boolean delete(LoadBalancerMetadata metadata) throws LoadBalancingMetadataException;

  @TypeMapper
  enum LoadBalancerToLoadBalancerAttributesTransform implements Function<LoadBalancerView, LoadBalancerAttributes> {
    INSTANCE;

    @Nullable
    @Override
    public LoadBalancerAttributes apply( @Nullable final LoadBalancerView loadBalancer ) {
      LoadBalancerAttributes attributes = null;
      if ( loadBalancer != null ) {
        attributes = new LoadBalancerAttributes( );

        final ConnectionDraining connectionDraining = new ConnectionDraining( );
        connectionDraining.setEnabled( false );
        attributes.setConnectionDraining( connectionDraining );

        final ConnectionSettings connectionSettings = new ConnectionSettings( );
        connectionSettings.setIdleTimeout(
            MoreObjects.firstNonNull( loadBalancer.getConnectionIdleTimeout( ), 60 ) );
        attributes.setConnectionSettings( connectionSettings );

        final CrossZoneLoadBalancing crossZoneLoadBalancing = new CrossZoneLoadBalancing( );
        crossZoneLoadBalancing.setEnabled(
            MoreObjects.firstNonNull(loadBalancer.getCrossZoneLoadbalancingEnabled(), false) );
        attributes.setCrossZoneLoadBalancing( crossZoneLoadBalancing );

        final AccessLog accessLog = new AccessLog();
        accessLog.setEnabled(MoreObjects.firstNonNull(loadBalancer.getAccessLogEnabled(), false));
        accessLog.setEmitInterval(loadBalancer.getAccessLogEmitInterval());
        accessLog.setS3BucketName(loadBalancer.getAccessLogS3BucketName());
        accessLog.setS3BucketPrefix(loadBalancer.getAccessLogS3BucketPrefix());
        attributes.setAccessLog( accessLog );
      }
      return attributes;
    }
  }

  @QuantityMetricFunction( LoadBalancerMetadata.class )
  enum CountLoadBalancers implements Function<OwnerFullName, Long> {
    INSTANCE;

    @Override
    public Long apply( final OwnerFullName input ) {
      try ( final TransactionResource db = Entities.transactionFor( LoadBalancer.class ) ) {
        return Entities.count( LoadBalancer.named( input, null ) );
      }
    }
  }

}
