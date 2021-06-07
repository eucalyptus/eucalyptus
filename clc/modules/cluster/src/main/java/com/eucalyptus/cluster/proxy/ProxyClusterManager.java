/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
package com.eucalyptus.cluster.proxy;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.ConnectException;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.cluster.common.Cluster;
import com.eucalyptus.cluster.common.msgs.BroadcastNetworkInfoType;
import com.eucalyptus.cluster.common.msgs.ClusterDescribeServicesResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterDescribeServicesType;
import com.eucalyptus.cluster.common.msgs.ClusterDisableServiceType;
import com.eucalyptus.cluster.common.msgs.ClusterEnableServiceType;
import com.eucalyptus.cluster.common.msgs.ClusterStartServiceType;
import com.eucalyptus.cluster.proxy.config.ProxyClusterConfiguration;
import com.eucalyptus.cluster.common.msgs.NodeType;
import com.eucalyptus.cluster.proxy.callback.ProxyClusterCertsCallback;
import com.eucalyptus.cluster.service.ClusterServiceEnv;
import com.eucalyptus.cluster.service.ClusterServiceId;
import com.eucalyptus.cluster.service.conf.ClusterEucaConf;
import com.eucalyptus.cluster.service.conf.ClusterEucaConfLoader;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Faults;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.crypto.util.PEMFiles;
import com.eucalyptus.empyrean.ServiceId;
import com.eucalyptus.empyrean.ServiceStatusType;
import com.eucalyptus.empyrean.ServiceTransitionType;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.ConnectionException;
import com.eucalyptus.util.async.FailedRequestException;
import com.eucalyptus.util.async.RemoteCallback;
import com.eucalyptus.util.async.SubjectMessageCallback;
import com.eucalyptus.util.async.SubjectRemoteCallbackFactory;
import com.eucalyptus.util.fsm.AbstractTransitionAction;
import com.eucalyptus.util.fsm.Automata;
import com.eucalyptus.util.fsm.ExistingTransitionException;
import com.eucalyptus.util.fsm.HasStateMachine;
import com.eucalyptus.util.fsm.StateMachine;
import com.eucalyptus.util.fsm.StateMachineBuilder;
import com.eucalyptus.util.fsm.TransitionAction;
import com.eucalyptus.util.fsm.Transitions;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.ObjectArrays;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import com.eucalyptus.cluster.common.msgs.NodeCertInfo;
import io.vavr.control.Option;

/**
 * Manager for the C/proxy cluster controller.
 *
 * This manages the state for the external service and syncs with the cluster
 * controller state via the bootstrapper.
 *
 * This manager is responsible for checking certificates of the external
 * service.
 *
 * @see ProxyClusterBootstrapper
 */
public class ProxyClusterManager implements HasStateMachine<ProxyClusterManager, ProxyClusterManager.State, ProxyClusterManager.Transition> {
  private static final Logger LOG = Logger.getLogger( ProxyClusterManager.class );
  private static final AtomicReference<ProxyClusterManager> managerRef = new AtomicReference<>( );

  private final StateMachine<ProxyClusterManager, ProxyClusterManager.State, ProxyClusterManager.Transition> stateMachine;
  private final ProxyClusterConfiguration configuration;
  private final BlockingQueue<Throwable> pendingErrors  = new LinkedBlockingDeque<>( );
  private final AtomicReference<BroadcastNetworkInfoType> broadcast = new AtomicReference<>(  );

  @Nullable
  public static Option<ProxyClusterManager> local( ) {
    return Option.of( managerRef.get( ) );
  }

