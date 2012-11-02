/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.component;

import java.net.InetAddress;
import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrap.Stage;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.CanBootstrap;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.fsm.Automata;
import com.eucalyptus.util.fsm.StateMachine;
import com.eucalyptus.util.fsm.TransitionException;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.notNullValue;

public class Component implements HasName<Component> {
  private static Logger               LOG = Logger.getLogger( Component.class );
  public final ComponentId            identity;
  private final ServiceRegistry       serviceRegistry;
  private final ComponentBootstrapper bootstrapper;
  
  public enum State implements Automata.State<State>, Predicate<ServiceConfiguration> {
    BROKEN,
    PRIMORDIAL,
    INITIALIZED,
    LOADED,
    STOPPED,
    NOTREADY,
    DISABLED,
    ENABLED;
    
    @Override
    public boolean apply( ServiceConfiguration input ) {
      return this.equals( input.lookupState( ) );
    }
  }
  
  public enum Transition implements Automata.Transition<Transition> {
    INITIALIZING,
    LOAD,
    START,
    READY_CHECK,
    STOP,
    STOPPING_NOTREADY,
    STOPPING_BROKEN,
    ENABLE,
    ENABLED_CHECK,
    DISABLE,
    DISABLED_CHECK,
    DESTROY,
    FAILED_TO_PREPARE,
    RELOAD,
    REMOVING;
  }
  
  Component( ComponentId componentId ) throws ServiceRegistrationException {
    this.identity = componentId;
    this.serviceRegistry = new ServiceRegistry( );
    this.bootstrapper = new ComponentBootstrapper( this );
  }
  
  /**
   * @return the identity
   */
  public ComponentId getComponentId( ) {
    return this.identity;
  }
  
  public State getState( ) {
    return this.hasLocalService( )
      ? this.getLocalServiceConfiguration( ).lookupState( )
      : State.PRIMORDIAL;
  }
  
  /**
   * Get the name of this component. This is the proper short name; e.g., 'eucalyptus', 'walrus',
   * etc, as used in the META-INF descriptor file.
   * 
   * @return Component name
   */
  public String getName( ) {
    return this.identity.name( );
  }
  
  public ServiceBuilder<? extends ServiceConfiguration> getBuilder( ) {
    return ServiceBuilders.lookup( this.identity.getClass( ) );
  }
  
  public NavigableSet<ServiceConfiguration> services( ) {
    return this.serviceRegistry.getServices( );
  }
  
  public Boolean hasLocalService( ) {
    return this.serviceRegistry.hasLocalService( );
  }
  
  public Boolean isEnabledLocally( ) {
    return this.serviceRegistry.hasLocalService( ) && State.ENABLED.equals( this.getLocalServiceConfiguration( ).lookupState( ) );
  }
  
  public Boolean isRunningLocally( ) {
    return this.isEnabledLocally( );
  }
  
  /**
   * @return the bootstrapper
   */
  public CanBootstrap getBootstrapper( ) {
    return this.bootstrapper;
  }
  
  public ServiceConfiguration lookup( String name ) {
    return this.serviceRegistry.getService( name );
  }
  
  /**
   * Builds a BasicService instance for this component using the local default
   * values.
   * 
   * @return BasicService instance of the service
   * @throws ServiceRegistrationException
   */
  public ServiceConfiguration initService( ) {
    if ( !this.identity.isAvailableLocally( ) ) {
      throw Exceptions.toUndeclared( "The component " + this.getName( ) + " is not being loaded automatically." );
    } else {
      return initRemoteService( Internets.localHostInetAddress( ) );
    }
  }
  
  /**
   * Builds a BasicService instance for this cloudLocal component when Eucalyptus is remote.
   * 
   * @return BasicService instance of the service
   * @throws ServiceRegistrationException
   */
  public ServiceConfiguration initRemoteService( InetAddress addr ) {
    ServiceConfiguration config = this.getBuilder( ).newInstance( this.getComponentId( ).getPartition( ), addr.getHostAddress( ), addr.getHostAddress( ),
                                                                  this.getComponentId( ).getPort( ) );
    BasicService ret = this.serviceRegistry.register( config );
    Logs.extreme( ).debug( "Initializing remote service for host " + addr
               + " with configuration: "
               + config );
    return config;
  }
  
