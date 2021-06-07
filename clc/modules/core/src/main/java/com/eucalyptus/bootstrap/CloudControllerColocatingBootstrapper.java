/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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

package com.eucalyptus.bootstrap;

import java.util.Set;
import com.eucalyptus.bootstrap.Bootstrapper.Simple;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceDependencyException;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.context.ServiceStateException;
import com.google.common.collect.Sets;

/**
 * Extensions of this bootstrapper will cause services of the @Provides specified component type to
 * be colocated with the currently ENABLED service of the component {@link Eucalyptus}.
 */
public abstract class CloudControllerColocatingBootstrapper extends Simple {
  private final Class<? extends ComponentId> component;
  
  protected CloudControllerColocatingBootstrapper( Class<? extends ComponentId> component ) {
    this.component = component;
    EnablingBootstrap.colocatedComponents.add( component );
  }
  
  /**
   * This check fails when the bootstrapper is executed on behalf of a service which must be
   * colocated with the cloud controller.
   * 
   * @see com.eucalyptus.bootstrap.Bootstrapper.Simple#check()
   */
  @Override
  public boolean check( ) throws Exception {
    enforceColocation( true );
    return super.check( );
  }

  @Override
  public boolean enable( ) throws Exception {
    enforceColocation( false );
    return super.check( );
  }

  private void enforceColocation( final boolean requireEnabled ) throws ServiceDependencyException {
    if ( !ComponentIds.lookup( this.component ).isManyToOnePartition( ) &&
        !Topology.isEnabledLocally( Eucalyptus.class ) &&
        ( !requireEnabled || Topology.isEnabledLocally( component ) ) ) {
      throw new ServiceDependencyException( "The "
          + ComponentIds.lookup( component ).name( )
          + " service depends upon a locally ENABLED "
          + ComponentIds.lookup( Eucalyptus.class ).name( ) );
    }
  }

  /**
   * This bootstrapper creates the services for components which are to be colocated with the cloud
   * controller.
   */
  @RunDuring( Bootstrap.Stage.RemoteServicesInit )
  @Provides( Eucalyptus.class )
  public static class EnablingBootstrap extends Bootstrapper.Simple {
    private static final Set<Class<? extends ComponentId>> colocatedComponents = Sets.newHashSet( );
    
    @Override
    public boolean enable( ) throws Exception {
      for ( Class<? extends ComponentId> compId : colocatedComponents ) {
        Components.lookup( compId ).initService( );
      }
      return super.enable( );
    }

  }
  
}
