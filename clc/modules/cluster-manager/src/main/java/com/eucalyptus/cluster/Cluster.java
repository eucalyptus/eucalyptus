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
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.util.X509CertHelper;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.cluster.Cluster.State;
import com.eucalyptus.cluster.Cluster.Transition;
import com.eucalyptus.cluster.callback.ClusterCertsCallback;
import com.eucalyptus.cluster.callback.ClusterLogMessageCallback;
import com.eucalyptus.cluster.callback.NetworkStateCallback;
import com.eucalyptus.cluster.callback.PublicAddressStateCallback;
import com.eucalyptus.cluster.callback.ResourceStateCallback;
import com.eucalyptus.cluster.callback.ServiceStateCallback;
import com.eucalyptus.cluster.callback.VmPendingCallback;
import com.eucalyptus.cluster.callback.VmStateCallback;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.ServiceEndpoint;
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
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.async.Callback;
import com.eucalyptus.util.async.Callbacks;
import com.eucalyptus.util.async.ConnectionException;
import com.eucalyptus.util.async.FailedRequestException;
import com.eucalyptus.util.async.RemoteCallback;
import com.eucalyptus.util.async.SubjectRemoteCallbackFactory;
import com.eucalyptus.util.fsm.ExistingTransitionException;
import com.eucalyptus.util.fsm.StateMachine;
import com.eucalyptus.util.fsm.StateMachineBuilder;
import com.eucalyptus.util.fsm.AbstractTransitionAction;
import com.eucalyptus.vm.VmType;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import edu.ucsb.eucalyptus.cloud.NodeInfo;
import edu.ucsb.eucalyptus.msgs.NodeCertInfo;
import edu.ucsb.eucalyptus.msgs.NodeLogInfo;
import edu.ucsb.eucalyptus.msgs.NodeType;

public class Cluster implements HasName<Cluster>, EventListener {
  private static Logger                                       LOG            = Logger.getLogger( Cluster.class );
  private final StateMachine<Cluster, State, Transition> stateMachine;
  private final ClusterConfiguration                          configuration;
  private final FullName                                      fullName;
  private final ThreadFactory                                 threadFactory;
  private final ConcurrentNavigableMap<String, NodeInfo>      nodeMap;
  private final ClusterState                                  state;
  private final ClusterNodeState                              nodeState;
  private NodeLogInfo                                         lastLog        = new NodeLogInfo( );
  private boolean                                             hasClusterCert = false;
  private boolean                                             hasNodeCert    = false;
  
  public enum State {
    DISABLED, /* just like down, but is explicitly requested */
    DOWN, /* cluster either down, unreachable, or responds with errors */
    AUTHENTICATING, CHECKING_SERVICE, STARTING, STARTING_VMS2, STARTING_RESOURCES, STARTING_NET, STARTING_VMS, STARTING_ADDRS,
    RUNNING_ADDRS, RUNNING_RSC, RUNNING_NET, RUNNING_VMS, /* available */
  }
  
  public enum Transition {
    START, /* {DISABLED,DOWN} -> AUTHENTICATING: check certs */
    STOP, /* {RUNNING,STARTING,AUTHENTICATING} -> DOWN */
    DISABLE, /* any -> DISABLED */
    ENABLE, /* DISABLED -> DOWN */
    //    IO_ERROR, /* any -> DOWN: error communicating with cluster */
//    NETWORK_ERROR, /* any -> DOWN: error reaching cluster host */
//    CONFIG_ERROR, /* any -> DOWN: configuration error on the cluster */
    INIT_CERTS, /* AUTHENTICATING -> STARTING */
    INIT_SERVICES,
    INIT_RESOURCES,
    INIT_NET,
    INIT_VMS,
    INIT_ADDRS,
    INIT_VMS2,
    INIT_ADDRS2, /* STARTING -> RUNNING */
    RUNNING_ADDRS,
    RUNNING_VMS,
    RUNNING_NET,
    RUNNING_RSC, /* RUNNING -> RUNNING */
  }
  