  static void checkLocal( ) {
    final Option<ServiceConfiguration> clusterService = Components.services( ClusterServiceId.class )
        .filter( ServiceConfigurations.filterVmLocal( ) )
        .headOption( );
    if ( clusterService.isDefined( ) && ClusterServiceEnv.requireProxy( ) ) {
      final ServiceConfiguration conf = clusterService.get( );
      final ClusterEucaConf eucaConf = new ClusterEucaConfLoader().load( System.currentTimeMillis() );
      final ProxyClusterConfiguration proxyConf = new ProxyClusterConfiguration(
          conf.getPartition( ),
          conf.getName( ),
          conf.getHostName( ),
          eucaConf.getClusterPort( )
      );
      final ProxyClusterManager current = managerRef.get( );
      if ( current==null ||
          !current.getConfiguration( ).getPartition( ).equals( proxyConf.getPartition( ) ) ||
          !current.getConfiguration( ).getPort( ).equals( proxyConf.getPort( ) ) ) {
        final ProxyClusterManager proxyProvider = new ProxyClusterManager( proxyConf );
        managerRef.set( proxyProvider  );
        LOG.info( "Using local cluster controller " + proxyConf );
      }
    } else {
      managerRef.set( null );
      LOG.info( "Cleared local cluster controller" );
    }
  }

  @SuppressWarnings( "WeakerAccess" )
  ProxyClusterManager( final ProxyClusterConfiguration configuration ) {
    this.configuration = configuration;
    this.stateMachine = new StateMachineBuilder<ProxyClusterManager, ProxyClusterManager.State, ProxyClusterManager.Transition>( this, State.PENDING ) {
      {
        final TransitionAction<ProxyClusterManager> noop = Transitions.noop( );
        this.in( State.NOTREADY ).run( ProxyClusterManager.ServiceStateDispatch.DISABLED );
        this.from( State.BROKEN ).to( State.PENDING ).error( State.BROKEN ).on( ProxyClusterManager.Transition.RESTART_BROKEN ).run( noop );

        this.from( State.STOPPED ).to( State.PENDING ).error( State.PENDING ).on( ProxyClusterManager.Transition.PRESTART ).run( noop );
        this.from( State.PENDING ).to( State.AUTHENTICATING ).error( State.PENDING ).on( ProxyClusterManager.Transition.AUTHENTICATE ).run( ProxyClusterManager.LogRefresh.CERTS );
        this.from( State.AUTHENTICATING ).to( State.STARTING ).error( State.PENDING ).on( ProxyClusterManager.Transition.START ).run( noop );
        this.from( State.STARTING ).to( State.STARTING_NOTREADY ).error( State.PENDING ).on( ProxyClusterManager.Transition.START_CHECK ).run( ProxyClusterManager.Refresh.SERVICEREADY );
        this.from( State.STARTING_NOTREADY ).to( State.NOTREADY ).error( State.PENDING ).on( ProxyClusterManager.Transition.STARTING_SERVICES ).run( ProxyClusterManager.Refresh.SERVICEREADY );

        this.from( State.NOTREADY ).to( State.DISABLED ).error( State.NOTREADY ).on( ProxyClusterManager.Transition.NOTREADYCHECK ).run( ProxyClusterManager.Refresh.SERVICEREADY );

        this.from( State.DISABLED ).to( State.DISABLED ).error( State.NOTREADY ).on( ProxyClusterManager.Transition.DISABLEDCHECK ).addListener( ProxyClusterManager.ErrorStateListeners.FLUSHPENDING ).run(
            ProxyClusterManager.Refresh.SERVICEREADY );
        this.from( State.DISABLED ).to( State.ENABLED ).error( State.DISABLED ).on( ProxyClusterManager.Transition.ENABLE ).run( ProxyClusterManager.ServiceStateDispatch.ENABLED );
        this.from( State.DISABLED ).to( State.STOPPED ).error( State.PENDING ).on( ProxyClusterManager.Transition.STOP ).run( noop );

        this.from( State.ENABLED ).to( State.DISABLED ).error( State.NOTREADY ).on( ProxyClusterManager.Transition.DISABLE ).run( ProxyClusterManager.ServiceStateDispatch.DISABLED );

        this.from( State.ENABLED ).to( State.ENABLED_SERVICE_CHECK ).error( State.NOTREADY ).on( ProxyClusterManager.Transition.ENABLED_SERVICES ).run( ProxyClusterManager.Refresh.SERVICEREADY );
        this.from( State.ENABLED_SERVICE_CHECK ).to( State.ENABLED ).error( State.NOTREADY ).on( ProxyClusterManager.Transition.ENABLED ).run( ProxyClusterManager.ErrorStateListeners.FLUSHPENDING );
      }
    }.newAtomicMarkedState( );
  }

