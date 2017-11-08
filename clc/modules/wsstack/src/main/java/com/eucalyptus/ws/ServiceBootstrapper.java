/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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

package com.eucalyptus.ws;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Faults;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.Topology;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import java.util.concurrent.atomic.AtomicBoolean;

@Provides( Empyrean.class )
@RunDuring( Bootstrap.Stage.RemoteServicesInit )
public class ServiceBootstrapper extends Bootstrapper.Simple {
  private static Logger    LOG                           = Logger.getLogger( ServiceBootstrapper.class );

  private static final int NUM_SERVICE_BOOTSTRAP_WORKERS =
      MoreObjects.firstNonNull( Ints.tryParse( System.getProperty( "com.eucalyptus.bootstrap.serviceWorkers", "" ) ), 40 );
                                                                                                          
  static class ServiceBootstrapWorker {
    private static final ConcurrentMap<ServiceBootstrapWorker.Worker, Runnable> workers  = Maps.newConcurrentMap( );
    private static final Runnable                                               IDLE     = new Runnable( ) {
                                                                                           @Override
                                                                                           public String toString( ) {
                                                                                             return "IDLE";
                                                                                           }
                                                                                           @Override
                                                                                           public void run( ) {}
                                                                                         };
    private static final AtomicBoolean                                          running  = new AtomicBoolean( true );
    private static final BlockingQueue<Runnable>                                msgQueue = new LinkedBlockingQueue<>( );
    private static final ExecutorService                                        executor = Executors.newCachedThreadPool(
                                                                                           Threads.threadFactory(
                                                                                             "web-services-bootstrap-%d"
                                                                                           ) );
    private static final ServiceBootstrapWorker                                 worker   = new ServiceBootstrapWorker( );

    private ServiceBootstrapWorker( ) {
      for ( int i = 0; i < NUM_SERVICE_BOOTSTRAP_WORKERS; i++ ) {
        executor.submit( new Worker( i ) );
      }
    }
    
    public static void markFinished( ) {
      running.set( false );
      executor.shutdownNow( );
    }
    
    public static void submit( final Runnable run ) {
      LOG.info( run );
      if ( !running.get( ) ) {
        throw new IllegalStateException( "Worker has been stopped: " + ServiceBootstrapWorker.class );
      } else {
        msgQueue.add( run );
      }
    }
    
    class Worker implements Runnable, Comparable<Worker> {
      private final String name;
      
      Worker( final int number ) {
        super( );
        this.name = "service-bootstrap-worker-" + number;
        workers.put( this, IDLE );
      }
      
      @Override
      public void run( ) {
        while ( !msgQueue.isEmpty( ) || running.get( ) ) {
          Runnable event;
          try {
            if ( ( event = msgQueue.poll( Long.MAX_VALUE, TimeUnit.MILLISECONDS ) ) != null ) {
              try {
                workers.replace( this, event );
                event.run( );
              } finally {
                workers.replace( this, IDLE );
              }
            }
          } catch ( final InterruptedException e ) {
            Thread.currentThread( ).interrupt( );
            break;
          } catch ( final Throwable e ) {
            Exceptions.trace( e );
          }
        }
        LOG.debug( "Finished servicing bootstrap registration request queue: " + this.toString( ) );
      }
      
      @Override
      public String toString( ) {
        final StringBuilder builder = new StringBuilder( );
        builder.append( "ServiceBootstrapWorker" )
               .append( " " )
               .append( this.name )
               .append( " work: " )
               .append( workers.get( this ) )
               .append( " thread=" )
               .append( Thread.currentThread( ).toString( ) );
        return builder.toString( );
      }
      
      /**
       * @see java.lang.Comparable#compareTo(java.lang.Object)
       */
      @Override
      public int compareTo( final Worker o ) {
        return this.name.compareTo( o.name );
      }
      
      @Override
      public int hashCode( ) {
        final int prime = 31;
        int result = 1;
        result = prime * result
                 + this.getOuterType( ).hashCode( );
        result = prime * result
                 + ( ( this.name == null )
                   ? 0
                   : this.name.hashCode( ) );
        return result;
      }
      
