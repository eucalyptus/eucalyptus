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
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.cluster.callback.ClusterCertsCallback;
import com.eucalyptus.cluster.callback.LogDataCallback;
import com.eucalyptus.cluster.callback.NetworkStateCallback;
import com.eucalyptus.cluster.callback.PublicAddressStateCallback;
import com.eucalyptus.cluster.callback.ResourceStateCallback;
import com.eucalyptus.cluster.callback.ServiceStateCallback;
import com.eucalyptus.cluster.callback.VmPendingCallback;
import com.eucalyptus.cluster.callback.VmStateCallback;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.event.LifecycleEvent;
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
import com.eucalyptus.util.async.Callback;
import com.eucalyptus.util.async.Callbacks;
import com.eucalyptus.util.async.ConnectionException;
import com.eucalyptus.util.async.FailedRequestException;
import com.eucalyptus.util.async.RemoteCallback;
import com.eucalyptus.util.async.SubjectRemoteCallbackFactory;
import com.eucalyptus.util.fsm.AbstractTransitionAction;
import com.eucalyptus.util.fsm.Automata;
import com.eucalyptus.util.fsm.ExistingTransitionException;
import com.eucalyptus.util.fsm.HasStateMachine;
import com.eucalyptus.util.fsm.StateMachine;
import com.eucalyptus.util.fsm.StateMachineBuilder;
import com.eucalyptus.util.fsm.TransitionAction;
import com.eucalyptus.util.fsm.Transitions;
import com.eucalyptus.vm.VmType;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import edu.ucsb.eucalyptus.cloud.NodeInfo;
import edu.ucsb.eucalyptus.msgs.NodeCertInfo;
import edu.ucsb.eucalyptus.msgs.NodeLogInfo;
import edu.ucsb.eucalyptus.msgs.NodeType;

public class Cluster implements HasFullName<Cluster>, EventListener, HasStateMachine<Cluster, Cluster.State, Cluster.Transition> {
  private static Logger                                  LOG                   = Logger.getLogger( Cluster.class );
  private final StateMachine<Cluster, State, Transition> stateMachine;
  private final ClusterConfiguration                     configuration;
  private final FullName                                 fullName;
  private final ThreadFactory                            threadFactory;
  private final ConcurrentNavigableMap<String, NodeInfo> nodeMap;
  private final ClusterState                             state;
  private final ClusterNodeState                         nodeState;
  private NodeLogInfo                                    lastLog               = new NodeLogInfo( );
  private boolean                                        hasClusterCert        = false;
  private boolean                                        hasNodeCert           = false;
  private final Predicate<Cluster>                       COMPONENT_IS_DISABLED = new Predicate<Cluster>( ) {
                                                                                 
                                                                                 @Override
                                                                                 public boolean apply( Cluster input ) {
                                                                                   return Component.State.DISABLED.equals( input.getConfiguration( ).lookupService( ).getState( ) );
                                                                                 }
                                                                               };
  private final Predicate<Cluster>                       COMPONENT_IS_ENABLED  = new Predicate<Cluster>( ) {
                                                                                 
                                                                                 @Override
                                                                                 public boolean apply( Cluster input ) {
                                                                                   return Component.State.ENABLED.equals( input.getConfiguration( ).lookupService( ).getState( ) );
                                                                                 }
                                                                               };
  private final Predicate<Cluster>                       COMPONENT_IS_STARTED  = new Predicate<Cluster>( ) {
                                                                                 
                                                                                 @Override
                                                                                 public boolean apply( Cluster input ) {
                                                                                   return Component.State.NOTREADY.ordinal( ) <= input.getConfiguration( ).lookupService( ).getState( ).ordinal( );
                                                                                 }
                                                                               };
  
  enum LogRefresh implements Function<Cluster, TransitionAction<Cluster>> {
    LOGS( LogDataCallback.class ),
    CERTS( ClusterCertsCallback.class );
    Class refresh;
    
    private LogRefresh( Class refresh ) {
      this.refresh = refresh;
    }
    
