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

package com.eucalyptus.bootstrap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import com.eucalyptus.binding.BindingCache;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.empyrean.EmpyreanService;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.system.Threads;
import com.eucalyptus.system.Threads.ThreadPool;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.LogUtil;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Mechanism for setting up and progressing through the sequence of stages the system goes through
 * during bootstrap. The bootstrap process consists
 * of two phases:
 * 
 * <ol>
 * <li><b>load()</b>: {@link SystemBootstrapper#load()}</li>
 * <li><b>start()</b> {@link SystemBootstrapper#start()}</li>
 * </ol>
 * 
 * Each phase consists of iterating through each {@link Bootstrap.Stage} and executing the
 * associated bootstrappers accordingly.
 * Implementors of {@link Bootstrapper} must declare which {@link Bootstrap.Stage} they are to be
 * executed by specifying the {@link RunDuring} annotation.
 * 
 * NOTE: It is worth noting that the {@link #start()}-phase is <b>NOT</b> executed for the
 * {@link EmpyreanService.Stage.PrivilegedConfiguration} stage. Since
 * privileges must be dropped
 * after {@link EmpyreanService.Stage.PrivilegedConfiguration}.{@link #load()} the bootstrappers
 * would no
 * longer have the indicated privileges.
 * 
 * After a call to {@link #transition()} the current stage can be obtained from
 * {@link #getCurrentStage()}.
 * 
 * Once {@link EmpyreanService.Stage.Final} is reached for {@link SystemBootstrapper#load()} the
 * {@link #getCurrentStage()} is reset to be {@link EmpyreanService.Stage.SystemInit} and
 * {@link SystemBootstrapper#start()} proceeds. Upon completing {@link SystemBootstrapper#start()}
 * the state
 * forever remains {@link EmpyreanService.Stage.Final}.
 * return {@link EmpyreanService.Stage.Final}.
 * 
 * @see Bootstrap.Stage
 * @see PrivilegedConfiguration#start()
 * @see SystemBootstrapper#init()
 * @see SystemBootstrapper#load()
 * @see SystemBootstrapper#start()
 */
public class Bootstrap {
  static Logger LOG = Logger.getLogger( Bootstrap.class );
  
  @Target( { ElementType.TYPE,
      ElementType.FIELD } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface Discovery {
    /**
     * Filter arguments to {@link Predicate#apply(Object argument)} by enforcing that {@link
     * Iterables#any(Iterable #value( ), Predicate Classes#assignable())} is true (where assignable
     * checks {@link Class#isAssignableFrom(Class argument)}) is {@code true} for the
     * {@code argument}
     * 
     * @return
     */
    Class[] value( ) default {};
    
    double priority( ) default 0.99d;

    /**
     * Filter arguments to {@link Predicate#apply(Object argument)} by enforcing that {@link
     * Iterables#any(Iterable #annotations( ), Predicate Classes#assignable())} is true (where assignable
     * checks {@link Ats#from(Object argument)#has(Class #annotations( ))}) is {@code true} for the
     * {@code argument}
     * 
     * @return
     */
    Class[] annotations( );
  }
  
  /**
   * Mechanism for setting up and progressing through the sequence of stages the system goes through
   * during bootstrap. The bootstrap process consists
   * of two phases:
   * <ol>
   * <li><b>load()</b>: {@link SystemBootstrapper#load()}</li>
   * <li><b>start()</b> {@link SystemBootstrapper#start()}</li>
   * </ol>
   * Each phase consists of iterating through each {@link Bootstrap.Stage} and executing the
   * associated bootstrappers accordingly.
   * 
   * NOTE: It is worth noting that the {@link #start()}-phase is <b>NOT</b> executed for the
   * {@link EmpyreanService.Stage.PrivilegedConfiguration} stage. Since
   * privileges must be dropped
   * after {@link EmpyreanService.Stage.PrivilegedConfiguration}.{@link #load()} the bootstrappers
   * would no
   * longer have the indicated privileges.
   * 
   * Once {@link EmpyreanService.Stage.Final} is reached for {@link SystemBootstrapper#load()} the
   * {@link #getCurrentStage()} is reset to be {@link EmpyreanService.Stage.SystemInit} and
   * {@link SystemBootstrapper#start()} proceeds. Upon completing {@link SystemBootstrapper#start()}
   * the state
   * forever remains {@link EmpyreanService.Stage.Final}.
   * return {@link EmpyreanService.Stage.Final}.
   * 
   * @see PrivilegedConfiguration#start()
   * @see SystemBootstrapper#init()
   * @see SystemBootstrapper#load()
   * @see SystemBootstrapper#start()
   */
  public enum Stage {
    SystemInit {
      /**
       * Nothing is allowed to execute during the start phase of this {@link Bootstrap.Stage}
       * 
       * @see com.eucalyptus.bootstrap.Bootstrap.Stage#start()
       */
      @Override
      public void start( ) {
        for ( Bootstrapper b : this.bootstrappers ) {
          EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_SKIPPED, this.name( ), "SKIPPING start()", b.getClass( ).getCanonicalName( ) ).warn( );
        }
      }
    },
    PrivilegedConfiguration {
      /**
       * Nothing is allowed to execute during the start phase of this {@link Bootstrap.Stage}
       * 
       * @see com.eucalyptus.bootstrap.Bootstrap.Stage#start()
       */
      @Override
      public void start( ) {
        for ( Bootstrapper b : this.bootstrappers ) {
          EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_SKIPPED, this.name( ), "SKIPPING start()", b.getClass( ).getCanonicalName( ) ).warn( );
        }
      }
    },
    UnprivilegedConfiguration,
    SystemCredentialsInit, /* <-- this means system credentials, not user. */
    RemoteConfiguration,
    DatabaseInit,
    UpgradeDatabase,
    PoolInit,
    PersistenceInit,
    RemoteServicesInit,
    UserCredentialsInit,
    CloudServiceInit,
    Final;
    public static List<Stage> list( ) {
      return Arrays.asList( Stage.values( ) );
    }
    
    protected final Set<Bootstrapper> bootstrappers         = new ConcurrentSkipListSet<Bootstrapper>( );
    private final Set<Bootstrapper>   disabledBootstrappers = new ConcurrentSkipListSet<Bootstrapper>( );
    
    void addBootstrapper( Bootstrapper b ) {
      if ( this.bootstrappers.contains( b ) ) {
        throw BootstrapException.throwFatal( "Duplicate bootstrapper registration: " + b.getClass( ).toString( ) );
      } else {
        this.bootstrappers.add( b );
      }
    }
    
    void skipBootstrapper( Bootstrapper b ) {
      if ( this.disabledBootstrappers.contains( b ) ) {
        throw BootstrapException.throwFatal( "Duplicate bootstrapper registration: " + b.getClass( ).toString( ) );
      } else {
        this.disabledBootstrappers.add( b );
      }
    }
    
    private void printAgenda( ) {
      if ( !this.bootstrappers.isEmpty( ) ) {
        LOG.info( LogUtil.header( "Bootstrap stage: " + this.name( )
                                  + "."
                                  + ( !Bootstrap.starting
                                                         ? "load()"
                                                         : "start()" ) ) );
        LOG.debug( Joiner.on( " " ).join( this.name( ) + " bootstrappers:  ", this.bootstrappers ) );
      }
    }
    
    public void updateBootstrapDependencies( ) {
      Iterable<Bootstrapper> currBootstrappers = Iterables.concat( Lists.newArrayList( this.bootstrappers ),
                                                                   Lists.newArrayList( this.disabledBootstrappers ) );
      this.bootstrappers.clear( );
      this.disabledBootstrappers.clear( );
      for ( Bootstrapper bootstrapper : currBootstrappers ) {
        try {
          if ( bootstrapper.checkLocal( ) && bootstrapper.checkRemote( ) ) {
            this.enableBootstrapper( bootstrapper );
          } else {
            this.disableBootstrapper( bootstrapper );
          }
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    }
    
    private void enableBootstrapper( Bootstrapper bootstrapper ) {
      Logs.exhaust( ).trace( EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_MARK_ENABLED, "stage=" + this.toString( ), bootstrapper.toString( ) ) );
      this.disabledBootstrappers.remove( bootstrapper );
      this.bootstrappers.add( bootstrapper );
    }
    
    private void disableBootstrapper( Bootstrapper bootstrapper ) {
      Logs.exhaust( ).trace( EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_MARK_DISABLED, "stage=" + this.toString( ), bootstrapper.toString( ) ) );
      this.bootstrappers.remove( bootstrapper );
      this.disabledBootstrappers.add( bootstrapper );
    }
    
    public void load( ) {
      this.updateBootstrapDependencies( );
      this.printAgenda( );
      for ( Bootstrapper b : this.bootstrappers ) {
        try {
          EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_LOAD, this.name( ), b.getClass( ).getCanonicalName( ) ).info( );
          boolean result = b.load( );
          if ( !result ) {
            throw BootstrapException.throwFatal( b.getClass( ).getSimpleName( ) + " returned 'false' from load( ): terminating bootstrap." );
          }
        } catch ( Exception e ) {
          EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_ERROR, this.name( ), b.getClass( ).getCanonicalName( ) ).info( );
          throw BootstrapException.throwFatal( b.getClass( ).getSimpleName( ) + " threw an error in load( ): "
                                               + e.getMessage( ), e );
        }
      }
    }
    
    public void start( ) {
      this.updateBootstrapDependencies( );
      this.printAgenda( );
      for ( Bootstrapper b : this.bootstrappers ) {
        try {
          EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_START, this.name( ), b.getClass( ).getCanonicalName( ) ).info( );
          boolean result = b.start( );
          if ( !result ) {
            throw BootstrapException.throwFatal( b.getClass( ).getSimpleName( ) + " returned 'false' from start( ): terminating bootstrap." );
          }
        } catch ( Exception e ) {
          EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_ERROR, this.name( ), b.getClass( ).getCanonicalName( ) ).info( );
          throw BootstrapException.throwFatal( b.getClass( ).getSimpleName( ) + " threw an error in start( ): "
                                               + e.getMessage( ), e );
        }
      }
    }
    
    public String describe( ) {
      StringBuffer buf = new StringBuffer( this.name( ) ).append( " " );
      for ( Bootstrapper b : this.bootstrappers ) {
        buf.append( b.getClass( ).getSimpleName( ) ).append( " " );
      }
      return buf.append( "\n" ).toString( );
    }
    
  }
  
  static Boolean         loading      = false;
  private static Boolean starting     = false;
  private static Boolean finished     = false;
  private static Stage   currentStage = Stage.SystemInit;
  static Boolean         shutdown     = false;
  
  /**
   * @return Bootstrap.currentStage
   */
  public static Stage getCurrentStage( ) {
    return currentStage;
  }
  
  /**
   * Find and run all discovery implementations (see {@link ServiceJarDiscovery}).
   * 
   * First, find all instantiable descendants of {@link ServiceJarDiscovery}.
   * Second, execute each discovery implementation.
   * <b>NOTE:</b> This method finds the available bootstrappers but does not evaluate their
   * dependency constraints.
   */
  private static void doDiscovery( ) {
    ServiceJarDiscovery.processLibraries( );
    ServiceJarDiscovery.runDiscovery( );
  }
  
  /**
   * TODO: DOCUMENT Bootstrap.java
   */
  public static void initBootstrappers( ) {
    for ( Bootstrap.Stage stage : Stage.values( ) ) {
      stage.bootstrappers.clear( );
      stage.disabledBootstrappers.clear( );
    }
    for ( Bootstrapper bootstrap : BootstrapperDiscovery.getBootstrappers( ) ) {//these have all been checked at discovery time
      try {
        Class<ComponentId> compType;
        String bc = bootstrap.getClass( ).getCanonicalName( );
        Bootstrap.Stage stage = bootstrap.getBootstrapStage( );
        compType = bootstrap.getProvides( );
        EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_INIT, stage.name( ), bc, "component=" + compType.getSimpleName( ) ).info( );
        if ( ComponentId.class.isAssignableFrom( compType ) && !Empyrean.class.equals( compType )
             && !ComponentId.class.equals( compType ) ) {
          EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_ADDED, stage.name( ), bc, "component=" + compType.getSimpleName( ) ).info( );
          Components.lookup( compType ).addBootstrapper( bootstrap );
        } else if ( Bootstrap.checkDepends( bootstrap ) ) {
          if ( Empyrean.class.equals( compType ) ) {
            EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_ADDED, stage.name( ), bc, "component=" + compType.getSimpleName( ) ).info( );
            stage.addBootstrapper( bootstrap );
          } else if ( ComponentId.class.equals( compType ) ) {
            for ( Component c : Components.list( ) ) {
              EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_ADDED, stage.name( ), bc, "component=" + c.getName( ) ).info( );
              c.addBootstrapper( bootstrap );
            }
          }
        } else {
          EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_SKIPPED, stage.name( ), bc, "component=" + compType.getSimpleName( ),
                            "localDepends=" + bootstrap.checkLocal( ), "remoteDepends=" + bootstrap.checkRemote( ) ).info( );
        }
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
      }
    }
  }
  
  private static boolean checkDepends( Bootstrapper bootstrap ) {
    String bc = bootstrap.getClass( ).getCanonicalName( );
    if ( bootstrap.checkLocal( ) && bootstrap.checkRemote( ) ) {
      return true;
    } else {
      if ( !bootstrap.checkLocal( ) ) {
        EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_SKIPPED, currentStage.name( ), bc, "DependsLocal",
                          bootstrap.getDependsLocal( ).toString( ) ).info( );
      } else if ( !bootstrap.checkRemote( ) ) {
        EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_SKIPPED, currentStage.name( ), bc, "DependsRemote",
                          bootstrap.getDependsRemote( ).toString( ) ).info( );
      }
      return false;
    }
  }
  
  /**
   * Subsequent calls to {@link #transition()} trigger the transition through the two-phase
   * (load/start) iteration through the {@link Bootstrap.Stage}s.
   * 
   * After a call to {@link #transition()} the current stage can be obtained from
   * {@link #getCurrentStage()}.
   * 
   * Once {@link EmpyreanService.Stage.Final} is reached for {@link SystemBootstrapper#load()} the
   * {@link #getCurrentStage()} is reset to be {@link EmpyreanService.Stage.SystemInit} and
   * {@link SystemBootstrapper#start()} proceeds. Upon completing {@link SystemBootstrapper#start()}
   * the state
   * forever remains {@link EmpyreanService.Stage.Final}.
   * return {@link EmpyreanService.Stage.Final}.
   * 
   * @return currentStage either the same as before, or the next {@link Bootstrap.Stage}.
   */
  public static synchronized Stage transition( ) {
    if ( currentStage == Stage.SystemInit && !loading
         && !starting
         && !finished ) {
      loading = true;
      starting = false;
      finished = false;
    } else if ( currentStage != null ) {
      LOG.info( LogUtil.header( "Bootstrap stage completed: " + currentStage.toString( ) ) );
      if ( Stage.Final.equals( currentStage ) ) {
        currentStage = null;
        if ( loading && !starting
             && !finished ) {
          loading = true;
          starting = true;
          finished = false;
        } else if ( loading && starting
                    && !finished ) {
          loading = true;
          starting = true;
          finished = true;
        }
        return currentStage;
      }
    }
    int currOrdinal = currentStage != null
                                          ? currentStage.ordinal( )
                                          : -1;
    for ( int i = currOrdinal + 1; i <= Stage.Final.ordinal( ); i++ ) {
      currentStage = Stage.values( )[i];
      if ( currentStage.bootstrappers.isEmpty( ) ) {
        LOG.trace( LogUtil.subheader( "Bootstrap stage skipped: " + currentStage.toString( ) ) );
        continue;
      } else {
        return currentStage;
      }
    }
    return currentStage;
  }
  
  public static Boolean isLoaded( ) {
    return starting;
  }
  
  public static Boolean isOperational( ) {
    return isFinished( ) && !isShuttingDown( );
  }
  
  public static void awaitFinished( ) {
    awaitFinished( Long.MAX_VALUE );
  }
  
  public static void awaitFinished( Long millis ) {
    try {
      while ( !finished && ( millis -= 50 ) > 0 ) {
        TimeUnit.MILLISECONDS.sleep( 50 );
      }
    } catch ( InterruptedException ex1 ) {
      Thread.currentThread( ).interrupt( );
    }
  }
  
  public static Boolean isFinished( ) {
    return finished;
  }
  
  public static Boolean isShuttingDown( ) {
    return shutdown;
  }
  
  /**
   * Prepares the system to execute the bootstrap sequence defined by {@link Bootstrap.Stage}.
   * 
   * The initialization phase needs to identify all {@link Bootstrapper} implementations available
   * locally -- this determines what components it is possible to 'bootstrap' on the current host.
   * Subsequently, component configuration is prepared and bootstrapper dependency contraints are
   * evaluated. The bootstrappers which conform to the state of the local system are associated with
   * their respective {@link EmpyreanService.State}.
   * 
   * The following steps are performed in order.
   * 
   * <ol>
   * <li><b>Component configurations</b>: Load the component configuration files from the local jar
   * files. This determines which services it is possible to start in the <tt>local</tt> context.</li>
   * 
   * <li><b>Print configurations</b>: The configuration is printed for review.</li>
   * 
   * <li><b>Discovery ({@link ServiceJarDiscovery}</b>: First, find all instantiable descendants of
   * {@code ServiceJarDiscovery}. Second, execute each discovery implementation. <b>NOTE:</b> This
   * step finds the available bootstrappers but does not evaluate their dependency constraints.</li>
   * 
   * <li><b>Print configurations</b>: The configuration is printed for review.</li>
   * <li><b>Print configurations</b>: The configuration is printed for review.</li>
   * </ol>
   * 
   * @see Component#startService(com.eucalyptus.component.ServiceConfiguration)
   * @see ServiceJarDiscovery
   * @see Bootstrap#loadConfigs
   * @see Bootstrap#doDiscovery()
   * 
   * @throws Exception
   */
  public static void init( ) throws Exception {
    
    Runtime.getRuntime( ).addShutdownHook( new Thread( ) {
      
      @Override
      public void run( ) {
        Bootstrap.shutdown = Boolean.TRUE;
      }
      
    } );
    /**
     * Populate the binding cache.  Skip it when running the upgrade.
     */
    LOG.info( LogUtil.header( "Populating binding cache." ) );
    BindingCache.compileBindings( );
    /**
     * run discovery to find (primarily) bootstrappers, msg typs, bindings, util-providers, etc. See
     * the descendants of {@link ServiceJarDiscovery}.
     * 
     * @see ServiceJarDiscovery
     */
    LOG.info( LogUtil.header( "Initializing discoverable bootstrap resources." ) );
    Bootstrap.doDiscovery( );
    
    LOG.info( LogUtil.header( "Initializing component identifiers:" ) );
    for ( ComponentId compId : ComponentIds.list( ) ) {
      Components.create( compId );
    }
    
    /**
     * Create the component stubs (but do not startService) to do dependency checks on bootstrappers
     * and satisfy any forward references from bootstrappers.
     */
    LOG.info( LogUtil.header( "Building core local services: cloudLocal=" + BootstrapArgs.isCloudController( ) ) );
    for ( Component comp : Components.whichCanLoad( ) ) {
      try {
        comp.initService( );
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
      }
    }
    
    LOG.info( LogUtil.header( "Initializing component resources:" ) );
    Bootstrap.applyTransition( Component.State.INITIALIZED, Components.whichCanLoad( ) );
    
    LOG.info( LogUtil.header( "Initializing bootstrappers." ) );
    Bootstrap.initBootstrappers( );
    
    LOG.info( LogUtil.header( "System ready: starting bootstrap." ) );
    for ( Component c : Components.list( ) ) {
      LOG.info( c.toString( ) );
    }
  }
  
  static void applyTransition( Component.State state, Iterable<Component> components ) {
    applyTransition( state, Iterables.toArray( components, Component.class ) );
  }
  
  private static void applyTransition( final Component.State state, Component... components ) {
    ThreadPool exec = Threads.lookup( Empyrean.class );
    for ( final Component component : components ) {
      ServiceConfiguration config = component.getLocalServiceConfiguration( );
      try {
        Topology.transition( state ).apply( config ).get( );
      } catch ( Exception ex ) {
        Exceptions.maybeInterrupted( ex );
        LOG.error( ex );
        Logs.extreme( ).error( ex, ex );
      }
    }
  }
  
  public static void initializeSystem( ) throws Exception {
    Groovyness.run( "initialize_cloud.groovy" );
  }
}
