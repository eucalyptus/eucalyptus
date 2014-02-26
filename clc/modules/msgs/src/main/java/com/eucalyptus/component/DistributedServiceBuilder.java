/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.Exceptions;

/**
 *
 */
public abstract class DistributedServiceBuilder implements ServiceBuilder<ServiceConfiguration> {
  private final ComponentId component;

  protected DistributedServiceBuilder( ComponentId component ) {
    this.component = component;
  }

  @Override
  public ComponentId getComponentId( ) {
    return this.component;
  }

  @Override
  public boolean checkAdd( String partition, String name, String host, Integer port ) throws ServiceRegistrationException {
    return true;
  }

  @Override
  public boolean checkUpdate( final String partition, final String name, final String host, final Integer port ) throws ServiceRegistrationException {
    return false;
  }

  @Override
  public ServiceConfiguration newInstance( String partition, String name, String host, Integer port ) {
    ComponentId compId = this.getComponentId( );
    try {
      return ServiceConfigurations.createEphemeral( compId, InetAddress.getByName( host ) );
    } catch ( UnknownHostException e ) {
      throw Exceptions.toUndeclared( host );
    }
  }

  @Override
  public ServiceConfiguration newInstance( ) {
    ComponentId compId = this.getComponentId( );
    return ServiceConfigurations.createEphemeral( compId );
  }

  @Override
  public void fireLoad( ServiceConfiguration parent ) throws ServiceRegistrationException {}

  @Override
  public void fireStart( ServiceConfiguration config ) throws ServiceRegistrationException {
    EventRecord.here( ServiceBuilder.class, EventType.COMPONENT_SERVICE_START, config.getFullName().toString(), config.toString() ).exhaust( );
  }

  @Override
  public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException {
    EventRecord.here( ServiceBuilder.class, EventType.COMPONENT_SERVICE_STOP, config.getFullName( ).toString( ), config.toString( ) ).exhaust( );
  }

  @Override
  public void fireEnable( ServiceConfiguration config ) throws ServiceRegistrationException {
    EventRecord.here( ServiceBuilder.class, EventType.COMPONENT_SERVICE_ENABLE, config.getFullName( ).toString( ), config.toString( ) ).exhaust( );
  }

  @Override
  public void fireDisable( ServiceConfiguration config ) throws ServiceRegistrationException {
    EventRecord.here( ServiceBuilder.class, EventType.COMPONENT_SERVICE_DISABLE, config.getFullName( ).toString( ), config.toString( ) ).exhaust( );
  }

  @Override
  public void fireCheck( ServiceConfiguration config ) throws ServiceRegistrationException {
    EventRecord.here( ServiceBuilder.class, EventType.COMPONENT_SERVICE_CHECK, config.getFullName( ).toString( ), config.toString( ) ).exhaust( );
  }
}
