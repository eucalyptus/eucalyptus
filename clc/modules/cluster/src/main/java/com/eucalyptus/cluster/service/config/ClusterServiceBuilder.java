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
package com.eucalyptus.cluster.service.config;

import java.util.NoSuchElementException;
import com.eucalyptus.cluster.common.Cluster;
import com.eucalyptus.cluster.common.ClusterRegistry;
import com.eucalyptus.cluster.service.ClusterServiceId;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.annotation.ComponentPart;

/**
 *
 */
@ComponentPart( ClusterServiceId.class )
public class ClusterServiceBuilder extends AbstractServiceBuilder<ClusterServiceConfiguration> {

  @Override
  public ComponentId getComponentId() {
    return ComponentIds.lookup( ClusterServiceId.class );
  }

  @Override
  public ClusterServiceConfiguration newInstance( final String partition, final String name, final String host, final Integer port ) {
    return new ClusterServiceConfiguration( partition, name, host, port );
  }

  @Override
  public ClusterServiceConfiguration newInstance() {
    return new ClusterServiceConfiguration( );
  }


  @Override
  public void fireStart( final ServiceConfiguration config ) throws ServiceRegistrationException {
    if ( !registry( ).contains( config.getName( ) ) ) {
      final Cluster newCluster = new Cluster( config );
      newCluster.start( );
    }
  }

  @Override
  public void fireStop( final ServiceConfiguration config ) throws ServiceRegistrationException {
    try {
      registry( ).lookupDisabled( config.getName( ) ).stop( );
    } catch ( final NoSuchElementException ignore ) {
    }
  }

  @Override
  public void fireEnable( final ServiceConfiguration config ) throws ServiceRegistrationException {
    try {
      registry( ).lookupDisabled( config.getName( ) ).enable( );
    } catch ( final NoSuchElementException ignore ) {
    }
  }

  @Override
  public void fireDisable( final ServiceConfiguration config ) throws ServiceRegistrationException {
    try {
      registry( ).lookup( config.getName( ) ).disable( );
    } catch ( final NoSuchElementException ignore ) {
    }
  }

  @Override
  public void fireCheck( final ServiceConfiguration config ) throws ServiceRegistrationException {
    try {
      registry( ).lookup( config.getName( ) ).enable( );
    } catch ( final NoSuchElementException e ) {
      try {
        registry( ).lookupDisabled( config.getName( ) ).enable( );
      } catch ( final NoSuchElementException ignore ) {
      }
    }
  }

  private ClusterRegistry registry() {
    return ClusterRegistry.getInstance( );
  }
}