    @Override
    public TransitionAction<Cluster> apply( Cluster cluster ) {
      final SubjectRemoteCallbackFactory<RemoteCallback, Cluster> factory = Callbacks.newSubjectMessageFactory( refresh, cluster );
      return new AbstractTransitionAction<Cluster>( ) {
        
        @Override
        public final void leave( final Cluster parent, final Callback.Completion transitionCallback ) {
          try {
            Callbacks.newRequest( factory.newInstance( ) ).then( transitionCallback )
                     .sendSync( parent.getLogServiceConfiguration( ) );
          } catch ( ExecutionException e ) {
            if ( e.getCause( ) instanceof FailedRequestException ) {
              LOG.error( e.getCause( ).getMessage( ) );
            } else if ( e.getCause( ) instanceof ConnectionException || e.getCause( ) instanceof IOException ) {
              LOG.error( parent.getName( ) + ": Error communicating with cluster: " + e.getCause( ).getMessage( ) );
            } else {
              LOG.error( e, e );
            }
          } catch ( InterruptedException e ) {
            LOG.error( e, e );
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
    
    private Refresh( Class refresh ) {
      this.refresh = refresh;
    }
    
    @Override
    public TransitionAction<Cluster> apply( Cluster cluster ) {
      final SubjectRemoteCallbackFactory<RemoteCallback, Cluster> factory = Callbacks.newSubjectMessageFactory( refresh, cluster );
      return new AbstractTransitionAction<Cluster>( ) {
        
        @Override
        public final void leave( final Cluster parent, final Callback.Completion transitionCallback ) {
          try {
            Callbacks.newRequest( factory.newInstance( ) ).then( transitionCallback ).sendSync( parent.getConfiguration( ) );
          } catch ( ExecutionException e ) {
            if ( e.getCause( ) instanceof FailedRequestException ) {
              LOG.error( e.getCause( ).getMessage( ) );
            } else if ( e.getCause( ) instanceof ConnectionException || e.getCause( ) instanceof IOException ) {
              LOG.error( parent.getName( ) + ": Error communicating with cluster: " + e.getCause( ).getMessage( ) );
            } else {
              LOG.error( e, e );
            }
          } catch ( InterruptedException e ) {
            LOG.error( e, e );
          }
        }
      };
    }
    
  }
  
  public enum State implements Automata.State<State> {
    BROKEN, /** cannot establish initial contact with cluster because of CLC side errors **/
    STOPPED, /** Component.State.NOTREADY: cluster unreachable **/
    PENDING, /** Component.State.NOTREADY: cluster unreachable **/
    STARTING_NOTREADY, /** Component.State.NOTREADY: reported state is NOTREADY **/
    STARTING_AUTHENTICATING, /** Component.State.NOTREADY:enter() **/
    NOTREADY, /** Component.State.NOTREADY -> Component.State.DISABLED **/
    DISABLED, /** Component.State.DISABLED -> DISABLED: service ready, not current primary **/
    /** Component.State.DISABLED -> Component.State.ENABLED **/
    ENABLING, ENABLING_RESOURCES, ENABLING_NET, ENABLING_VMS, ENABLING_ADDRS, ENABLING_VMS_PASS_TWO, ENABLING_ADDRS_PASS_TWO,
    /** Component.State.ENABLED -> Component.State.ENABLED **/
    ENABLED, ENABLED_ADDRS, ENABLED_RSC, ENABLED_NET, ENABLED_VMS, ENABLED_SERVICE_CHECK,
  }
  
  public enum Transition implements Automata.Transition<Transition> {
    RESTART_BROKEN,
    /** pending setup **/
    START, STARTING_CERTS, STARTING_SERVICES,
    NOTREADYCHECK,
    ENABLE, ENABLING_RESOURCES, ENABLING_NET, ENABLING_VMS, ENABLING_ADDRS, ENABLING_VMS_PASS_TWO, ENABLING_ADDRS_PASS_TWO,

    ENABLED_ADDRS, ENABLED_VMS, ENABLED_NET, ENABLED_SERVICES, ENABLED_RSC,

    DISABLE, DISABLEDCHECK,

    STOP,
    
  }
  
  public Cluster( ClusterConfiguration configuration ) {
    super( );
    this.configuration = configuration;
    this.fullName = configuration.getFullName( );
    this.state = new ClusterState( configuration.getName( ) );
    this.nodeState = new ClusterNodeState( configuration.getName( ) );
    this.nodeMap = new ConcurrentSkipListMap<String, NodeInfo>( );
    this.threadFactory = Threads.lookup( com.eucalyptus.component.id.ClusterController.class, Cluster.class, this.getFullName( ).toString( ) );
    this.stateMachine = new StateMachineBuilder<Cluster, State, Transition>( this, State.PENDING ) {
      {
        from( State.BROKEN ).to( State.PENDING ).error( State.BROKEN ).on( Transition.RESTART_BROKEN ).run( COMPONENT_IS_STARTED );
        
        from( State.PENDING ).to( State.STARTING_AUTHENTICATING ).error( State.PENDING ).on( Transition.START ).run( COMPONENT_IS_STARTED );
        from( State.STARTING_AUTHENTICATING ).to( State.STARTING_NOTREADY ).error( State.PENDING ).on( Transition.STARTING_CERTS ).run( LogRefresh.CERTS );
        from( State.STARTING_NOTREADY ).to( State.NOTREADY ).error( State.PENDING ).on( Transition.STARTING_SERVICES ).run( Refresh.SERVICEREADY );
        
        from( State.NOTREADY ).to( State.DISABLED ).error( State.NOTREADY ).on( Transition.NOTREADYCHECK ).run( Refresh.SERVICEREADY );
        
        from( State.DISABLED ).to( State.DISABLED ).error( State.NOTREADY ).on( Transition.DISABLEDCHECK ).run( Refresh.SERVICEREADY );
        from( State.DISABLED ).to( State.ENABLING ).error( State.DISABLED ).on( Transition.ENABLE ).run( COMPONENT_IS_ENABLED );
        
        from( State.ENABLING ).to( State.ENABLING_RESOURCES ).error( State.NOTREADY ).on( Transition.ENABLING_RESOURCES ).run( Refresh.RESOURCES );
        from( State.ENABLING_RESOURCES ).to( State.ENABLING_NET ).error( State.NOTREADY ).on( Transition.ENABLING_NET ).run( Refresh.NETWORKS );
        from( State.ENABLING_NET ).to( State.ENABLING_VMS ).error( State.NOTREADY ).on( Transition.ENABLING_VMS ).run( Refresh.INSTANCES );
        from( State.ENABLING_VMS ).to( State.ENABLING_ADDRS ).error( State.NOTREADY ).on( Transition.ENABLING_ADDRS ).run( Refresh.ADDRESSES );
        from( State.ENABLING_ADDRS ).to( State.ENABLING_VMS_PASS_TWO ).error( State.NOTREADY ).on( Transition.ENABLING_VMS_PASS_TWO ).run( Refresh.INSTANCES );
        from( State.ENABLING_VMS_PASS_TWO ).to( State.ENABLED_ADDRS ).error( State.NOTREADY ).on( Transition.ENABLING_ADDRS_PASS_TWO ).run( Refresh.ADDRESSES );
        
        from( State.ENABLED_ADDRS ).to( State.ENABLED_RSC ).error( State.NOTREADY ).on( Transition.ENABLED_RSC ).run( Refresh.RESOURCES );
        from( State.ENABLED_RSC ).to( State.ENABLED_NET ).error( State.NOTREADY ).on( Transition.ENABLED_NET ).run( Refresh.NETWORKS );
        from( State.ENABLED_NET ).to( State.ENABLED_VMS ).error( State.NOTREADY ).on( Transition.ENABLED_VMS ).run( Refresh.INSTANCES );
        from( State.ENABLED_VMS ).to( State.ENABLED_SERVICE_CHECK ).error( State.NOTREADY ).on( Transition.ENABLED_ADDRS ).run( Refresh.ADDRESSES );
        from( State.ENABLED_SERVICE_CHECK ).to( State.ENABLED_ADDRS ).error( State.NOTREADY ).on( Transition.ENABLED_SERVICES ).run( Refresh.SERVICEREADY );
        
      }
    }.newAtomicMarkedState( );
  }
  
  private void nextState( ) {
    try {
      if ( this.stateMachine.isBusy( ) ) {
        return;
      } else {
        switch ( this.stateMachine.getState( ) ) {
          case PENDING:
          case STARTING_AUTHENTICATING:
          case STARTING_NOTREADY:
            Automata.chainedTransition( this, State.PENDING, State.STARTING_AUTHENTICATING, State.STARTING_NOTREADY, State.NOTREADY ).call( );
            break;
          case NOTREADY:
            Automata.chainedTransition( this, State.NOTREADY, State.DISABLED ).call( );
          case DISABLED:
            if ( Component.State.ENABLED.apply( this.configuration ) ) {
              Automata.chainedTransition( this, State.DISABLED, State.ENABLING ).call( );
            } else if ( Component.State.DISABLED.apply( this.configuration ) ) {
              Automata.chainedTransition( this, State.DISABLED, State.DISABLED ).call( );
            }
          case ENABLING:
          case ENABLING_RESOURCES:
          case ENABLING_NET:
          case ENABLING_VMS:
          case ENABLING_ADDRS:
          case ENABLING_VMS_PASS_TWO:
          case ENABLING_ADDRS_PASS_TWO:
            Automata.chainedTransition( this, State.ENABLING, State.ENABLING_RESOURCES, State.ENABLING_NET, State.ENABLING_VMS, State.ENABLING_ADDRS,
                                        State.ENABLING_VMS_PASS_TWO, State.ENABLING_ADDRS_PASS_TWO, State.ENABLED ).call( );
            break;
          case ENABLED:
          case ENABLED_ADDRS:
          case ENABLED_RSC:
          case ENABLED_NET:
          case ENABLED_VMS:
          case ENABLED_SERVICE_CHECK:
            if ( Component.State.ENABLED.apply( this.configuration ) ) {
              Automata.chainedTransition( this, State.ENABLED, State.ENABLED_ADDRS, State.ENABLED_RSC, State.ENABLED_NET, State.ENABLED_VMS,
                                          State.ENABLED_SERVICE_CHECK ).call( );
            } else if ( Component.State.DISABLED.apply( this.configuration ) || Component.State.NOTREADY.apply( this.configuration ) ) {
              this.stateMachine.transition( State.DISABLED );
            }
            break;
          default:
            break;
        }
      }
    } catch ( IllegalStateException ex ) {
      Exceptions.trace( ex );
    } catch ( ExistingTransitionException ex ) {
      LOG.debug( ex.getMessage( ) );
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
    }
  }
  
  public Boolean isReady( ) {
    return this.hasClusterCert && this.hasNodeCert && Bootstrap.isFinished( );
  }
  
  public X509Certificate getClusterCertificate( ) {
    try {
      return Partitions.lookup( this.configuration ).getCertificate( );
    } catch ( ServiceRegistrationException ex ) {
      LOG.error( ex, ex );
      return null;
    }
  }
  
  public X509Certificate getNodeCertificate( ) {
    try {
      return Partitions.lookup( this.configuration ).getNodeCertificate( );
    } catch ( ServiceRegistrationException ex ) {
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
  
  public NodeInfo getNode( String serviceTag ) {
    return this.nodeMap.get( serviceTag );
  }
  
  public void updateNodeInfo( ArrayList<String> serviceTags ) {
    NodeInfo ret = null;
    for ( String serviceTag : serviceTags ) {
      if ( ( ret = this.nodeMap.putIfAbsent( serviceTag, new NodeInfo( serviceTag ) ) ) != null ) {
        ret.touch( );
        ret.setServiceTag( serviceTag );
        ret.setIqn( "No IQN reported" );
      }
    }
  }
  
  public void updateNodeInfo( List<NodeType> nodeTags ) {
    NodeInfo ret = null;
    for ( NodeType node : nodeTags )
      if ( ( ret = this.nodeMap.putIfAbsent( node.getServiceTag( ), new NodeInfo( node ) ) ) != null ) {
        ret.touch( );
        ret.setServiceTag( node.getServiceTag( ) );
        ret.setIqn( node.getIqn( ) );
      }
  }
  
  @Override
  public int compareTo( Cluster that ) {
    return this.getName( ).compareTo( that.getName( ) );
  }
  
  public ClusterConfiguration getConfiguration( ) {
    return this.configuration;
  }
  
  public RegisterClusterType getWeb( ) {
    String host = this.getConfiguration( ).getHostName( );
    int port = 0;
    URI uri = this.getConfiguration( ).getUri( );
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
  
  public void start( ) {
    Clusters.getInstance( ).register( this );
    this.configuration.lookupService( ).getEndpoint( ).start( );//TODO:GRZE: this has a corresponding transition and needs to be removed when that is activated.
    ListenerRegistry.getInstance( ).register( ClockTick.class, this );
    ListenerRegistry.getInstance( ).register( Hertz.class, this );
  }
  
  public void stop( ) {
    ListenerRegistry.getInstance( ).deregister( Hertz.class, this );
    ListenerRegistry.getInstance( ).deregister( ClockTick.class, this );
    this.configuration.lookupService( ).getEndpoint( ).stop( );//TODO:GRZE: this has a corresponding transition and needs to be removed when that is activated.
    Clusters.getInstance( ).registerDisabled( this );
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
  public boolean equals( Object obj ) {
    if ( this == obj ) return true;
    if ( obj == null ) return false;
    if ( getClass( ) != obj.getClass( ) ) return false;
    Cluster other = ( Cluster ) obj;
    if ( this.configuration == null ) {
      if ( other.configuration != null ) return false;
    } else if ( !this.configuration.equals( other.configuration ) ) return false;
    if ( this.state == null ) {
      if ( other.state != null ) return false;
    } else if ( !this.state.equals( other.state ) ) return false;
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
    StringBuilder buf = new StringBuilder( );
    buf.append( "Cluster " ).append( this.configuration ).append( '\n' );
    buf.append( "Cluster " ).append( this.configuration.getName( ) ).append( " mq=" ).append( this.getConfiguration( ).lookupService( ).getEndpoint( ) ).append( '\n' );
    for ( NodeInfo node : this.nodeMap.values( ) ) {
      buf.append( "Cluster " ).append( this.configuration.getName( ) ).append( " node=" ).append( node ).append( '\n' );
    }
    for ( VmType type : VmTypes.list( ) ) {
      VmTypeAvailability avail = this.nodeState.getAvailability( type.getName( ) );
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
      } catch ( Throwable t ) {
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
      public boolean apply( NodeInfo arg0 ) {
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
      } catch ( Throwable t ) {
        LOG.debug( t, t );
      } finally {
        this.logUpdate.set( false );
      }
    }
    return nodeInfo.getLogs( );
  }
  
  public void setLastLog( NodeLogInfo lastLog ) {
    this.lastLog = lastLog;
  }
  
  public boolean checkCerts( NodeCertInfo certs ) {
    if ( certs == null || certs.getCcCert( ) == null || certs.getNcCert( ) == null ) {
      return false;
    }
    
    X509Certificate clusterx509 = PEMFiles.getCert( B64.dec( certs.getCcCert( ) ) );
    X509Certificate nodex509 = PEMFiles.getCert( B64.dec( certs.getNcCert( ) ) );
    if ( "self".equals( certs.getServiceTag( ) ) || certs.getServiceTag( ) == null ) {
      return ( this.hasClusterCert = checkCerts( this.getClusterCertificate( ), clusterx509 ) )
             && ( this.hasNodeCert = checkCerts( this.getNodeCertificate( ), nodex509 ) );
    } else if ( this.nodeMap.containsKey( certs.getServiceTag( ) ) ) {
      NodeInfo nodeInfo = this.nodeMap.get( certs.getServiceTag( ) );
      nodeInfo.setHasClusterCert( checkCerts( this.getClusterCertificate( ), clusterx509 ) );
      nodeInfo.setHasNodeCert( checkCerts( this.getNodeCertificate( ), nodex509 ) );
      return nodeInfo.getHasClusterCert( ) && nodeInfo.getHasNodeCert( );
    } else {
      LOG.error( "Cluster " + this.getName( ) + " failed to find cluster/node info for service tag: " + certs.getServiceTag( ) );
      return false;
    }
  }
  
  private boolean checkCerts( X509Certificate realx509, X509Certificate msgx509 ) {
    if ( realx509 != null ) {
      Boolean match = realx509.equals( msgx509 );
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
  
  private AbstractTransitionAction<Cluster> newLogRefresh( final Class msgClass ) {
    final Cluster cluster = this;
    final SubjectRemoteCallbackFactory<RemoteCallback, Cluster> factory = Callbacks.newSubjectMessageFactory( msgClass, cluster );
    return new AbstractTransitionAction<Cluster>( ) {
      
      @Override
      public final void leave( final Cluster parent, final Callback.Completion transitionCallback ) {
        try {
          Callbacks.newRequest( factory.newInstance( ) ).then( transitionCallback )
                   .sendSync( parent.getLogServiceConfiguration( ) );
        } catch ( ExecutionException e ) {
          if ( e.getCause( ) instanceof FailedRequestException ) {
            LOG.error( e.getCause( ).getMessage( ) );
          } else if ( e.getCause( ) instanceof ConnectionException || e.getCause( ) instanceof IOException ) {
            LOG.error( parent.getName( ) + ": Error communicating with cluster: " + e.getCause( ).getMessage( ) );
          } else {
            LOG.error( e, e );
          }
        } catch ( InterruptedException e ) {
          LOG.error( e, e );
        }
      }
    };
  }
  
  protected ServiceConfiguration getLogServiceConfiguration( ) {
    ComponentId glId = ComponentIds.lookup( GatherLogService.class );
    ServiceConfiguration conf = this.getConfiguration( );
    return ServiceConfigurations.createEphemeral( glId, conf.getPartition( ), conf.getName( ),
                                                  glId.makeRemoteUri( conf.getHostName( ), conf.getPort( ) ) );
  }
  
  @Override
  public void fireEvent( Event event ) {
    if ( !Bootstrap.isFinished( ) ) {
      LOG.info( this.getConfiguration( ).toString( ) + " skipping clock event because bootstrap isn't finished" );
    } else if ( event instanceof ClockTick && ( ( ClockTick ) event ).isBackEdge( ) ) {
      this.nextState( );
    } else if ( event instanceof Hertz ) {
      Hertz tick = ( Hertz ) event;
      if ( State.ENABLED_ADDRS.ordinal( ) >= this.stateMachine.getState( ).ordinal( ) ) {
        this.nextState( );
      } else if ( State.ENABLED_ADDRS.ordinal( ) < this.stateMachine.getState( ).ordinal( ) && tick.isAsserted( 3 ) ) {
        this.updateVolatiles( );
      }
    } else if ( event instanceof LifecycleEvent ) {
      LOG.info( LogUtil.dumpObject( event ) );//TODO:GRZE: FINISH UP HERE.
    }
  }
  
  private void updateVolatiles( ) {
    try {
      Callbacks.newRequest( new VmPendingCallback( this ) ).sendSync( this.getConfiguration( ) );
    } catch ( ExecutionException ex ) {
      Exceptions.trace( ex );
    } catch ( InterruptedException ex ) {
      Exceptions.trace( ex );
    } catch ( CancellationException ex ) {
      /** operation self-cancelled **/
    }
  }
  
  public void check( ) {
    //TODO:GRZE:OMGDOIT
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
