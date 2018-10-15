/**
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cluster.service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public class ClusterServiceEnv {

  private static final String PROPERTY_CLUSTER_NATIVE_AVAILABLE = "com.eucalyptus.cluster.service.nativeInstalled";
  private static final String PROPERTY_CLUSTER_SERVICE = "com.eucalyptus.cluster.service";
  private static final String PROPERTY_SERVICE_NATIVE = "native";
  private static final String PROPERTY_SERVICE_DETECT = "auto";

  private static AtomicReference<String> beanName = new AtomicReference<>( "clusterService" );
  private static AtomicBoolean enableClusterActivities = new AtomicBoolean( true );
  private static AtomicBoolean requireProxy = new AtomicBoolean( false );

  static {
    init( );
  }

  private static void init( ) {
    final String delegateSetting = System.getProperty(PROPERTY_CLUSTER_SERVICE, PROPERTY_SERVICE_DETECT);
    final boolean useProxy;
    if ( PROPERTY_SERVICE_NATIVE.equalsIgnoreCase(delegateSetting) ) {
      useProxy = true;
    } else if ( PROPERTY_SERVICE_DETECT.equalsIgnoreCase(delegateSetting) ) {
      useProxy = Boolean.getBoolean(PROPERTY_CLUSTER_NATIVE_AVAILABLE);
    } else {
      useProxy = false;
    }

    if ( useProxy ) {
      beanName.set( "proxyClusterService" );
      enableClusterActivities.set( false );
      requireProxy.set( true );
    }
  }


  public static String beanName( ) {
    return beanName.get( );
  }

  public static boolean enableClusterActivities() {
    return enableClusterActivities.get( );
  }

  public static boolean requireProxy( ) {
    return requireProxy.get( );
  }
}