      @Override
      public boolean equals( final Object obj ) {
        if ( this == obj ) {
          return true;
        }
        if ( obj == null ) {
          return false;
        }
        if ( this.getClass( ) != obj.getClass( ) ) {
          return false;
        }
        final Worker other = ( Worker ) obj;
        if ( !this.getOuterType( ).equals( other.getOuterType( ) ) ) {
          return false;
        }
        if ( this.name == null ) {
          if ( other.name != null ) {
            return false;
          }
        } else if ( !this.name.equals( other.name ) ) {
          return false;
        }
        return true;
      }
      
      private ServiceBootstrapWorker getOuterType( ) {
        return ServiceBootstrapWorker.this;
      }
      
    }
    
    static void waitAll( ) {
      try {
        while ( !msgQueue.isEmpty( ) ) {
          for ( final Map.Entry<Worker,Runnable> entry : workers.entrySet( ) ) {
            if ( entry.getValue( ) == IDLE ) continue;
            LOG.info( "Waiting for " + entry.getKey( ) );
          }
          TimeUnit.MILLISECONDS.sleep( 200 );
        }
      } catch ( final InterruptedException ex ) {
        Thread.currentThread( ).interrupt( );
      }
    }
  }
  
  enum ShouldLoad implements Predicate<ServiceConfiguration> {
    INSTANCE;
    
    @Override
    public boolean apply( final ServiceConfiguration config ) {
      boolean ret = config.getComponentId( ).isAlwaysLocal( ) || config.isVmLocal( )
                          || ( BootstrapArgs.isCloudController( ) && config.getComponentId( ).isCloudLocal( ) )
                          || Hosts.isCoordinator( );
      LOG.debug( "ServiceBootstrapper.shouldLoad(" + config.toString( )
                 + "):"
                 + ret );
      return ret;
    }
  }
  
  @Override
  public boolean load( ) {
    WebServices.restart( );
    ServiceBootstrapper.execute( config -> {
      ServiceBootstrapWorker.submit( new Runnable( ) {
        @Override
        public void run( ) {
          try {
            Components.lookup( config.getComponentId( ) ).setup( config );
            if ( config.lookupState( ).ordinal( ) < State.LOADED.ordinal( ) ) {
              Topology.transition( State.LOADED ).apply( config );
            }
          } catch ( final Exception ex ) {
            Faults.failure( config, ex );
          }
        }

        @Override
        public String toString( ) {
          return "ServiceBootstrap.load(): " + config.getFullName( );
        }

      } );
      return true;
    } );
    ServiceBootstrapWorker.waitAll( );
    return true;
  }
  
  @Override
  public boolean start( ) throws Exception {
    ServiceBootstrapper.execute( config -> {
      ServiceBootstrapWorker.submit( new Runnable( ) {
        @Override
        public void run( ) {
          try {
            Topology.transition( State.DISABLED ).apply( config );
          } catch ( final Exception ex ) {
            Exceptions.maybeInterrupted( ex );
          }
        }

        @Override
        public String toString( ) {
          return "ServiceBootstrap.start(): " + config.getFullName( );
        }

      } );
      return true;
    } );
    try {
      ServiceBootstrapWorker.waitAll( );
    } finally {
      ServiceBootstrapWorker.markFinished( );
    }
    return true;
  }
  
  private static void execute( final Predicate<ServiceConfiguration> predicate ) throws NoSuchElementException {
    for ( final ComponentId compId : ComponentIds.list( ) ) {
      final Component comp = Components.lookup( compId );
      if ( compId.isRegisterable( ) ) {
        for ( final ServiceConfiguration config : Iterables.filter( ServiceConfigurations.list( compId.getClass( ) ), ShouldLoad.INSTANCE ) ) {
          try {
            predicate.apply( config );
          } catch ( final Exception ex ) {
            Exceptions.trace( ex );
          }
        }
      } else if ( comp.hasLocalService( ) ) {
        final ServiceConfiguration config = comp.getLocalServiceConfiguration( );
        if ( config.isVmLocal( ) || ( BootstrapArgs.isCloudController( ) && config.isHostLocal( ) ) ) {
          try {
            predicate.apply( config );
          } catch ( final Exception ex ) {
            Exceptions.trace( ex );
          }
        }
      }
    }
  }
}