  public Cluster( ClusterConfiguration configuration ) {
    super( );
    this.configuration = configuration;
    this.fullName = configuration.getFullName( );
    this.state = new ClusterState( configuration.getName( ) );
    this.nodeState = new ClusterNodeState( configuration.getName( ) );
    this.nodeMap = new ConcurrentSkipListMap<String, NodeInfo>( );
    this.threadFactory = Threads.lookup( com.eucalyptus.component.id.ClusterController.class, Cluster.class, this.getFullName( ).toString( ) );
    this.stateMachine = new StateMachineBuilder<Cluster, State, Transition>( this, State.DOWN ) {
      {
        //when entering state DOWN
        in( State.DOWN ).run( new Callback<Cluster>( ) {
          @Override
          public void fire( Cluster t ) {
            Cluster.this.transitionIfSafe( Transition.START );
          }
        } );
        
        //when entering state AUTHENTICATING
        in( State.AUTHENTICATING ).run( new Callback<Cluster>( ) {
          @Override
          public void fire( Cluster t ) {
            Cluster.this.transitionIfSafe( Transition.INIT_CERTS );
          }
        } );
        
        //on input START when in state DOWN transition to AUTHENTICATING and do nothing
        on( Transition.START )//
        .from( State.DOWN ).to( State.AUTHENTICATING ).noop( );
        
        //on input INIT_CERTS when in state AUTHENTICATING transition to STARTING on success or DOWN on failure with the transition listeners specified
        on( Transition.INIT_CERTS )//
        .from( State.AUTHENTICATING ).to( State.STARTING ).error( State.DOWN ).run( newRefresh( ClusterCertsCallback.class ) );
        
//        on( Transition.INIT_SERVICES ).from( State.CHECKING_SERVICE ).to( State.STARTING ).error( State.STARTING ).run( newRefresh( ServiceStateCallback.class ) );
        on( Transition.INIT_RESOURCES ).from( State.STARTING ).to( State.STARTING_RESOURCES ).error( State.DOWN ).run( newRefresh( ResourceStateCallback.class ) );
        on( Transition.INIT_NET ).from( State.STARTING_RESOURCES ).to( State.STARTING_NET ).error( State.DOWN ).run( newRefresh( NetworkStateCallback.class ) );
        on( Transition.INIT_VMS ).from( State.STARTING_NET ).to( State.STARTING_VMS ).error( State.DOWN ).run( newRefresh( VmStateCallback.class ) );
        on( Transition.INIT_ADDRS ).from( State.STARTING_VMS ).to( State.STARTING_ADDRS ).error( State.DOWN ).run( newRefresh( PublicAddressStateCallback.class ) );
        on( Transition.INIT_VMS2 ).from( State.STARTING_ADDRS ).to( State.STARTING_VMS2 ).error( State.DOWN ).run( newRefresh( VmStateCallback.class ) );
        on( Transition.INIT_ADDRS2 ).from( State.STARTING_VMS2 ).to( State.RUNNING_ADDRS ).error( State.DOWN ).run( newRefresh( PublicAddressStateCallback.class ) );
        
        on( Transition.RUNNING_RSC ).from( State.RUNNING_ADDRS ).to( State.RUNNING_RSC ).error( State.DOWN ).run( newRefresh( ResourceStateCallback.class ) );
        on( Transition.RUNNING_NET ).from( State.RUNNING_RSC ).to( State.RUNNING_NET ).error( State.DOWN ).run( newRefresh( NetworkStateCallback.class ) );
        on( Transition.RUNNING_VMS ).from( State.RUNNING_NET ).to( State.RUNNING_VMS ).error( State.DOWN ).run( newRefresh( VmStateCallback.class ) );
        on( Transition.RUNNING_ADDRS ).from( State.RUNNING_VMS ).to( State.RUNNING_ADDRS ).error( State.DOWN ).run( newRefresh( PublicAddressStateCallback.class ) );
        
        on( Transition.ENABLE ).from( State.DISABLED ).to( State.DOWN ).noop( );
        
      }
    }.newAtomicMarkedState( );
  }
  
  private FullName getFullName( ) {
    return this.fullName;
  }
  
  public Boolean isReady( ) {
    return this.hasClusterCert && this.hasNodeCert && Bootstrap.isFinished( );
  }
  
  public void transitionIfSafe( Transition transition ) {
    try {
      this.stateMachine.startTransition( transition );
    } catch ( IllegalStateException ex ) {
      LOG.error( ex, ex );
    } catch ( ExistingTransitionException ex ) {
    }
  }
  
  public X509Certificate getClusterCertificate( ) {
    try {
      return Partitions.lookup( this.configuration ).getCertificate( );
    } catch ( ServiceRegistrationException ex ) {
      LOG.error( ex , ex );
      return null;
    }
  }
  