  public ProxyClusterConfiguration getConfiguration( ) {
    return configuration;
  }

  private Option<Component.State> getClusterState( ) {
    return Components.services( ClusterServiceId.class )
        .filter( ServiceConfigurations.filterVmLocal( ) )
        .map( ServiceConfiguration::lookupState )
        .headOption( );
  }

  public boolean isEnabled( ) {
    return getStateMachine( ).getState( ).ordinal( ) >= State.ENABLED.ordinal( );
  }

  public void runState( ) {
    final ProxyClusterManager.State currentState = this.getState( );
    final List<Throwable> currentErrors = Lists.newArrayList( this.pendingErrors );
    this.pendingErrors.clear( );
    try {
      final Component.State clusterState =
          getClusterState( ).getOrElse( Component.State.PRIMORDIAL );
      final State proxyState = this.stateMachine.getState( );
      if ( clusterState == Component.State.STOPPED ) {
        disabledTransition( ).call( ).get( );
      } else if ( proxyState.ordinal( ) < State.DISABLED.ordinal( ) ) {
        startingTransition( ).call( ).get( );
      } else {
        enabledTransition( ).call( ).get( );
      }
    } catch ( Exception ex ) {
      //ignore cancellation errors.
      if ( !( ex.getCause( ) instanceof CancellationException ) ) {
        currentErrors.add( ex );
      }
    }
  }

  public void check( ) { }

  public void start( ) throws ServiceRegistrationException { }

  public void stop( ) throws ServiceRegistrationException { }

  public void enable( ) throws ServiceRegistrationException { }

  public void disable( ) throws ServiceRegistrationException { }

  public void refreshResources() { }

  public void updateNodeInfo( final long time, final List<NodeType> nodes ) { }

  public boolean hasNode( final String sourceHost ) {
    return false;
  }

  public void cleanup( final Cluster cluster, final Exception ex ) {
  }

