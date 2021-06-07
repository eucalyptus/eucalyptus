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
