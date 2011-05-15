/*******************************************************************************
 * Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian Dr., Goleta, CA 93101
 * USA or visit <http://www.eucalyptus.com/licenses/> if you need additional
 * information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California All rights
 * reserved.
 * 
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF s * LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE
 * PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL, COPYRIGHTED MATERIAL OR
 * PATENTED MATERIAL IN THIS SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED
 * THE PARTY DISCOVERING IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF
 * CALIFORNIA, SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE
 * REMEDY, WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED,
 * OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH ANY
 * SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.cluster;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.cluster.callback.ClusterCertsCallback;
import com.eucalyptus.cluster.callback.DisableServiceCallback;
import com.eucalyptus.cluster.callback.EnableServiceCallback;
import com.eucalyptus.cluster.callback.LogDataCallback;
import com.eucalyptus.cluster.callback.NetworkStateCallback;
import com.eucalyptus.cluster.callback.PublicAddressStateCallback;
import com.eucalyptus.cluster.callback.ResourceStateCallback;
import com.eucalyptus.cluster.callback.ServiceStateCallback;
import com.eucalyptus.cluster.callback.StartServiceCallback;
import com.eucalyptus.cluster.callback.VmPendingCallback;
import com.eucalyptus.cluster.callback.VmStateCallback;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.LifecycleEvent;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceChecks;
import com.eucalyptus.component.ServiceChecks.CheckException;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.component.id.GatherLogService;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.config.RegisterClusterType;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.crypto.util.PEMFiles;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Hertz;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.EucalyptusClusterException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.Callback;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.ConnectionException;
import com.eucalyptus.util.async.FailedRequestException;
import com.eucalyptus.util.async.RemoteCallback;
import com.eucalyptus.util.async.SubjectMessageCallback;
import com.eucalyptus.util.async.SubjectRemoteCallbackFactory;
import com.eucalyptus.util.fsm.AbstractTransitionAction;
import com.eucalyptus.util.fsm.Automata;
import com.eucalyptus.util.fsm.HasStateMachine;
import com.eucalyptus.util.fsm.StateMachine;
import com.eucalyptus.util.fsm.StateMachineBuilder;
import com.eucalyptus.util.fsm.TransitionAction;
import com.eucalyptus.util.fsm.Transitions;
import com.eucalyptus.vm.VmType;
import com.eucalyptus.ws.WebServicesException;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.ucsb.eucalyptus.cloud.NodeInfo;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.NodeCertInfo;
import edu.ucsb.eucalyptus.msgs.NodeLogInfo;
import edu.ucsb.eucalyptus.msgs.NodeType;

public class Cluster implements HasFullName<Cluster>, EventListener, HasStateMachine<Cluster, Cluster.State, Cluster.Transition> {
  /**
   * 
   */
  private static final int                               CLUSTER_STARTUP_SYNC_RETRIES = 15;
  /**
   * 
   */
  private static final long                              STATE_INTERVAL_ENABLED       = 10l;
  /**
   * 
   */
  private static final long                              STATE_INTERVAL_DISABLED      = 10l;
  /**
   * 
   */
  private static final long                              STATE_INTERVAL_NOTREADY      = 10l;
  /**
   * 
   */
  private static final long                              STATE_INTERVAL_PENDING       = 3l;
  private static Logger                                  LOG                          = Logger.getLogger( Cluster.class );
  private final StateMachine<Cluster, State, Transition> stateMachine;
  private final ClusterConfiguration                     configuration;
  private final FullName                                 fullName;
  private final ThreadFactory                            threadFactory;
  private final ConcurrentNavigableMap<String, NodeInfo> nodeMap;
  private final BlockingQueue<Throwable>                 errors                       = new LinkedBlockingDeque<Throwable>( );
  private final ClusterState                             state;
  private final ClusterNodeState                         nodeState;
  private NodeLogInfo                                    lastLog                      = new NodeLogInfo( );
  private boolean                                        hasClusterCert               = false;
  private boolean                                        hasNodeCert                  = false;
  
  enum ComponentStatePredicates implements Predicate<Cluster> {
    STARTED {
      
      @Override
      public boolean apply( final Cluster input ) {
        if ( Component.State.NOTREADY.ordinal( ) <= input.getConfiguration( ).lookupStateMachine( ).getState( ).ordinal( ) ) {
          try {
            AsyncRequests.newRequest( new StartServiceCallback( input ) ).dispatch( input.configuration ).get( );
            return true;
          } catch ( ExecutionException ex ) {
            input.errors.add( ex.getCause( ) );
            return false;
          } catch ( InterruptedException ex ) {
            input.errors.add( ex.getCause( ) );
            return false;
          }
        } else {
          return false;
        }
      }
    },
    ENABLED {
      @Override
      public boolean apply( final Cluster input ) {
        if ( Component.State.ENABLED.equals( input.getConfiguration( ).lookupStateMachine( ).getState( ) ) ) {
          try {
            AsyncRequests.newRequest( new EnableServiceCallback( input ) ).dispatch( input.configuration ).get( );
            return true;
          } catch ( ExecutionException ex ) {
            input.errors.add( ex.getCause( ) );
            return false;
          } catch ( InterruptedException ex ) {
            input.errors.add( ex.getCause( ) );
            return false;
          } finally {
            try {
              Clusters.getInstance( ).enable( input.getName( ) );
            } catch ( NoSuchElementException ex ) {
              LOG.error( ex , ex );
            }
          }
        } else {
          return false;
        }
      }
    },
    DISABLED {
      @Override
      public boolean apply( final Cluster input ) {
        if ( Component.State.DISABLED.equals( input.getConfiguration( ).lookupStateMachine( ).getState( ) ) ) {
          try {
            Clusters.getInstance( ).disable( input.getName( ) );
          } catch ( NoSuchElementException ex ) {
            LOG.error( ex , ex );
          }
          try {
            AsyncRequests.newRequest( new DisableServiceCallback( input ) ).dispatch( input.configuration ).get( );
            return true;
          } catch ( ExecutionException ex ) {
            input.errors.add( ex.getCause( ) );
            return false;
          } catch ( InterruptedException ex ) {
            input.errors.add( ex.getCause( ) );
            return false;
          }
        } else {
          return false;
        }
      }
    };
  }
  
  enum LogRefresh implements Function<Cluster, TransitionAction<Cluster>> {
    LOGS( LogDataCallback.class ),
    CERTS( ClusterCertsCallback.class );
    Class refresh;
    
    private LogRefresh( final Class refresh ) {
      this.refresh = refresh;
    }
    
    @Override
    public TransitionAction<Cluster> apply( final Cluster cluster ) {
      final SubjectRemoteCallbackFactory<RemoteCallback, Cluster> factory = newSubjectMessageFactory( this.refresh, cluster );
      return new AbstractTransitionAction<Cluster>( ) {
        
        @Override
        public final void leave( final Cluster parent, final Callback.Completion transitionCallback ) {
          try {
            AsyncRequests.newRequest( factory.newInstance( ) ).then( transitionCallback )
                         .sendSync( parent.getLogServiceConfiguration( ) );
          } catch ( final ExecutionException e ) {
            if ( e.getCause( ) instanceof FailedRequestException ) {
              LOG.error( e.getCause( ).getMessage( ) );
              parent.errors.add( e );
            } else if ( ( e.getCause( ) instanceof ConnectionException ) || ( e.getCause( ) instanceof IOException ) ) {
              LOG.error( parent.getName( ) + ": Error communicating with cluster: " + e.getCause( ).getMessage( ) );
              parent.errors.add( e );
            } else {
              LOG.error( e, e );
              parent.errors.add( e );
            }
          } catch ( final InterruptedException e ) {
            LOG.error( e, e );
            parent.errors.add( e );
          }
        }
      };
    }
  }
  
  enum Refresh implements Function<Cluster, TransitionAction<Cluster>> {
    RESOURCES( ResourceStateCallback.class ),
    NETWORKS( NetworkStateCallback.class ),
    INSTANCES( VmStateCallback.class ),
    ADDRESSES( PublicAddressStateCallback.class ),
    SERVICEREADY( ServiceStateCallback.class );
    Class refresh;
    
    private Refresh( final Class refresh ) {
      this.refresh = refresh;
    }
    
    private static final List<Class<? extends Exception>> communicationErrors = Lists.newArrayList( ConnectionException.class, IOException.class,
                                                                                                    WebServicesException.class );
    private static final List<Class<? extends Exception>> executionErrors     = Lists.newArrayList( UndeclaredThrowableException.class,
                                                                                                    ExecutionException.class );
    
    @Override
    public TransitionAction<Cluster> apply( final Cluster cluster ) {
      final SubjectRemoteCallbackFactory<RemoteCallback, Cluster> factory = newSubjectMessageFactory( this.refresh, cluster );
      return new AbstractTransitionAction<Cluster>( ) {
        
        @Override
        public final void leave( final Cluster parent, final Callback.Completion transitionCallback ) {
          try {
            AsyncRequests.newRequest( factory.newInstance( ) ).then( transitionCallback ).sendSync( parent.getConfiguration( ) );
          } catch ( final ExecutionException e ) {
            if ( e.getCause( ) instanceof FailedRequestException ) {
              LOG.error( e.getCause( ).getMessage( ) );
              parent.errors.add( e );
            } else if ( ( e.getCause( ) instanceof ConnectionException ) || ( e.getCause( ) instanceof IOException ) ) {
              LOG.error( parent.getName( ) + ": Error communicating with cluster: " + e.getCause( ).getMessage( ) );
              parent.errors.add( e );
            } else {
              LOG.error( e, e );
              parent.errors.add( e );
            }
          } catch ( final InterruptedException e ) {
            LOG.error( e, e );
            parent.errors.add( e );
          }
        }
      };
    }
  }
  
  public enum State implements Automata.State<State> {
    BROKEN, /** cannot establish initial contact with cluster because of CLC side errors **/
    STOPPED, /** Component.State.NOTREADY: cluster unreachable **/
    PENDING, /** Component.State.NOTREADY: cluster unreachable **/
    AUTHENTICATING, STARTING, STARTING_NOTREADY, /** Component.State.NOTREADY:enter() **/
    NOTREADY, /** Component.State.NOTREADY -> Component.State.DISABLED **/
    DISABLED, /** Component.State.DISABLED -> DISABLED: service ready, not current primary **/
    /** Component.State.DISABLED -> Component.State.ENABLED **/
    ENABLING, ENABLING_RESOURCES, ENABLING_NET, ENABLING_VMS, ENABLING_ADDRS, ENABLING_VMS_PASS_TWO, ENABLING_ADDRS_PASS_TWO,
    /** Component.State.ENABLED -> Component.State.ENABLED **/
    ENABLED, ENABLED_ADDRS, ENABLED_RSC, ENABLED_NET, ENABLED_VMS, ENABLED_SERVICE_CHECK,
    
  }
  
  public enum Transition implements Automata.Transition<Transition> {
    RESTART_BROKEN, PRESTART,
    /** pending setup **/
    AUTHENTICATE, START, START_CHECK, STARTING_SERVICES,
    NOTREADYCHECK,
    ENABLE, ENABLING_RESOURCES, ENABLING_NET, ENABLING_VMS, ENABLING_ADDRS, ENABLING_VMS_PASS_TWO, ENABLING_ADDRS_PASS_TWO,

    ENABLED, ENABLED_ADDRS, ENABLED_VMS, ENABLED_NET, ENABLED_SERVICES, ENABLED_RSC,

    DISABLE, DISABLEDCHECK,

    STOP,
    
  }
  
  public Cluster( final ClusterConfiguration configuration ) {
    super( );
    this.configuration = configuration;
    this.fullName = configuration.getFullName( );
    this.state = new ClusterState( configuration.getName( ) );
    this.nodeState = new ClusterNodeState( configuration.getName( ) );
    this.nodeMap = new ConcurrentSkipListMap<String, NodeInfo>( );
    this.threadFactory = Threads.lookup( com.eucalyptus.component.id.ClusterController.class, Cluster.class, this.getFullName( ).toString( ) );
    this.stateMachine = new StateMachineBuilder<Cluster, State, Transition>( this, State.PENDING ) {
      {
        final TransitionAction<Cluster> noop = Transitions.noop( );
        this.from( State.BROKEN ).to( State.PENDING ).error( State.BROKEN ).on( Transition.RESTART_BROKEN ).run( noop );
        
        this.from( State.STOPPED ).to( State.PENDING ).error( State.PENDING ).on( Transition.PRESTART ).run( noop );
        this.from( State.PENDING ).to( State.AUTHENTICATING  ).error( State.PENDING ).on( Transition.AUTHENTICATE ).run( LogRefresh.CERTS );
        this.from( State.AUTHENTICATING ).to( State.STARTING ).error( State.PENDING ).on( Transition.START ).run( Cluster.ComponentStatePredicates.STARTED );
        this.from( State.STARTING ).to( State.STARTING_NOTREADY ).error( State.PENDING ).on( Transition.START_CHECK ).run( Refresh.SERVICEREADY );
        this.from( State.STARTING_NOTREADY ).to( State.NOTREADY ).error( State.PENDING ).on( Transition.STARTING_SERVICES ).run( Refresh.SERVICEREADY );
        
        this.from( State.NOTREADY ).to( State.DISABLED ).error( State.NOTREADY ).on( Transition.NOTREADYCHECK ).run( Refresh.SERVICEREADY );
        
        this.from( State.DISABLED ).to( State.DISABLED ).error( State.NOTREADY ).on( Transition.DISABLEDCHECK ).run( Refresh.SERVICEREADY );
        this.from( State.DISABLED ).to( State.ENABLING ).error( State.DISABLED ).on( Transition.ENABLE ).run( Cluster.ComponentStatePredicates.ENABLED );
        this.from( State.DISABLED ).to( State.STOPPED ).error( State.PENDING ).on( Transition.STOP ).run( noop );
        
        this.from( State.ENABLED ).to( State.DISABLED ).error( State.NOTREADY ).on( Transition.DISABLE ).run( Cluster.ComponentStatePredicates.DISABLED );
        
        this.from( State.ENABLING ).to( State.ENABLING_RESOURCES ).error( State.NOTREADY ).on( Transition.ENABLING_RESOURCES ).run( Refresh.RESOURCES );
        this.from( State.ENABLING_RESOURCES ).to( State.ENABLING_NET ).error( State.NOTREADY ).on( Transition.ENABLING_NET ).run( Refresh.NETWORKS );
        this.from( State.ENABLING_NET ).to( State.ENABLING_VMS ).error( State.NOTREADY ).on( Transition.ENABLING_VMS ).run( Refresh.INSTANCES );
        this.from( State.ENABLING_VMS ).to( State.ENABLING_ADDRS ).error( State.NOTREADY ).on( Transition.ENABLING_ADDRS ).run( Refresh.ADDRESSES );
        this.from( State.ENABLING_ADDRS ).to( State.ENABLING_VMS_PASS_TWO ).error( State.NOTREADY ).on( Transition.ENABLING_VMS_PASS_TWO ).run( Refresh.INSTANCES );
        this.from( State.ENABLING_VMS_PASS_TWO ).to( State.ENABLING_ADDRS_PASS_TWO ).error( State.NOTREADY ).on( Transition.ENABLING_ADDRS_PASS_TWO ).run( Refresh.ADDRESSES );
        this.from( State.ENABLING_ADDRS_PASS_TWO ).to( State.ENABLED ).error( State.NOTREADY ).on( Transition.ENABLING_ADDRS_PASS_TWO ).run( Refresh.ADDRESSES );
        
        this.from( State.ENABLED ).to( State.ENABLED_SERVICE_CHECK ).error( State.NOTREADY ).on( Transition.ENABLED_SERVICES ).run( Refresh.SERVICEREADY );
        this.from( State.ENABLED_SERVICE_CHECK ).to( State.ENABLED_ADDRS ).error( State.NOTREADY ).on( Transition.ENABLED_ADDRS ).run( Refresh.ADDRESSES );
        this.from( State.ENABLED_ADDRS ).to( State.ENABLED_RSC ).error( State.NOTREADY ).on( Transition.ENABLED_RSC ).run( Refresh.RESOURCES );
        this.from( State.ENABLED_RSC ).to( State.ENABLED_NET ).error( State.NOTREADY ).on( Transition.ENABLED_NET ).run( Refresh.NETWORKS );
        this.from( State.ENABLED_NET ).to( State.ENABLED_VMS ).error( State.NOTREADY ).on( Transition.ENABLED_VMS ).run( Refresh.INSTANCES );
        this.from( State.ENABLED_VMS ).to( State.ENABLED ).error( State.NOTREADY ).on( Transition.ENABLED ).run( noop );
        
      }
    }.newAtomicMarkedState( );
  }
  
  private void fireClockTick( final Hertz tick ) {
    try {
      boolean initialized = this.configuration.lookupState( ).ordinal( ) > Component.State.LOADED.ordinal( );
      if ( !this.stateMachine.isBusy( ) ) {
        Callable<CheckedListenableFuture<Cluster>> transition = null;
        switch ( this.stateMachine.getState( ) ) {
          case PENDING:
          case STARTING:
            if ( tick.isAsserted( Cluster.STATE_INTERVAL_PENDING ) ) {
              transition = Automata.sequenceTransitions( this, State.STOPPED, State.PENDING, State.AUTHENTICATING, State.STARTING,
                                                         State.STARTING_NOTREADY,
                                                         State.NOTREADY, State.DISABLED );
            }
            break;
          case NOTREADY:
            if ( initialized && tick.isAsserted( Cluster.STATE_INTERVAL_NOTREADY ) ) {
              transition = Automata.sequenceTransitions( this, State.NOTREADY, State.DISABLED );
            }
            break;
          case DISABLED:
            if ( initialized && tick.isAsserted( Cluster.STATE_INTERVAL_DISABLED ) && Component.State.DISABLED.isIn( this.configuration ) ) {
              transition = Automata.sequenceTransitions( this, State.DISABLED, State.DISABLED );
            } else if ( initialized && tick.isAsserted( Cluster.STATE_INTERVAL_DISABLED ) && Component.State.ENABLED.isIn( this.configuration ) ) {
              transition = Automata.sequenceTransitions( this, State.ENABLING, State.ENABLING_RESOURCES, State.ENABLING_NET, State.ENABLING_VMS,
                                                         State.ENABLING_ADDRS, State.ENABLING_VMS_PASS_TWO, State.ENABLING_ADDRS_PASS_TWO, State.ENABLED );
            }
            break;
          case ENABLED:
            if ( initialized && tick.isAsserted( Cluster.STATE_INTERVAL_ENABLED ) && Component.State.ENABLED.isIn( this.configuration ) ) {
              transition = Automata.sequenceTransitions( this, State.ENABLED, State.ENABLED_SERVICE_CHECK, State.ENABLED_ADDRS, State.ENABLED_RSC,
                                                         State.ENABLED_NET, State.ENABLED_VMS, State.ENABLED );
            } else if ( initialized && Component.State.DISABLED.isIn( this.configuration ) || Component.State.NOTREADY.isIn( this.configuration ) ) {
              transition = Automata.sequenceTransitions( this, State.ENABLED, State.DISABLED );
            }
            break;
          default:
            break;
        }
        if ( transition != null ) {
          final Callable<CheckedListenableFuture<Cluster>> t = transition;
          Threads.lookup( ClusterController.class, Cluster.class ).submit( new Runnable( ) {
            
            @Override
            public void run( ) {
              try {
                t.call( ).get( );
              } catch ( Exception ex ) {
                Cluster.this.configuration.error( ex );
              }
            }
          } );
        }
      }
    } catch ( final IllegalStateException ex ) {
      Exceptions.trace( ex );
    } catch ( final Exception ex ) {
      LOG.error( ex, ex );
    }
  }
  
  public Boolean isReady( ) {
    return this.hasClusterCert && this.hasNodeCert && Bootstrap.isFinished( );
  }
  
  public X509Certificate getClusterCertificate( ) {
    try {
      return Partitions.lookup( this.configuration ).getCertificate( );
    } catch ( final ServiceRegistrationException ex ) {
      LOG.error( ex, ex );
      return null;
    }
  }
  
  public X509Certificate getNodeCertificate( ) {
    try {
      return Partitions.lookup( this.configuration ).getNodeCertificate( );
    } catch ( final ServiceRegistrationException ex ) {
      LOG.error( ex, ex );
      return null;
    }
  }
  
  @Override
  public String getName( ) {
    return this.configuration.getName( );
  }
  
  public NavigableSet<String> getNodeTags( ) {
    return this.nodeMap.navigableKeySet( );
  }
  
  public NodeInfo getNode( final String serviceTag ) {
    return this.nodeMap.get( serviceTag );
  }
  
  public void updateNodeInfo( final ArrayList<String> serviceTags ) {
    NodeInfo ret = null;
    for ( final String serviceTag : serviceTags ) {
      if ( ( ret = this.nodeMap.putIfAbsent( serviceTag, new NodeInfo( serviceTag ) ) ) != null ) {
        ret.touch( );
        ret.setServiceTag( serviceTag );
        ret.setIqn( "No IQN reported" );
      }
    }
  }
  
  public void updateNodeInfo( final List<NodeType> nodeTags ) {
    NodeInfo ret = null;
    for ( final NodeType node : nodeTags ) {
      if ( ( ret = this.nodeMap.putIfAbsent( node.getServiceTag( ), new NodeInfo( node ) ) ) != null ) {
        ret.touch( );
        ret.setServiceTag( node.getServiceTag( ) );
        ret.setIqn( node.getIqn( ) );
      }
    }
  }
  
  @Override
  public int compareTo( final Cluster that ) {
    return this.getName( ).compareTo( that.getName( ) );
  }
  
  public ClusterConfiguration getConfiguration( ) {
    return this.configuration;
  }
  
  public RegisterClusterType getWeb( ) {
    String host = this.getConfiguration( ).getHostName( );
    int port = 0;
    final URI uri = this.getConfiguration( ).getUri( );
    host = uri.getHost( );
    port = uri.getPort( );
    return new RegisterClusterType( this.getConfiguration( ).getPartition( ), this.getName( ), host, port );
  }
  
  public ClusterState getState( ) {
    return this.state;
  }
  
  public ClusterNodeState getNodeState( ) {
    return this.nodeState;
  }
  
  public void start( ) throws ServiceRegistrationException {
    Clusters.getInstance( ).registerDisabled( this );
    this.configuration.lookupService( ).getEndpoint( ).start( );//TODO:GRZE: this has a corresponding transition and needs to be removed when that is activated.
    final Callable<CheckedListenableFuture<Cluster>> transition = Automata.sequenceTransitions( Cluster.this, State.PENDING, State.AUTHENTICATING, State.STARTING,
                                                                                                State.STARTING_NOTREADY, State.NOTREADY, State.DISABLED );
    Exception error = null;
    try {
      for ( int i = 0; i < Cluster.CLUSTER_STARTUP_SYNC_RETRIES; i++ ) {
        try {
          transition.call( ).get( );
          break;
        } catch ( Exception ex ) {
          LOG.error( ex );
          error = ex;
        }
        TimeUnit.SECONDS.sleep( 1 );
      }
    } catch ( InterruptedException ex ) {
      LOG.error( ex , ex );
    } finally {
      ListenerRegistry.getInstance( ).register( ClockTick.class, Cluster.this );
      ListenerRegistry.getInstance( ).register( Hertz.class, Cluster.this );
    }
    if ( error != null ) {
      this.configuration.info( error );
    }
  }
  
  public void enable( ) throws ServiceRegistrationException {
    try {
      final Callable<CheckedListenableFuture<Cluster>> transition = Automata.sequenceTransitions( this, State.PENDING, State.AUTHENTICATING, State.STARTING, 
                                                                                                  State.STARTING_NOTREADY, State.NOTREADY,
                                                                                                  State.DISABLED,
                                                                                                  State.ENABLING, State.ENABLING_RESOURCES,
                                                                                                  State.ENABLING_NET, State.ENABLING_VMS,
                                                                                                  State.ENABLING_ADDRS, State.ENABLING_VMS_PASS_TWO,
                                                                                                  State.ENABLING_ADDRS_PASS_TWO, State.ENABLED );
      Threads.lookup( ClusterController.class, Cluster.class ).submit( transition );
    } catch ( NoSuchElementException ex ) {
      throw ex;
    }
  }
  
  public void disable( ) throws ServiceRegistrationException {
    final Callable<CheckedListenableFuture<Cluster>> transition = Automata.sequenceTransitions( this, State.ENABLED, State.DISABLED );
    Threads.lookup( ClusterController.class, Cluster.class ).submit( transition );
  }
  
  public void stop( ) throws ServiceRegistrationException {
    final Callable<CheckedListenableFuture<Cluster>> transition = Automata.sequenceTransitions( this, State.DISABLED, State.STOPPED );
    Threads.lookup( ClusterController.class, Cluster.class ).submit( transition );
    ListenerRegistry.getInstance( ).deregister( Hertz.class, this );
    ListenerRegistry.getInstance( ).deregister( ClockTick.class, this );
    this.configuration.lookupService( ).getEndpoint( ).stop( );//TODO:GRZE: this has a corresponding transition and needs to be removed when that is activated.
    Clusters.getInstance( ).deregister( this.getName( ) );
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.configuration == null )
      ? 0
      : this.configuration.hashCode( ) );
    result = prime * result + ( ( this.state == null )
      ? 0
      : this.state.hashCode( ) );
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
    final Cluster other = ( Cluster ) obj;
    if ( this.configuration == null ) {
      if ( other.configuration != null ) {
        return false;
      }
    } else if ( !this.configuration.equals( other.configuration ) ) {
      return false;
    }
    if ( this.state == null ) {
      if ( other.state != null ) {
        return false;
      }
    } else if ( !this.state.equals( other.state ) ) {
      return false;
    }
    return true;
  }
  
  public URI getUri( ) {
    return this.configuration.getUri( );
  }
  
  public String getHostName( ) {
    return this.configuration.getHostName( );
  }
  
  public String getInsecureServicePath( ) {
    return this.configuration.getInsecureServicePath( );
  }
  
  public Integer getPort( ) {
    return this.configuration.getPort( );
  }
  
  public String getServicePath( ) {
    return this.configuration.getServicePath( );
  }
  
  public ThreadFactory getThreadFactory( ) {
    return this.threadFactory;
  }
  
  @Override
  public String toString( ) {
    final StringBuilder buf = new StringBuilder( );
    buf.append( "Cluster " ).append( this.configuration ).append( '\n' );
    buf.append( "Cluster " ).append( this.configuration.getName( ) ).append( " mq=" ).append( this.getConfiguration( ).lookupService( ).getEndpoint( ) ).append( '\n' );
    for ( final NodeInfo node : this.nodeMap.values( ) ) {
      buf.append( "Cluster " ).append( this.configuration.getName( ) ).append( " node=" ).append( node ).append( '\n' );
    }
    for ( final VmType type : VmTypes.list( ) ) {
      final VmTypeAvailability avail = this.nodeState.getAvailability( type.getName( ) );
      buf.append( "Cluster " ).append( this.configuration.getName( ) ).append( " node=" ).append( avail ).append( '\n' );
    }
    return buf.toString( );
  }
  
  private final AtomicBoolean logUpdate = new AtomicBoolean( false );
  
  public NodeLogInfo getLastLog( ) {
    if ( this.logUpdate.compareAndSet( false, true ) ) {
      final Cluster self = this;
      try {
        /**
         * TODO:ASAP:GRZE: RESTORE
         * Callbacks.newRequest( new LogDataCallback( this, null ) )
         * .execute( this.getServiceEndpoint( ),
         * com.eucalyptus.component.id.Cluster.getLogClientPipeline( ) )
         * .getResponse( ).get( );
         * Callbacks.newLogRequest( new LogDataCallback( this, null ) ).dispatch(
         * this.getServiceEndpoint( ) );
         **/
      } catch ( final Throwable t ) {
        LOG.error( t, t );
      } finally {
        this.logUpdate.set( false );
      }
    }
    return this.lastLog;
  }
  
  public void clearLogPending( ) {
    this.logUpdate.set( false );
  }
  
  public NodeLogInfo getNodeLog( final String nodeIp ) throws EucalyptusClusterException {
    final NodeInfo nodeInfo = Iterables.find( this.nodeMap.values( ), new Predicate<NodeInfo>( ) {
      @Override
      public boolean apply( final NodeInfo arg0 ) {
        return nodeIp.equals( arg0.getName( ) );
      }
    } );
    if ( nodeInfo == null ) {
      throw new EucalyptusClusterException( "Error obtaining node log files for: " + nodeIp );
    }
    if ( this.logUpdate.compareAndSet( false, true ) ) {
      final Cluster self = this;
      try {
        /**
         * TODO:ASAP:GRZE: RESTORE
         * Callbacks.newRequest( new LogDataCallback( this, null ) )
         * .execute( this.getServiceEndpoint( ),
         * com.eucalyptus.component.id.Cluster.getLogClientPipeline( ) )
         * .getResponse( ).get( );
         **/
//        Callbacks.newLogRequest( new LogDataCallback( this, nodeInfo ) ).dispatch( this.getServiceEndpoint( ) );
      } catch ( final Throwable t ) {
        LOG.debug( t, t );
      } finally {
        this.logUpdate.set( false );
      }
    }
    return nodeInfo.getLogs( );
  }
  
  public void setLastLog( final NodeLogInfo lastLog ) {
    this.lastLog = lastLog;
  }
  
  public boolean checkCerts( final NodeCertInfo certs ) {
    if ( ( certs == null ) || ( certs.getCcCert( ) == null ) || ( certs.getNcCert( ) == null ) ) {
      return false;
    }
    
    final X509Certificate clusterx509 = PEMFiles.getCert( B64.standard.dec( certs.getCcCert( ) ) );
    final X509Certificate nodex509 = PEMFiles.getCert( B64.standard.dec( certs.getNcCert( ) ) );
    if ( "self".equals( certs.getServiceTag( ) ) || ( certs.getServiceTag( ) == null ) ) {
      return ( this.hasClusterCert = this.checkCerts( this.getClusterCertificate( ), clusterx509 ) )
             && ( this.hasNodeCert = this.checkCerts( this.getNodeCertificate( ), nodex509 ) );
    } else if ( this.nodeMap.containsKey( certs.getServiceTag( ) ) ) {
      final NodeInfo nodeInfo = this.nodeMap.get( certs.getServiceTag( ) );
      nodeInfo.setHasClusterCert( this.checkCerts( this.getClusterCertificate( ), clusterx509 ) );
      nodeInfo.setHasNodeCert( this.checkCerts( this.getNodeCertificate( ), nodex509 ) );
      return nodeInfo.getHasClusterCert( ) && nodeInfo.getHasNodeCert( );
    } else {
      LOG.error( "Cluster " + this.getName( ) + " failed to find cluster/node info for service tag: " + certs.getServiceTag( ) );
      return false;
    }
  }
  
  private boolean checkCerts( final X509Certificate realx509, final X509Certificate msgx509 ) {
    if ( realx509 != null ) {
      final Boolean match = realx509.equals( msgx509 );
      EventRecord.here( Cluster.class, EventType.CLUSTER_CERT, this.getName( ), realx509.getSubjectX500Principal( ).getName( ), match.toString( ) ).info( );
      if ( !match ) {
        LOG.warn( LogUtil.subheader( "EXPECTED CERTIFICATE" ) + realx509 );
        LOG.warn( LogUtil.subheader( "RECEIVED CERTIFICATE" ) + msgx509 );
      }
      return match;
    } else {
      return false;
    }
  }
  
  private AbstractTransitionAction<Cluster> newLogRefresh( final Class msgClass ) {//TODO:GRZE:REMOVE
    final Cluster cluster = this;
    final SubjectRemoteCallbackFactory<RemoteCallback, Cluster> factory = newSubjectMessageFactory( msgClass, cluster );
    return new AbstractTransitionAction<Cluster>( ) {
      
      @Override
      public final void leave( final Cluster parent, final Callback.Completion transitionCallback ) {
        try {
          AsyncRequests.newRequest( factory.newInstance( ) ).then( transitionCallback )
                       .sendSync( parent.getLogServiceConfiguration( ) );
        } catch ( final ExecutionException e ) {
          if ( e.getCause( ) instanceof FailedRequestException ) {
            LOG.error( e.getCause( ).getMessage( ) );
          } else if ( ( e.getCause( ) instanceof ConnectionException ) || ( e.getCause( ) instanceof IOException ) ) {
            LOG.error( parent.getName( ) + ": Error communicating with cluster: " + e.getCause( ).getMessage( ) );
          } else {
            LOG.error( e, e );
          }
        } catch ( final InterruptedException e ) {
          LOG.error( e, e );
        }
      }
    };
  }
  
  protected ServiceConfiguration getLogServiceConfiguration( ) {
    final ComponentId glId = ComponentIds.lookup( GatherLogService.class );
    final ServiceConfiguration conf = this.getConfiguration( );
    return ServiceConfigurations.createEphemeral( glId, conf.getPartition( ), conf.getName( ),
                                                  glId.makeInternalRemoteUri( conf.getHostName( ), conf.getPort( ) ) );
  }
  
  @Override
  public void fireEvent( final Event event ) {
    if ( !Bootstrap.isFinished( ) ) {
      LOG.info( this.getConfiguration( ).getFullName( ) + " skipping clock event because bootstrap isn't finished" );
    } else if ( event instanceof Hertz ) {
      this.fireClockTick( ( Hertz ) event );
    } else if ( event instanceof LifecycleEvent ) {
      this.fireLifecycleEvent( ( LifecycleEvent ) event );
    }
  }
  
  private static <P, T extends SubjectMessageCallback<P, Q, R>, Q extends BaseMessage, R extends BaseMessage> SubjectRemoteCallbackFactory<T, P> newSubjectMessageFactory( final Class<T> callbackClass, final P subject ) {
    return new SubjectRemoteCallbackFactory( ) {
      @Override
      public T newInstance( ) {
        try {
          final T callback = callbackClass.newInstance( );
          callback.setSubject( subject );
          return callback;
        } catch ( final Throwable t ) {
          LOG.error( t, t );
          throw new RuntimeException( t );
        }
      }
      
      @Override
      public P getSubject( ) {
        return subject;
      }
    };
  }
  
  private void fireLifecycleEvent( final LifecycleEvent lifecycleEvent ) {
    if ( this.configuration.equals( lifecycleEvent.getReference( ) ) ) {
      LOG.info( lifecycleEvent );
//TODO:GRZE:come back and decide.
//        switch ( ( ( LifecycleEvent ) event ).getLifecycleEventType( ) ) {
//          case START:
//            this.start( );
//            break;
//          case ENABLE:
//            this.enable( );
//            break;
//          case DISABLE:
//            this.disable( );
//            break;
//          case STOP:
//            this.stop( );
//            break;
//          case ERROR:
//            LOG.info( event );
//            break;
//          case RESTART:
//            this.stop( );
//            this.start( );
//            break;
//          case STATE:
//            LOG.info( event );
//            break;
//        }
    }
  }
  
  private void updateVolatiles( ) {
    try {
      AsyncRequests.newRequest( new VmPendingCallback( this ) ).sendSync( this.getConfiguration( ) );
    } catch ( final ExecutionException ex ) {
      Exceptions.trace( ex );
    } catch ( final InterruptedException ex ) {
      Exceptions.trace( ex );
    } catch ( final CancellationException ex ) {
      /** operation self-cancelled **/
    }
  }
  
  public void check( ) throws CheckException {
    List<Throwable> currentErrors = Lists.newArrayList( );
    this.errors.drainTo( currentErrors );
    if ( !currentErrors.isEmpty( ) ) {
      CheckException ex = ServiceChecks.Severity.ERROR.transform( this.configuration, currentErrors );
      throw ex;
    }
  }
  
  @Override
  public String getPartition( ) {
    return this.configuration.getPartition( );
  }
  
  @Override
  public FullName getFullName( ) {
    return this.configuration.getFullName( );
  }
  
  @Override
  public StateMachine<Cluster, State, Transition> getStateMachine( ) {
    return this.stateMachine;
  }
  
}
