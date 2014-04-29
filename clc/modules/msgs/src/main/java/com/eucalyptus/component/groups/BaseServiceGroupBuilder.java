package com.eucalyptus.component.groups;

/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 * 
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

import java.util.Collection;

import org.apache.log4j.Logger;

import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Faults;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

/**
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
public abstract class BaseServiceGroupBuilder<T extends BaseServiceGroupConfiguration> extends AbstractServiceBuilder<T> implements ServiceGroupBuilder<T> {
  private static Logger LOG = Logger.getLogger( BaseServiceGroupBuilder.class );
  
  public BaseServiceGroupBuilder( ) {}
  
  @Override
  public final boolean __MUST_EXTEND_BASE_SERVICE_BUILDER( ) {
    return true;
  }
  
  @Override
  public final ComponentId getComponentId( ) {
    return this.newInstance( ).getComponentId( );
  }
  
  @Override
  public final boolean checkAdd( String partition, String name, String host, Integer port ) throws ServiceRegistrationException {
    return super.checkAdd( partition, name, host, port );
  }
  
  @Override
  public final boolean checkUpdate( String partition, String name, String host, Integer port ) throws ServiceRegistrationException {
    return super.checkUpdate( partition, name, host, port );
  }
  
  @Override
  public final void fireLoad( ServiceConfiguration config ) throws ServiceRegistrationException {
    LOG.debug( "Loading " + config );
  }
  
  @Override
  public final void fireStart( ServiceConfiguration config ) throws ServiceRegistrationException {
    LOG.debug( "Starting " + config );
  }
  
  @Override
  public final void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException {
    LOG.debug( "Stopping " + config );
  }
  
  @Override
  public final void fireEnable( ServiceConfiguration config ) throws ServiceRegistrationException {
    LOG.debug( "Enabling " + config );
  }
  
  @Override
  public final void fireDisable( ServiceConfiguration config ) throws ServiceRegistrationException {
    LOG.debug( "Disabling " + config );
  }
  
  @Override
  public final void fireCheck( ServiceConfiguration config ) throws ServiceRegistrationException, Faults.CheckException {
    LOG.trace( "Checking " + config );
  }
  
  @Override
  public abstract T newInstance( String partition, String name, String host, Integer port );
  
  @Override
  public abstract T newInstance( );
  
  @Override
  public Collection<ServiceConfiguration> onDeregister( T config ) {
    return ServiceGroups.list( config );
  }
  
  @Override
  public Collection<ServiceConfiguration> onRegister( T config ) {
    Collection<ServiceConfiguration> members = Lists.newArrayList();
    for ( ComponentId compId : Collections2.filter( ComponentIds.list(), config.filterComponentIds() ) ) {
      members.add( config.newInstance( compId ) );
    }
    return members;
  }
  
  @Override
  public Collection<ServiceConfiguration> onUpdate( T config ) {
    return ServiceGroups.list( config );
  }

  @Override
  public Collection<ServiceConfiguration> onEnabled( T config ) {
    return ServiceGroups.list( config );
  }

  @Override
  public Collection<ServiceConfiguration> onDisabled( T config ) {
    return ServiceGroups.list( config );
  }

  @Override
  public Collection<ServiceConfiguration> onNotready( T config ) {
    return ServiceGroups.list( config );
  }

  @Override
  public Collection<ServiceConfiguration> onStarted( T config ) {
    return ServiceGroups.list( config );
  }

  @Override
  public Collection<ServiceConfiguration> onStopped( T config ) {
    return ServiceGroups.list( config );
  }
}