  public X509Certificate getNodeCertificate( ) {
    try {
      return Partitions.lookup( this.configuration ).getNodeCertificate( );
    } catch ( ServiceRegistrationException ex ) {
      LOG.error( ex , ex );
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
    if( realx509 != null ) {
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
  
  /**
   * @param msgClass
   * @param nextState
   * @return
   */
  private AbstractTransitionAction<Cluster> newRefresh( final Class msgClass ) {
    return new AbstractTransitionAction<Cluster>( ) {
      private final SubjectRemoteCallbackFactory<RemoteCallback, Cluster> factory = Callbacks.newSubjectMessageFactory( msgClass, Cluster.this );
      
      @Override
      public final void leave( final Cluster parent, final Callback.Completion transitionCallback ) {
        Callback.Completion cb = new Callback.Completion( ) {
          
          @Override
          public void fire( ) {
            transitionCallback.fire( );
          }
          
          @Override
          public void fireException( Throwable t ) {
            if ( t instanceof FailedRequestException ) {
              if ( Cluster.this.getState( ).hasPublicAddressing( ) && PublicAddressStateCallback.class.isAssignableFrom( msgClass ) ) {
                transitionCallback.fire( );
              } else {
                transitionCallback.fireException( t );
              }
            } else {
              LOG.trace( t, t );
              transitionCallback.fireException( t );
            }
          }
        };
        //TODO: retry.
        try {
          if ( ClusterLogMessageCallback.class.isAssignableFrom( msgClass ) ) {
            ComponentId glId = ComponentIds.lookup( GatherLogService.class );
            ServiceConfiguration conf = parent.getConfiguration( );
            Callbacks.newRequest( this.factory.newInstance( ) ).then( cb )
                     .sendSync(  ServiceConfigurations.createEphemeral( glId, conf.getPartition( ), conf.getName( ), glId.makeRemoteUri( conf.getHostName( ), conf.getPort() ) ) ); 
          } else {
            Callbacks.newRequest( this.factory.newInstance( ) ).then( cb ).sendSync( parent.getConfiguration( ) );
          }
        } catch ( ExecutionException e ) {
          if ( e.getCause( ) instanceof FailedRequestException ) {
            LOG.error( e.getCause( ).getMessage( ) );
          } else if ( e.getCause( ) instanceof ConnectionException || e.getCause( ) instanceof IOException ) {
            //REVIEW: this is LOG.error( parent.getName( ) + ": Error communicating with cluster: " + e.getCause( ).getMessage( ) ); 
          } else {
            LOG.error( e, e );
          }
        } catch ( InterruptedException e ) {
          LOG.error( e, e );
        }
      }
    };
  }
  
  @Override
  public void fireEvent( Event event ) {
    if ( !Bootstrap.isFinished( ) ) {
      LOG.info( this.getConfiguration( ).toString( ) + " skipping clock event because bootstrap isn't finished" );
    } else if ( event instanceof ClockTick && ( ( ClockTick ) event ).isBackEdge( ) ) {
      this.nextState( );
    } else if ( event instanceof Hertz ) {
      Hertz tick = ( Hertz ) event;
      if ( State.RUNNING_ADDRS.ordinal( ) >= this.stateMachine.getState( ).ordinal( ) ) {
        this.nextState( );
      } else if ( State.RUNNING_ADDRS.ordinal( ) < this.stateMachine.getState( ).ordinal( ) && tick.isAsserted( 3 ) ) {
        this.updateVolatiles( );
      }
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
  
  private void nextState( ) {
    try {
      switch ( this.stateMachine.getState( ) ) {
        case DOWN:
          this.stateMachine.startTransition( Transition.START );
          break;
        case AUTHENTICATING:
          this.stateMachine.startTransition( Transition.INIT_CERTS );
          break;
        case CHECKING_SERVICE:
          this.stateMachine.startTransition( Transition.INIT_SERVICES );
          break;
        case STARTING:
          this.stateMachine.startTransition( Transition.INIT_RESOURCES );
          break;
        case STARTING_RESOURCES:
          this.stateMachine.startTransition( Transition.INIT_NET );
          break;
        case STARTING_NET:
          this.stateMachine.startTransition( Transition.INIT_VMS );
          break;
        case STARTING_VMS:
          this.stateMachine.startTransition( Transition.INIT_ADDRS );
          break;
        case STARTING_ADDRS:
          this.stateMachine.startTransition( Transition.INIT_VMS2 );
          break;
        case STARTING_VMS2:
          this.stateMachine.startTransition( Transition.INIT_ADDRS2 );
          break;
        case RUNNING_ADDRS:
          this.stateMachine.startTransition( Transition.RUNNING_RSC );
          break;
        case RUNNING_RSC:
          this.stateMachine.startTransition( Transition.RUNNING_NET );
          break;
        case RUNNING_NET:
          this.stateMachine.startTransition( Transition.RUNNING_VMS );
          break;
        case RUNNING_VMS:
          this.stateMachine.startTransition( Transition.RUNNING_ADDRS );
          break;
        default:
          break;
      }
    } catch ( IllegalStateException ex ) {
      Exceptions.trace( ex );
    } catch ( ExistingTransitionException ex ) {
      LOG.debug( ex.getMessage( ) );
    }
  }
  
}