  void destroy( final ServiceConfiguration configuration ) throws ServiceRegistrationException {
    try {
      BasicService service = null;
      if ( this.serviceRegistry.hasService( configuration ) ) {
        service = this.serviceRegistry.lookup( configuration );
      }
      try {
        EventRecord.caller( Component.class, EventType.COMPONENT_SERVICE_DESTROY, this.getName( ), configuration.getFullName( ),
                            ServiceUris.remote( configuration ).toASCIIString( ) ).info( );
        this.serviceRegistry.deregister( configuration );
      } catch ( Exception ex ) {
        throw new ServiceRegistrationException( "Failed to destroy service: " + configuration
                                                + " because of: "
                                                + ex.getMessage( ), ex );
      }
    } catch ( NoSuchElementException ex ) {
      throw new ServiceRegistrationException( "Failed to find service corresponding to: " + configuration, ex );
    }
  }
  
  /**
   * Builds a BasicService instance for this component using the provided service
   * configuration.
   * 
   * @throws ServiceRegistrationException
   */
  public void setup( final ServiceConfiguration config ) throws IllegalStateException {
    if ( ( config.isVmLocal( ) || config.isHostLocal( ) ) && !this.serviceRegistry.hasLocalService( ) ) {
      this.serviceRegistry.register( config );
    } else if ( this.serviceRegistry.hasService( config ) ) {
      this.serviceRegistry.lookup( config );
    } else {
      this.serviceRegistry.register( config );
    }
  }
  
  public boolean hasService( ServiceConfiguration config ) {
    return this.serviceRegistry.hasService( config );
  }
  
  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString( ) {
    return String.format( "Component %s=%s service=%s\n",
                          this.identity.name( ),
                          ( this.identity.isAvailableLocally( )
                            ? ""
                            : "not" ) + "available",
                          ( this.serviceRegistry.hasLocalService( )
                            ? this.serviceRegistry.getLocalService( )
                            : "not-local" ) );
  }
  