  public boolean checkCerts( final NodeCertInfo certs ) {
    if ( certs == null ||
        certs.getCcCert( ) == null ||
        certs.getNcCert( ) == null ) {
      return false;
    }
    final X509Certificate clusterx509 = PEMFiles.getCert( B64.standard.dec( certs.getCcCert( ) ) );
    final X509Certificate nodex509 = PEMFiles.getCert( B64.standard.dec( certs.getNcCert( ) ) );
    return ( this.checkCerts( this.getClusterCertificate( ), clusterx509 ) )
        && ( this.checkCerts( this.getNodeCertificate( ), nodex509 ) );
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

  private ServiceConfiguration getLogServiceConfiguration( ) {
    final ComponentId glId = ComponentIds.lookup( ProxyClusterController.GatherLogService.class );
    final ServiceConfiguration conf = this.getConfiguration( );
    final URI glUri =
        ServiceUris.remote( ComponentIds.lookup( ProxyClusterController.GatherLogService.class ), conf.getInetAddress( ), conf.getPort( ) );
    return ServiceConfigurations.createEphemeral( glId, conf.getPartition( ), conf.getName( ), glUri );
  }

  private static SubjectRemoteCallbackFactory<SubjectMessageCallback<ProxyClusterManager, ?, ?>, ProxyClusterManager> newSubjectMessageFactory(
      final Class<? extends SubjectMessageCallback<ProxyClusterManager, ?, ?>> callbackClass,
      final ProxyClusterManager spi
  ) throws CancellationException {
    return new SubjectRemoteCallbackFactory<SubjectMessageCallback<ProxyClusterManager, ?, ?>, ProxyClusterManager>( ) {
      @Override
      public SubjectMessageCallback<ProxyClusterManager, ?, ?> newInstance( ) {
        try {
          final ProxyClusterManager subject = getSubject( );
          if ( subject != null ) {
            try {
              return Classes.builder( callbackClass ).arg( subject ).newInstance( );
            } catch ( UndeclaredThrowableException ex ) {
              if ( ex.getCause( ) instanceof CancellationException ) {
                throw ( CancellationException ) ex.getCause( );
              } else if ( ex.getCause( ) instanceof NoSuchMethodException ) {
                try {
                  SubjectMessageCallback<ProxyClusterManager, ?, ?> callback = Classes.builder( callbackClass ).newInstance( );
                  callback.setSubject( subject );
                  return callback;
                } catch ( UndeclaredThrowableException ex1 ) {
                  if ( ex1.getCause( ) instanceof CancellationException ) {
                    throw ( CancellationException ) ex.getCause( );
                  } else if ( ex1.getCause( ) instanceof NoSuchMethodException ) {
                    throw ex1;
                  } else {
                    throw ex1;
                  }
                } catch ( Exception ex1 ) {
                  if ( ex1.getCause( ) instanceof CancellationException ) {
                    throw ( CancellationException ) ex.getCause( );
                  } else if ( ex1.getCause( ) instanceof NoSuchMethodException ) {
                    throw ex;
                  } else {
                    throw Exceptions.toUndeclared( ex1 );
                  }
                }
              } else {
                SubjectMessageCallback<ProxyClusterManager, ?, ?> callback = callbackClass.newInstance( );
                LOG.error( "Creating uninitialized callback (subject=" + subject + ") for type: " + callbackClass.getCanonicalName( ) );
                return callback;
              }
            } catch ( RuntimeException ex ) {
              LOG.error( "Failed to create instance of: " + callbackClass );
              Logs.extreme( ).error( ex, ex );
              throw ex;
            }
          } else {
            SubjectMessageCallback<ProxyClusterManager, ?, ?> callback = callbackClass.newInstance( );
            LOG.error( "Creating uninitialized callback for type: " + callbackClass.getCanonicalName( ) );
            return callback;
          }
        } catch ( final CancellationException ex ) {
          LOG.debug( ex );
          throw ex;
        } catch ( final Exception ex ) {
          LOG.error( ex );
          Logs.extreme( ).error( ex, ex );
          throw Exceptions.toUndeclared( ex );
        }
      }

      @Override
      public ProxyClusterManager getSubject( ) {
        return spi;
      }
    };
  }

  private <T extends Throwable> boolean swallowException( final T t ) {
    final Level level = Exceptions.isCausedBy( t, ConnectException.class ) ? Level.WARN : Level.ERROR;
    LOG.log( level, this.getConfiguration( ).getFullName( ) + " checking: " + Exceptions.causeString( t ) );
    if ( Exceptions.isCausedBy(t, InterruptedException.class) ) {
      Thread.currentThread( ).interrupt( );
      return true;
    } else if ( Exceptions.isCausedBy( t, FailedRequestException.class ) ) {
      Logs.extreme( ).debug( t, t );
      this.pendingErrors.add( t );
      return false;
    } else if ( Exceptions.isCausedBy( t, ConnectionException.class ) || Exceptions.isCausedBy( t, IOException.class ) ) {
      LOG.log( level, this.getName( ) + ": Error communicating with cluster: "
          + t.getMessage( ) );
      Logs.extreme( ).debug( t, t );
      this.pendingErrors.add( t );
      return false;
    } else {
      Logs.extreme( ).debug( t, t );
      this.pendingErrors.add( t );
      return false;
    }
  }

  public State getState( ) {
    return getStateMachine( ).getState( );
  }

  @Override
  public StateMachine<ProxyClusterManager, ProxyClusterManager.State, ProxyClusterManager.Transition> getStateMachine( ) {
    return stateMachine;
  }

  @Override
  public String getPartition() {
    return configuration.getPartition( );
  }

  public Partition lookupPartition() {
    return Partitions.lookup( configuration );
  }

  public String getHostName( ) {
    return configuration.getHostName();
  }

  @Override
  public FullName getFullName() {
    return configuration.getFullName( );
  }

  @Override
  public String getName() {
    return configuration.getName( );
  }

  @Override
  public int compareTo( @Nonnull final ProxyClusterManager that ) {
    return this.getName( ).compareTo( that.getName( ) );
  }

  private enum ServiceStateDispatch implements Predicate<ProxyClusterManager>, RemoteCallback<ServiceTransitionType, ServiceTransitionType> {
    STARTED( ClusterStartServiceType.class ),
    ENABLED( ClusterEnableServiceType.class ) {
      @Override
      public boolean apply( final ProxyClusterManager input ) {
        try {
          if ( Bootstrap.isOperational( ) ) {
            super.apply( input );
          }
          return true;
        } catch ( final Exception t ) {
          return input.swallowException( t );
        }
      }
    },
    DISABLED( ClusterDisableServiceType.class ) {
      @Override
      public boolean apply( final ProxyClusterManager input ) {
        try {
          if ( Bootstrap.isOperational( ) ) {
            super.apply( input );
          }
          return true;
        } catch ( Exception ex ) {
          return false;
        }
      }
    };
    final Class<? extends ServiceTransitionType> msgClass;

    ServiceStateDispatch( Class<? extends ServiceTransitionType> msgClass ) {
      this.msgClass = msgClass;
    }

    @Override
    public ServiceTransitionType getRequest( ) {
      return Classes.newInstance( this.msgClass );
    }

    @Override
    public void fire( ServiceTransitionType msg ) {
      LOG.debug( this.name( ) + " service: " + msg );
    }

    @Override
    public boolean apply( final ProxyClusterManager input ) {
      if ( input.configuration.isHostLocal( ) ) {
        try {
          AsyncRequests.newRequest( this ).sendSync( input.configuration );
          return true;
        } catch ( final Exception t ) {
          return input.swallowException( t );
        }
      } else {
        return true;
      }
    }

    @Override
    public void initialize( ServiceTransitionType request ) throws Exception {
      request.set_return( null );
      request.setUserId( Principals.systemUser( ).getUserId( ) );
    }

    @Override
    public void fireException( Throwable t ) {
      Logs.extreme( ).error( t, t );
    }
  }

  private enum LogRefresh implements Function<ProxyClusterManager, TransitionAction<ProxyClusterManager>> {
    CERTS( ProxyClusterCertsCallback.class );
    Class<? extends SubjectMessageCallback<ProxyClusterManager,?,?>> refresh;

    LogRefresh( final Class<? extends SubjectMessageCallback<ProxyClusterManager,?,?>>refresh ) {
      this.refresh = refresh;
    }

    @Override
    public TransitionAction<ProxyClusterManager> apply( final ProxyClusterManager cluster ) {
      final SubjectRemoteCallbackFactory<? extends RemoteCallback<?,?>, ProxyClusterManager> factory = newSubjectMessageFactory( this.refresh, cluster );
      return new AbstractTransitionAction<ProxyClusterManager>( ) {
        @Override
        public final void leave( final ProxyClusterManager parent, final Callback.Completion transitionCallback ) {
          ProxyClusterManager.fireCallback( parent, parent.getLogServiceConfiguration( ), /*false,*/ factory, transitionCallback );
        }
      };
    }
  }

  private static class ServiceStateCallback extends SubjectMessageCallback<ProxyClusterManager, ClusterDescribeServicesType, ClusterDescribeServicesResponseType> {
    public ServiceStateCallback( ) {
      this.setRequest( new ClusterDescribeServicesType( ) );
    }

    @Override
    public void fire( final ClusterDescribeServicesResponseType msg ) {
      List<ServiceStatusType> serviceStatuses = msg.getServiceStatuses();
      ProxyClusterManager proxyClusterSpi = (ProxyClusterManager) this.getSubject();
      if ( serviceStatuses.isEmpty( ) ) {
        throw new NoSuchElementException( "Failed to find service info for cluster: " + proxyClusterSpi.getFullName( ) );
      } else if ( !Bootstrap.isOperational( ) ) {
        return;
      } else {
        ServiceConfiguration config = proxyClusterSpi.getConfiguration( );
        for ( ServiceStatusType status : serviceStatuses ) {
          if ( "self".equals( status.getServiceId( ).getName( ) ) || !config.getName( ).equals( status.getServiceId( ).getName( ) ) ) {
            status.setServiceId( TypeMappers.transform( proxyClusterSpi.getConfiguration( ), ServiceId.class ) );
          }
          if ( status.getServiceId() == null || status.getServiceId().getName() == null || status.getServiceId().getType() == null ) {
            LOG.error( "Received invalid service id: " + status );
          } else if ( config.getName( ).equals( status.getServiceId( ).getName( ) )
              && Components.lookup( ClusterServiceId.class ).getName().equals( status.getServiceId( ).getType() ) ) {
            LOG.debug( "Found service info: " + status );
            Component.State serviceState = Component.State.valueOf( status.getLocalState( ) );
            Faults.CheckException ex = Faults.transformToExceptions( ).apply( status );
            State proxyState = proxyClusterSpi.getStateMachine( ).getState( );
            if ( proxyState.ordinal( ) > State.DISABLED.ordinal( ) && serviceState.ordinal( ) < Component.State.ENABLED.ordinal( ) ) {
              try {
                proxyClusterSpi.getStateMachine( ).transition( State.BROKEN );
              } catch ( ExistingTransitionException e ) {
                // try again next time
              }
            }
            if ( Component.State.NOTREADY.equals( serviceState ) ) {
              throw new IllegalStateException( ex );
            } else if ( Component.State.ENABLED.equals( serviceState ) && State.DISABLED.ordinal( ) >= proxyState.ordinal( ) ) {
              ProxyClusterManager.ServiceStateDispatch.DISABLED.apply( proxyClusterSpi );
            } else if ( Component.State.DISABLED.equals( serviceState ) && State.ENABLED.equals( proxyState ) ) {
              ProxyClusterManager.ServiceStateDispatch.ENABLED.apply( proxyClusterSpi );
            } else if ( Component.State.LOADED.equals( serviceState ) && State.NOTREADY.ordinal( ) <= proxyState.ordinal( ) ) {
              ProxyClusterManager.ServiceStateDispatch.STARTED.apply( proxyClusterSpi );
            } else if ( Component.State.NOTREADY.ordinal( ) < serviceState.ordinal( ) ) {
              proxyClusterSpi.clearExceptions( );
            }
            return;
          }
        }
      }
      LOG.error("Failed to find service info for cluster: " + proxyClusterSpi.getFullName()
          + " instead found service status for: "
          + serviceStatuses);
      throw new NoSuchElementException( "Failed to find service info for cluster: " + proxyClusterSpi.getFullName( ) );
    }

    @Override
    public void setSubject( ProxyClusterManager subject ) {
      this.getRequest( ).getServices( ).add( TypeMappers.transform( subject.getConfiguration( ), ServiceId.class ) );
      this.getRequest( ).getServices( ).forEach( srv -> {
        srv.setType( "cluster" );
        srv.setFullName( srv.getFullName().replace( ":proxy-cluster:", ":cluster:" ) );
      } );
      super.setSubject( subject );
    }
  }

  private enum Refresh implements Function<ProxyClusterManager, TransitionAction<ProxyClusterManager>> {
    SERVICEREADY( ServiceStateCallback.class );
    Class<? extends SubjectMessageCallback<ProxyClusterManager,?,?>>  refresh;

    Refresh( final Class<? extends SubjectMessageCallback<ProxyClusterManager,?,?>> refresh ) {
      this.refresh = refresh;
    }

    @SuppressWarnings( { "rawtypes", "unchecked" } )
    @Override
    public TransitionAction<ProxyClusterManager> apply( final ProxyClusterManager manager ) {
      final SubjectRemoteCallbackFactory<? extends RemoteCallback<?,?>, ProxyClusterManager> factory = newSubjectMessageFactory( this.refresh, manager );
      return new AbstractTransitionAction<ProxyClusterManager>( ) {

        @SuppressWarnings( "rawtypes" )
        @Override
        public final void leave( final ProxyClusterManager parent, final Callback.Completion transitionCallback ) {
          ProxyClusterManager.fireCallback( parent, factory, transitionCallback );
        }
      };
    }

    public void fire( ProxyClusterManager input ) {
      final SubjectRemoteCallbackFactory<? extends RemoteCallback<?,?>, ProxyClusterManager> factory = newSubjectMessageFactory( this.refresh, input );
      try {
        RemoteCallback<?,?> messageCallback = factory.newInstance( );
        BaseMessage baseMessage = AsyncRequests.newRequest( messageCallback ).sendSync( input.getConfiguration( ) );
        if ( Logs.extreme( ).isDebugEnabled( ) ) {
          Logs.extreme( ).debug( "Response to " + messageCallback + ": " + baseMessage );
        }
      } catch ( CancellationException ex ) {
        //do nothing
      } catch ( Exception ex ) {
        LOG.error( ex );
        Logs.extreme( ).error( ex );
        throw Exceptions.toUndeclared( ex );
      }
    }

    @Override
    public String toString( ) {
      return this.name( ) + ":" + this.refresh.getSimpleName( );
    }
  }

  private static void fireCallback( final ProxyClusterManager parent, final SubjectRemoteCallbackFactory<? extends RemoteCallback<?,?>, ProxyClusterManager> factory, final Callback.Completion transitionCallback ) {
    fireCallback( parent, parent.getConfiguration( ), /*true,*/ factory, transitionCallback );
  }

  private static void fireCallback( final ProxyClusterManager parent,
                                    final ServiceConfiguration config,
                                    final SubjectRemoteCallbackFactory<? extends RemoteCallback<?,?>, ProxyClusterManager> factory,
                                    final Callback.Completion transitionCallback ) {
    RemoteCallback<?,?> messageCallback = null;
    try {
      try {
        messageCallback = factory.newInstance( );
        try {
          BaseMessage baseMessage = AsyncRequests.newRequest( messageCallback ).sendSync( config );
          transitionCallback.fire( );
          if ( Logs.isExtrrreeeme( ) ) {
            Logs.extreme( ).debug( baseMessage );
          }
        } catch ( final Exception t ) {
          if ( !parent.swallowException( t ) ) {
            transitionCallback.fireException( Exceptions.unwrapCause( t ) );
          } else {
            transitionCallback.fire( );
          }
        }
      } catch ( CancellationException ex ) {
        transitionCallback.fire( );
      } catch ( Exception ex ) {
        transitionCallback.fireException( ex );
      }
    } finally {
      if ( !transitionCallback.isDone( ) ) {
        LOG.debug( parent.getFullName( ) + " transition fell through w/o completing: " + messageCallback );
        Logs.extreme( ).debug( Exceptions.toUndeclared( parent.getFullName( ) + " transition fell through w/o completing: " + messageCallback ) );
        transitionCallback.fire( );
      }
    }
  }

  public enum State implements Automata.State<State> {
    BROKEN,
    /** cannot establish initial contact with cluster because of CLC side errors **/
    STOPPED,
    /** Component.State.NOTREADY: cluster unreachable **/
    PENDING,
    /** Component.State.NOTREADY: cluster unreachable **/
    AUTHENTICATING,
    STARTING,
    STARTING_NOTREADY,
    /** Component.State.NOTREADY:enter() **/
    NOTREADY,
    /** Component.State.NOTREADY -> Component.State.DISABLED **/
    DISABLED,
    /** Component.State.DISABLED -> DISABLED: service ready, not current primary **/
    /** Component.State.DISABLED -> Component.State.ENABLED **/
    /** Component.State.ENABLED -> Component.State.ENABLED **/
    ENABLED,
    ENABLED_SERVICE_CHECK
    ;
  }

  public enum Transition implements Automata.Transition<Transition> {
    RESTART_BROKEN,
    PRESTART,
    /** pending setup **/
    AUTHENTICATE,
    START,
    START_CHECK,
    STARTING_SERVICES,
    NOTREADYCHECK,
    ENABLE,

    ENABLED,
    ENABLED_VMS,
    ENABLED_SERVICES,
    ENABLED_RSC,

    DISABLE,
    DISABLEDCHECK,

    STOP,
  }

  private enum ErrorStateListeners implements Callback<ProxyClusterManager> {
    FLUSHPENDING {
      @Override
      public void fire( final ProxyClusterManager t ) {
        LOG.debug( "Clearing error logs for: " + t );
        t.clearExceptions( );
      }
    }
  }

  private void clearExceptions( ) {
    if ( !this.pendingErrors.isEmpty( ) ) {
      final List<Throwable> currentErrors = Lists.newArrayList( );
      this.pendingErrors.drainTo( currentErrors );
      for ( final Throwable t : currentErrors ) {
        final Throwable filtered = Exceptions.filterStackTrace( t );
        LOG.debug( this.configuration + ": Clearing error: "
            + filtered.getMessage( ), filtered );
      }
    } else {
      LOG.debug( this.configuration + ": no pending errors to clear." );
    }
  }

  private static final State[] PATH_NOTREADY      = new State[] { State.PENDING,
      State.AUTHENTICATING,
      State.STARTING,
      State.STARTING_NOTREADY,
      State.NOTREADY };
  private static final State[] PATH_DISABLED      = ObjectArrays.concat( PATH_NOTREADY, State.DISABLED );
  private static final State[] PATH_ENABLED       = new State[] { State.PENDING,
      State.AUTHENTICATING,
      State.STARTING,
      State.STARTING_NOTREADY,
      State.NOTREADY,
      State.DISABLED,
      State.ENABLED };

  private static final State[] PATH_ENABLED_CHECK = new State[] { State.ENABLED,
      State.ENABLED_SERVICE_CHECK,
      State.ENABLED };

  private Callable<CheckedListenableFuture<ProxyClusterManager>> enabledTransition( ) {
    if ( this.stateMachine.getState( ).ordinal( ) >= State.ENABLED.ordinal( ) ) {
      return Automata.sequenceTransitions( this, PATH_ENABLED_CHECK );
    } else {
      return Automata.sequenceTransitions( this, PATH_ENABLED );
    }
  }

  private Callable<CheckedListenableFuture<ProxyClusterManager>> disabledTransition( ) {
    if ( this.stateMachine.getState( ).ordinal( ) >= State.ENABLED.ordinal( ) ) {
      return Automata.sequenceTransitions( this, ObjectArrays.concat( PATH_ENABLED_CHECK, State.DISABLED ) );
    } else {
      return Automata.sequenceTransitions( this, ObjectArrays.concat( PATH_DISABLED, State.DISABLED ) );
    }
  }

  private Callable<CheckedListenableFuture<ProxyClusterManager>> notreadyTransition( ) {
    if ( this.stateMachine.getState( ).ordinal( ) >= State.ENABLED.ordinal( ) ) {
      return Automata.sequenceTransitions( this, ObjectArrays.concat( PATH_ENABLED_CHECK, State.DISABLED ) );
    } else {
      return Automata.sequenceTransitions( this, PATH_DISABLED );
    }
  }

  private Callable<CheckedListenableFuture<ProxyClusterManager>> startingTransition( ) {
    return Automata.sequenceTransitions( this, PATH_DISABLED );
  }

  private X509Certificate getClusterCertificate( ) {
    return Partitions.lookupByName( this.configuration.getPartition() ).getCertificate();
  }

  private X509Certificate getNodeCertificate( ) {
    return Partitions.lookupByName( this.configuration.getPartition() ).getNodeCertificate();
  }

  public static class ProxyClusterProviderEventListener implements EventListener<ClockTick> {
    public static void register( ) {
      Listeners.register( ClockTick.class, new ProxyClusterProviderEventListener() );
    }

    @Override
    public void fireEvent( final ClockTick event ) {
      if ( Bootstrap.isOperational( ) && ClusterServiceEnv.requireProxy( ) ) {
        for ( final ProxyClusterManager manager : ProxyClusterManager.local( ) ) {
          try {
            manager.runState( );
          } catch ( Exception e ) {
            LOG.error( e, e );
          }
        }
      }
    }
  }
}