  /**
   * Components are ordered by the lexicographic ordering of the name.
   * 
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo( Component that ) {
    return this.getName( ).compareTo( that.getName( ) );
  }
  
  /**
   * @see java.lang.Object#hashCode()
   * @return
   */
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result
             + ( ( this.identity == null )
               ? 0
               : this.identity.hashCode( ) );
    return result;
  }
  
  /**
   * @see java.lang.Object#equals(java.lang.Object)
   * @param obj
   * @return
   */
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) {
      return true;
    }
    if ( obj == null ) {
      return false;
    }
    if ( getClass( ) != obj.getClass( ) ) {
      return false;
    }
    Component other = ( Component ) obj;
    if ( this.identity == null ) {
      if ( other.identity != null ) {
        return false;
      }
    } else if ( !this.identity.equals( other.identity ) ) {
      return false;
    }
    return true;
  }
  
  class ServiceRegistry {
    private final AtomicReference<BasicService>                     localService = new AtomicReference( null );
    private final ConcurrentMap<ServiceConfiguration, BasicService> services     = Maps.newConcurrentMap( );
    
    public boolean hasLocalService( ) {
      return this.localService.get( ) != null;
    }
    
    public BasicService getLocalService( ) {
      BasicService ret = this.localService.get( );
      if ( ret == null ) {
        throw Exceptions.error( new NoSuchElementException( "Attempt to access a local service reference when none exists for: " + Component.this.toString( ) ) );
      } else {
        return ret;
      }
    }
    
    /**
     * Obtain a snapshot of the current service state. Note that this method creates a new set and
     * changes to the returned set will not be reflected in the underlying services set.
     * 
     * @return {@link NavigableSet<Service>} of the registered service of this {@link Component}
     *         type.
     */
    public NavigableSet<ServiceConfiguration> getServices( ) {
      return Sets.newTreeSet( this.services.keySet( ) );
    }
    
    /**
     * Deregisters the service with the provided {@link FullName}.
     * 
     * @param fullName
     * @return {@link Service} instance of the deregistered service.
     * @throws NoSuchElementException if no {@link Service} is registered with the provided
     *           {@link FullName}
     */
    public BasicService deregister( ServiceConfiguration config ) throws NoSuchElementException {
      BasicService ret = this.services.remove( config );
      if ( ret == null ) {
        throw new NoSuchElementException( "Failed to lookup service corresponding to full-name: " + config );
      } else if ( config.isVmLocal( ) ) {
        this.localService.compareAndSet( ret, null );
      }
      return ret;
    }
    
    /**
     * Returns the {@link Service} instance which was registered with the provided
     * {@link ServiceConfiguration}, if it exists. If a service with the given name
     * does not exist a
     * NoSuchElementException is thrown.
     * 
     * @see #lookup(FullName)
     * @param configuration
     * @return {@link Service} corresponding to provided the {@link ServiceConfiguration}
     * @throws NoSuchElementException
     */
    public BasicService lookup( ServiceConfiguration config ) throws NoSuchElementException {
      if ( !this.services.containsKey( config ) ) {
        throw new NoSuchElementException( "Failed to lookup service corresponding to service configuration: " + config.getName( ) );
      } else {
        return this.services.get( config );
      }
    }
    
    /**
     * Register the given {@link Service} with the registry. Only used internally.
     * 
     * @param service
     * @throws ServiceRegistrationException
     */
    BasicService register( ServiceConfiguration config ) {
      BasicService service = this.services.containsKey( config ) ? this.services.get( config ) : new BasicService( config );
      if ( config.isVmLocal( ) || config.isHostLocal( ) ) {
        this.localService.set( service );
      }
      BasicService ret = this.services.putIfAbsent( config, service );
      if ( ret == null ) {
        ret = service;
        try {
          config.lookupStateMachine( ).transition( Component.State.INITIALIZED ).get( );
          EventRecord.caller( Component.class, EventType.COMPONENT_SERVICE_REGISTERED,
                              Component.this.getName( ),
                              ( config.isVmLocal( ) || config.isHostLocal( ) )
                                ? "local"
                                : "remote",
                              config.toString( ) ).info( );
        } catch ( Exception ex ) {
          Logs.extreme( ).error( ex, ex );
        }
      } else {
        if ( ret.getStateMachine( ).getState( ).ordinal( ) < Component.State.INITIALIZED.ordinal( ) ) {
          try {
            config.lookupStateMachine( ).transition( Component.State.INITIALIZED ).get( );
            EventRecord.caller( Component.class, EventType.COMPONENT_SERVICE_REGISTERED,
                                Component.this.getName( ),
                                ( config.isVmLocal( ) || config.isHostLocal( ) )
                                  ? "local"
                                  : "remote",
                                config.toString( ) ).info( );
          } catch ( Exception ex ) {
            Logs.extreme( ).error( ex, ex );
          }
        }
      }
      return ret;
    }
    
    /**
     * Returns the {@link Service} instance which was registered with the provided service name if
     * it
     * exists. If a service with the given name does not exist a NoSuchElementException is thrown.
     * 
     * @param name - the name used to register the specific service.
     * @return
     * @throws NoSuchElementException
     * @deprecated {@link #getServices(ServiceConfiguration)}
     */
    public ServiceConfiguration getService( String name ) throws NoSuchElementException {
      checkParam( name, notNullValue() );
      for ( ServiceConfiguration s : this.services.keySet( ) ) {
        if ( s.getName( ).equals( name ) ) {
          return s;
        }
      }
      throw new NoSuchElementException( "No service found matching name: " + name
                                        + " for component: "
                                        + Component.this.getName( ) );
    }
    
    /**
     * TODO: DOCUMENT Component.java
     * 
     * @param config
     * @return
     */
    public boolean hasService( ServiceConfiguration config ) {
      return this.services.containsKey( config );
    }
  }
  
  public ServiceConfiguration getLocalServiceConfiguration( ) {
    return this.serviceRegistry.getLocalService( ).getServiceConfiguration( );
  }
  
  /**
   * @param componentConfiguration
   * @return
   */
  public StateMachine<ServiceConfiguration, State, Transition> getStateMachine( ServiceConfiguration conf ) {
    return this.serviceRegistry.lookup( conf ).getStateMachine( );
  }
  
  public void addBootstrapper( Bootstrapper bootstrapper ) {
    this.bootstrapper.addBootstrapper( bootstrapper );
  }
  
  public boolean load( ) {
    return this.bootstrapper.load( );
  }
  
  public boolean start( ) {
    return this.bootstrapper.start( );
  }
  
  public boolean enable( ) {
    return this.bootstrapper.enable( );
  }
  
  public boolean stop( ) {
    return this.bootstrapper.stop( );
  }
  
  public void destroy( ) {
    this.bootstrapper.destroy( );
  }
  
  public boolean disable( ) {
    return this.bootstrapper.disable( );
  }
  
  public boolean check( ) {
    return this.bootstrapper.check( );
  }
  
  public List<Bootstrapper> getBootstrappers( ) {
    return this.bootstrapper.getBootstrappers( );
  }
  
  static class ComponentBootstrapper implements CanBootstrap {
    private final Multimap<Bootstrap.Stage, Bootstrapper> bootstrappers;
    private final Multimap<Bootstrap.Stage, Bootstrapper> disabledBootstrappers;
    private final Component                               component;
    
    ComponentBootstrapper( Component component ) {
      super( );
      this.component = component;
      Multimap<Bootstrap.Stage, Bootstrapper> a = ArrayListMultimap.create( );
      this.bootstrappers = Multimaps.synchronizedMultimap( a );
      Multimap<Bootstrap.Stage, Bootstrapper> b = ArrayListMultimap.create( );
      this.disabledBootstrappers = Multimaps.synchronizedMultimap( b );
    }
    
    public void addBootstrapper( Bootstrapper bootstrapper ) {
      if ( Stage.PrivilegedConfiguration.equals( bootstrapper.getBootstrapStage( ) ) ) {
        EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_SKIPPED, "stage:" + bootstrapper.getBootstrapStage( ).toString( ),
                          this.component.getComponentId( ).name( ),
                          bootstrapper.getClass( ).getName( ),
                          "component=" + this.component.getComponentId( ).name( ) ).exhaust( );
      } else {
        EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_ADDED, "stage:" + bootstrapper.getBootstrapStage( ).toString( ),
                          this.component.getComponentId( ).name( ),
                          bootstrapper.getClass( ).getName( ),
                          "component=" + this.component.getComponentId( ).name( ) ).exhaust( );
        this.bootstrappers.put( bootstrapper.getBootstrapStage( ), bootstrapper );
      }
    }
    
    private void updateBootstrapDependencies( ) {
      Iterable<Bootstrapper> currBootstrappers = Iterables.concat( Lists.newArrayList( this.bootstrappers.values( ) ),
                                                                                   Lists.newArrayList( this.disabledBootstrappers.values( ) ) );
      this.bootstrappers.clear( );
      this.disabledBootstrappers.clear( );
      for ( Bootstrapper bootstrapper : currBootstrappers ) {
        try {
          Bootstrap.Stage stage = bootstrapper.getBootstrapStage( );
          if ( bootstrapper.checkLocal( ) && bootstrapper.checkRemote( ) ) {
            this.enableBootstrapper( stage, bootstrapper );
          } else {
            this.disableBootstrapper( stage, bootstrapper );
          }
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    }
    
    private void enableBootstrapper( Bootstrap.Stage stage, Bootstrapper bootstrapper ) {
      EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_MARK_ENABLED, "stage:", stage.toString( ), this.component.getComponentId( ).name( ),
                        bootstrapper.getClass( ).getName( ), "component=" + this.component.getComponentId( ).name( ) ).exhaust( );
      this.disabledBootstrappers.remove( stage, bootstrapper );
      this.bootstrappers.put( stage, bootstrapper );
    }
    
    private void disableBootstrapper( Bootstrap.Stage stage, Bootstrapper bootstrapper ) {
      EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_MARK_DISABLED, "stage:" + stage.toString( ), this.component.getComponentId( ).name( ),
                        bootstrapper.getClass( ).getName( ), "component=" + this.component.getComponentId( ).name( ) ).exhaust( );
      this.bootstrappers.remove( stage, bootstrapper );
      this.disabledBootstrappers.put( stage, bootstrapper );
    }
    
    private boolean doTransition( EventType transition, Function<Bootstrapper, Boolean> checkedFunction ) {
      return doTransition( transition, checkedFunction, Functions.forPredicate( ( Predicate ) Predicates.alwaysTrue( ) ) );
    }
    
    private boolean doTransition( EventType transition, Function<Bootstrapper, Boolean> checkedFunction, Function<Bootstrapper, Boolean> rollbackFunction ) {
      String name = transition.name( ).replaceAll( ".*_", "" ).toLowerCase( );
      List<Bootstrapper> rollbackBootstrappers = Lists.newArrayList( );
      this.updateBootstrapDependencies( );
      for ( Stage s : Bootstrap.Stage.values( ) ) {
        for ( Bootstrapper b : Lists.newArrayList( this.bootstrappers.get( s ) ) ) {
          EventRecord.here( this.component.getClass( ), transition, this.component.getComponentId( ).name( ), "stage", s.name( ),
                            b.getClass( ).getCanonicalName( ) ).extreme( );
          TransitionException ex = null;
          try {
            if ( checkedFunction.apply( b ) ) {
              rollbackBootstrappers.add( b );
            } else {
              ex = new TransitionException( b.getClass( ).getSimpleName( ) + "."
                + name
                + "( ): returned false, terminating bootstrap for component: "
                + this.component.getName( ) );
            }
          } catch ( Exception e ) {
            LOG.error( e );
            Logs.extreme( ).error( e, e );
            ex = new TransitionException( b.getClass( ).getSimpleName( ) + "."
              + name
              + "( ): failed because of: "
              + e.getMessage( )
              + ", terminating bootstrap for component: "
              + this.component.getName( ), e );
          }
          if ( ex != null ) {
            for ( Bootstrapper rollback : Lists.reverse( rollbackBootstrappers ) ) {
              try {
                rollbackFunction.apply( rollback );
              } catch ( Exception ex1 ) {
                LOG.error( ex1 );
                Logs.extreme( ).error( ex1, ex1 );
              }
            }
            throw ex;
          }
        }
      }
      return true;
    }
    
    enum BootstrapperTransition implements Function<Bootstrapper, Boolean> {
      LOAD {
        @Override
        public Boolean runBootstrapper( Bootstrapper arg0 ) throws Exception {
          return arg0.load( );
        }
      },
      START {
        @Override
        public Boolean runBootstrapper( Bootstrapper arg0 ) throws Exception {
          return arg0.start( );
        }
      },
      ENABLE {
        @Override
        public Boolean runBootstrapper( Bootstrapper arg0 ) throws Exception {
          return arg0.enable( );
        }
      },
      DISABLE {
        @Override
        public Boolean runBootstrapper( Bootstrapper arg0 ) throws Exception {
          return arg0.disable( );
        }
      },
      STOP {
        @Override
        public Boolean runBootstrapper( Bootstrapper arg0 ) throws Exception {
          try {
            arg0.stop( );
          } catch ( Exception ex ) {
            LOG.error( ex, ex );
          }
          return true;
        }
      },
      DESTROY {
        @Override
        public Boolean runBootstrapper( Bootstrapper arg0 ) throws Exception {
          try {
            arg0.destroy( );
          } catch ( Exception ex ) {
            LOG.error( ex, ex );
          }
          return true;
        }
      },
      CHECK {
        @Override
        public Boolean runBootstrapper( Bootstrapper arg0 ) throws Exception {
          return arg0.check( );
        }
      };
      
      public abstract Boolean runBootstrapper( Bootstrapper arg0 ) throws Exception;
      
      @Override
      public Boolean apply( Bootstrapper input ) {
        try {
          return this.runBootstrapper( input );
        } catch ( Exception ex ) {
          throw Exceptions.toUndeclared( ex );
        }
      }
    }
    
    @Override
    public boolean load( ) {
      return this.doTransition( EventType.BOOTSTRAPPER_LOAD, BootstrapperTransition.LOAD, BootstrapperTransition.DESTROY );
    }
    
    @Override
    public boolean start( ) {
      return this.doTransition( EventType.BOOTSTRAPPER_START, BootstrapperTransition.START, BootstrapperTransition.STOP );
    }
    
    @Override
    public boolean enable( ) {
      return this.doTransition( EventType.BOOTSTRAPPER_ENABLE, BootstrapperTransition.ENABLE, BootstrapperTransition.DISABLE );
    }
    
    @Override
    public boolean stop( ) {
      return this.doTransition( EventType.BOOTSTRAPPER_ENABLE, BootstrapperTransition.STOP );
    }
    
    @Override
    public void destroy( ) {
      this.doTransition( EventType.BOOTSTRAPPER_ENABLE, BootstrapperTransition.DESTROY );
    }
    
    @Override
    public boolean disable( ) {
      return this.doTransition( EventType.BOOTSTRAPPER_ENABLE, BootstrapperTransition.DISABLE );
    }
    
    @Override
    public boolean check( ) {
      return this.doTransition( EventType.BOOTSTRAPPER_ENABLE, BootstrapperTransition.CHECK );
    }
    
    public List<Bootstrapper> getBootstrappers( ) {
      return Lists.newArrayList( this.bootstrappers.values( ) );
    }
    
    @Override
    public String toString( ) {
      return Joiner.on( "\n" ).join( this.bootstrappers.values( ) );
    }
    
  }
}
