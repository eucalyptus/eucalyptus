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
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.cluster.common.Cluster;
import com.eucalyptus.cluster.common.ClusterRegistry;
import com.eucalyptus.cluster.common.callback.ResourceStateCallback;
import com.eucalyptus.cluster.common.callback.VmStateCallback;
import com.eucalyptus.cluster.common.msgs.ClusterDescribeServicesResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterDescribeServicesType;
import com.eucalyptus.cluster.common.msgs.ClusterDisableServiceType;
import com.eucalyptus.cluster.common.msgs.ClusterEnableServiceType;
import com.eucalyptus.cluster.common.msgs.ClusterStartServiceType;
import com.eucalyptus.cluster.common.msgs.NodeInfo;
import com.eucalyptus.cluster.proxy.config.ClusterConfiguration;
import com.eucalyptus.cluster.proxy.node.ProxyNodeController;
import com.eucalyptus.cluster.common.provider.ClusterProvider;
import com.eucalyptus.cluster.common.msgs.NodeType;
import com.eucalyptus.cluster.proxy.callback.ProxyClusterCertsCallback;
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
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Hertz;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.cluster.proxy.node.Nodes;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.TypeMapper;
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

/**
 * "Proxy" provider for the C cluster controller.
 *
 * This provider acts as a service state proxy for the external service.
 *
 * This provider is responsible for checking certificates of the external
 * service.
 */
public class ProxyClusterProvider implements ClusterProvider, EventListener, HasStateMachine<ProxyClusterProvider, ProxyClusterProvider.State, ProxyClusterProvider.Transition> {
  private static Logger LOG            = Logger.getLogger( ProxyClusterProvider.class );

  private final StateMachine<ProxyClusterProvider, ProxyClusterProvider.State, ProxyClusterProvider.Transition> stateMachine;
  private final ClusterConfiguration configuration;

  private final BlockingQueue<Throwable> pendingErrors  = new LinkedBlockingDeque<>( );
  private Cluster     cluster        = null;

  @SuppressWarnings( "WeakerAccess" )
  public ProxyClusterProvider( final ClusterConfiguration configuration ) {
    this.configuration = configuration;
    this.stateMachine = new StateMachineBuilder<ProxyClusterProvider, ProxyClusterProvider.State, ProxyClusterProvider.Transition>( this, ProxyClusterProvider.State.PENDING ) {
      {
        final TransitionAction<ProxyClusterProvider> noop = Transitions.noop( );
        this.in( ProxyClusterProvider.State.DISABLED ).run( ProxyClusterProvider.ZoneRegistration.DEREGISTER );
        this.in( ProxyClusterProvider.State.NOTREADY ).run( ProxyClusterProvider.ServiceStateDispatch.DISABLED );
        this.in( ProxyClusterProvider.State.ENABLED ).run( ProxyClusterProvider.ZoneRegistration.REGISTER );
        this.from( ProxyClusterProvider.State.BROKEN ).to( ProxyClusterProvider.State.PENDING ).error( ProxyClusterProvider.State.BROKEN ).on( ProxyClusterProvider.Transition.RESTART_BROKEN ).run( noop );

        this.from( ProxyClusterProvider.State.STOPPED ).to( ProxyClusterProvider.State.PENDING ).error( ProxyClusterProvider.State.PENDING ).on( ProxyClusterProvider.Transition.PRESTART ).run( noop );
        this.from( ProxyClusterProvider.State.PENDING ).to( ProxyClusterProvider.State.AUTHENTICATING ).error( ProxyClusterProvider.State.PENDING ).on( ProxyClusterProvider.Transition.AUTHENTICATE ).run( ProxyClusterProvider.LogRefresh.CERTS );
        this.from( ProxyClusterProvider.State.AUTHENTICATING ).to( ProxyClusterProvider.State.STARTING ).error( ProxyClusterProvider.State.PENDING ).on( ProxyClusterProvider.Transition.START ).run( noop );
        this.from( ProxyClusterProvider.State.STARTING ).to( ProxyClusterProvider.State.STARTING_NOTREADY ).error( ProxyClusterProvider.State.PENDING ).on( ProxyClusterProvider.Transition.START_CHECK ).run( ProxyClusterProvider.Refresh.SERVICEREADY );
        this.from( ProxyClusterProvider.State.STARTING_NOTREADY ).to( ProxyClusterProvider.State.NOTREADY ).error( ProxyClusterProvider.State.PENDING ).on( ProxyClusterProvider.Transition.STARTING_SERVICES ).run( ProxyClusterProvider.Refresh.SERVICEREADY );

        this.from( ProxyClusterProvider.State.NOTREADY ).to( ProxyClusterProvider.State.DISABLED ).error( ProxyClusterProvider.State.NOTREADY ).on( ProxyClusterProvider.Transition.NOTREADYCHECK ).run( ProxyClusterProvider.Refresh.SERVICEREADY );

        this.from( ProxyClusterProvider.State.DISABLED ).to( ProxyClusterProvider.State.DISABLED ).error( ProxyClusterProvider.State.NOTREADY ).on( ProxyClusterProvider.Transition.DISABLEDCHECK ).addListener( ProxyClusterProvider.ErrorStateListeners.FLUSHPENDING ).run(
            ProxyClusterProvider.Refresh.SERVICEREADY );
        this.from( ProxyClusterProvider.State.DISABLED ).to( ProxyClusterProvider.State.ENABLING ).error( ProxyClusterProvider.State.DISABLED ).on( ProxyClusterProvider.Transition.ENABLE ).run( ProxyClusterProvider.ServiceStateDispatch.ENABLED );
        this.from( ProxyClusterProvider.State.DISABLED ).to( ProxyClusterProvider.State.STOPPED ).error( ProxyClusterProvider.State.PENDING ).on( ProxyClusterProvider.Transition.STOP ).run( noop );

        this.from( ProxyClusterProvider.State.ENABLED ).to( ProxyClusterProvider.State.DISABLED ).error( ProxyClusterProvider.State.NOTREADY ).on( ProxyClusterProvider.Transition.DISABLE ).run( ProxyClusterProvider.ServiceStateDispatch.DISABLED );

        this.from( ProxyClusterProvider.State.ENABLING ).to( ProxyClusterProvider.State.ENABLING_RESOURCES ).error( ProxyClusterProvider.State.NOTREADY ).on( ProxyClusterProvider.Transition.ENABLING_RESOURCES ).run( ProxyClusterProvider.Refresh.RESOURCES );
        this.from( ProxyClusterProvider.State.ENABLING_RESOURCES ).to( ProxyClusterProvider.State.ENABLING_VMS ).error( ProxyClusterProvider.State.NOTREADY ).on( ProxyClusterProvider.Transition.ENABLING_VMS ).run( ProxyClusterProvider.Refresh.INSTANCES );
        this.from( ProxyClusterProvider.State.ENABLING_VMS ).to( ProxyClusterProvider.State.ENABLING_VMS_PASS_TWO ).error( ProxyClusterProvider.State.NOTREADY ).on( ProxyClusterProvider.Transition.ENABLING_VMS_PASS_TWO ).run( ProxyClusterProvider.Refresh.INSTANCES );
        this.from( ProxyClusterProvider.State.ENABLING_VMS_PASS_TWO ).to( ProxyClusterProvider.State.ENABLED ).error( ProxyClusterProvider.State.NOTREADY ).on( ProxyClusterProvider.Transition.ENABLING_VMS_PASS_TWO ).run( ProxyClusterProvider.Refresh.INSTANCES );

        this.from( ProxyClusterProvider.State.ENABLED ).to( ProxyClusterProvider.State.ENABLED_SERVICE_CHECK ).error( ProxyClusterProvider.State.NOTREADY ).on( ProxyClusterProvider.Transition.ENABLED_SERVICES ).run( ProxyClusterProvider.Refresh.SERVICEREADY );
        this.from( ProxyClusterProvider.State.ENABLED_SERVICE_CHECK ).to( ProxyClusterProvider.State.ENABLED_RSC ).error( ProxyClusterProvider.State.NOTREADY ).on( ProxyClusterProvider.Transition.ENABLED_RSC ).run( ProxyClusterProvider.Refresh.RESOURCES );
        this.from( ProxyClusterProvider.State.ENABLED_RSC ).to( ProxyClusterProvider.State.ENABLED_VMS ).error( ProxyClusterProvider.State.NOTREADY ).on( ProxyClusterProvider.Transition.ENABLED_VMS ).run( ProxyClusterProvider.Refresh.INSTANCES );
        this.from( ProxyClusterProvider.State.ENABLED_VMS ).to( ProxyClusterProvider.State.ENABLED ).error( ProxyClusterProvider.State.NOTREADY ).on( ProxyClusterProvider.Transition.ENABLED ).run( ProxyClusterProvider.ErrorStateListeners.FLUSHPENDING );
      }
    }.newAtomicMarkedState( );
  }

  public void init( final Cluster cluster ) {
    this.cluster = cluster;
  }

  public ClusterConfiguration getConfiguration( ) {
    return configuration;
  }

  public void check( ) {
    final ProxyClusterProvider.State currentState = this.stateMachine.getState( );
    final List<Throwable> currentErrors = Lists.newArrayList( this.pendingErrors );
    this.pendingErrors.clear( );
    try {
      if ( Component.State.ENABLED.equals( this.configuration.lookupState( ) ) ) {
        enabledTransition( ).call( ).get( );
      } else if ( Component.State.DISABLED.equals( this.configuration.lookupState( ) ) ) {
        disabledTransition( ).call( ).get( );
      } else if ( Component.State.NOTREADY.equals( this.configuration.lookupState( ) ) ) {
        notreadyTransition( ).call( ).get( );
      } else {
        Refresh.SERVICEREADY.fire( this );
      }
    } catch ( Exception ex ) {
      //ignore cancellation errors.
      if ( !(ex.getCause( ) instanceof CancellationException) ) {
        currentErrors.add( ex );
      }
    }
    final Component.State externalState = this.configuration.lookupState( );
    if ( !currentErrors.isEmpty( ) ) {
      throw Faults.failure( this.configuration, currentErrors );
    } else if ( Component.State.ENABLED.equals( externalState ) && ( ProxyClusterProvider.State.ENABLING.ordinal( ) >= currentState.ordinal( ) ) ) {
      final IllegalStateException ex = new IllegalStateException( "Cluster is currently reported as " + externalState
          + " but is really "
          + currentState
          + ":  please see logs for additional information." );
      currentErrors.add( ex );
      throw Faults.failure( this.configuration, currentErrors );
    }
  }

  @Override
  public void start( ) throws ServiceRegistrationException {
    if ( !State.DISABLED.equals( this.stateMachine.getState( ) ) ) {
      final Callable<CheckedListenableFuture<ProxyClusterProvider>> trans = startingTransition( );
      for ( int i = 0; i < ProxyClusterConfigurations.getConfiguration( ).getStartupSyncRetries( ); i++ ) {
        try {
          trans.call( ).get( );
          break;
        } catch ( final InterruptedException ex ) {
          Thread.currentThread( ).interrupt( );
        } catch ( final Exception ex ) {
          Logs.extreme( ).debug( ex, ex );
        }
      }
      Listeners.register(Hertz.class, this);
    }
  }

  @Override
  public void stop( ) throws ServiceRegistrationException {
    try {
      Automata.sequenceTransitions( this, State.DISABLED, State.STOPPED ).call( ).get( );
    } catch ( final InterruptedException ex ) {
      Thread.currentThread( ).interrupt( );
    } catch ( final Exception ex ) {
      Logs.extreme( ).debug( ex, ex );
      throw new ServiceRegistrationException( "Failed to call stop() on cluster " + this.configuration
          + " because of: "
          + ex.getMessage( ), ex );
    } finally {
      try {
        ListenerRegistry.getInstance( ).deregister( Hertz.class, this );
      } catch ( Exception ignore ) {}
      try {
        ListenerRegistry.getInstance( ).deregister( ClockTick.class, this );
      } catch ( Exception ignore ) {}
    }
  }

  @Override
  public void enable( ) throws ServiceRegistrationException {
    if ( State.ENABLING.ordinal( ) > this.stateMachine.getState( ).ordinal( ) ) {
      try {
        final Callable<CheckedListenableFuture<ProxyClusterProvider>> trans = enablingTransition( );
        RuntimeException fail = null;
        for ( int i = 0; i < ProxyClusterConfigurations.getConfiguration( ).getStartupSyncRetries( ); i++ ) {
          try {
            trans.call( ).get( );
            fail = null;
            break;
          } catch ( Exception ex ) {
            try {
              TimeUnit.SECONDS.sleep( 1 );
            } catch ( Exception ex1 ) {
              LOG.error( ex1, ex1 );
            }
            fail = Exceptions.toUndeclared( ex );
          }
        }
        if ( fail != null ) {
          throw fail;
        }
      } catch ( final Exception ex ) {
        Logs.extreme( ).debug( ex, ex );
        throw new ServiceRegistrationException( "Failed to call enable() on cluster " + this.configuration
            + " because of: "
            + ex.getMessage( ), ex );
      }
    }
  }

  @Override
  public void disable( ) throws ServiceRegistrationException {
    try {
      if ( State.NOTREADY.equals( this.getStateMachine( ).getState( ) ) ) {
        Automata.sequenceTransitions( this, State.NOTREADY, State.DISABLED ).call( ).get( );
      } else if ( State.ENABLED.equals( this.getStateMachine( ).getState( ) ) ) {
        Automata.sequenceTransitions( this, State.ENABLED, State.DISABLED ).call( ).get( );
      }
    } catch ( final InterruptedException ex ) {
      Thread.currentThread( ).interrupt( );
    } catch ( final Exception ex ) {
      Logs.extreme( ).debug( ex, ex );
    }
  }

  public void refreshResources() {
    Refresh.RESOURCES.fire( this );
  }

  @Override
  public void updateNodeInfo( final long time, final List<NodeType> nodes ) {
    Callable<Boolean> updateNodes = ( ) -> {
      try {
        Nodes.updateNodeInfo( time, getConfiguration(), nodes );
        return true;
      } catch ( Exception e ) {
        LOG.error( e, e );
        LOG.trace( e, e );
        return false;
      }
    };
    //GRZE: submit the node controller state updates in a separate thread to ensure it doesn't interfere with the Cluster state machine.
    Threads.enqueue( ProxyNodeController.class, ResourceStateCallback.class, updateNodes );
  }

  @Override
  public boolean hasNode( final String sourceHost ) {
    try {
      Nodes.lookup( getConfiguration( ), sourceHost );
      return true;
    } catch ( NoSuchElementException e ) {
      return false;
    }
  }

  @Override
  public void cleanup( final Cluster cluster, final Exception ex ) {
    Nodes.clusterCleanup( cluster, ex );
  }

  public boolean checkCerts( final NodeCertInfo certs ) {
    if ( ( certs == null ) || ( certs.getCcCert( ) == null )
        || ( certs.getNcCert( ) == null ) ) {
      return false;
    }

    final X509Certificate clusterx509 = PEMFiles.getCert( B64.standard.dec(certs.getCcCert()));
    final X509Certificate nodex509 = PEMFiles.getCert( B64.standard.dec( certs.getNcCert( ) ) );
    if ( "self".equals( certs.getServiceTag( ) ) || ( certs.getServiceTag( ) == null ) ) {
      return ( this.checkCerts( this.getClusterCertificate( ), clusterx509 ) )
          && ( this.checkCerts( this.getNodeCertificate( ), nodex509 ) );
    } else if ( this.cluster != null && this.cluster.getNodeMap( ).containsKey( certs.getServiceTag( ) ) ) {
      final NodeInfo nodeInfo = this.cluster.getNodeMap( ).get( certs.getServiceTag( ) );
      nodeInfo.setHasClusterCert( this.checkCerts( this.getClusterCertificate( ), clusterx509 ) );
      nodeInfo.setHasNodeCert( this.checkCerts( this.getNodeCertificate( ), nodex509 ) );
      return nodeInfo.getHasClusterCert( ) && nodeInfo.getHasNodeCert( );
    } else {
      LOG.error( "Cluster " + this.getName( )
          + " failed to find cluster/node info for service tag: "
          + certs.getServiceTag( ) );
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

  private ServiceConfiguration getLogServiceConfiguration( ) {
    final ComponentId glId = ComponentIds.lookup( ProxyClusterController.GatherLogService.class );
    final ServiceConfiguration conf = this.getConfiguration( );
    final URI glUri =
        ServiceUris.remote( ComponentIds.lookup( ProxyClusterController.GatherLogService.class ), conf.getInetAddress( ), conf.getPort( ) );
    return ServiceConfigurations.createEphemeral( glId, conf.getPartition( ), conf.getName( ), glUri );
  }

  @Override
  public void fireEvent( final Event event ) {
    if ( !Bootstrap.isFinished( ) ) {
      LOG.info(this.getFullName() + " skipping clock event because bootstrap isn't finished");
    } else if ( Hosts.isCoordinator( ) && event instanceof Hertz ) {
      this.fireClockTick((Hertz) event);
    }
  }

  private static SubjectRemoteCallbackFactory<SubjectMessageCallback<Cluster, ?, ?>, Cluster> newSubjectMessageFactory(
      final Class<? extends SubjectMessageCallback<Cluster, ?, ?>> callbackClass,
      final ProxyClusterProvider spi
  ) throws CancellationException {
    return new SubjectRemoteCallbackFactory<SubjectMessageCallback<Cluster, ?, ?>, Cluster>( ) {
      @Override
      public SubjectMessageCallback<Cluster, ?, ?> newInstance( ) {
        try {
          final Cluster subject = getSubject( );
          if ( subject != null ) {
            try {
              return Classes.builder( callbackClass ).arg( subject ).newInstance( );
            } catch ( UndeclaredThrowableException ex ) {
              if ( ex.getCause( ) instanceof CancellationException ) {
                throw ( CancellationException ) ex.getCause( );
              } else if ( ex.getCause( ) instanceof NoSuchMethodException ) {
                try {
                  SubjectMessageCallback<Cluster, ?, ?> callback = Classes.builder( callbackClass ).newInstance( );
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
                SubjectMessageCallback<Cluster, ?, ?> callback = callbackClass.newInstance( );
                LOG.error( "Creating uninitialized callback (subject=" + subject + ") for type: " + callbackClass.getCanonicalName( ) );
                return callback;
              }
            } catch ( RuntimeException ex ) {
              LOG.error( "Failed to create instance of: " + callbackClass );
              Logs.extreme( ).error( ex, ex );
              throw ex;
            }
          } else {
            SubjectMessageCallback<Cluster, ?, ?> callback = callbackClass.newInstance( );
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
      public Cluster getSubject( ) {
        return spi.cluster;
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
      LOG.error( this.getName( ) + ": Error communicating with cluster: "
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

  @Override
  public StateMachine<ProxyClusterProvider, ProxyClusterProvider.State, ProxyClusterProvider.Transition> getStateMachine( ) {
    return stateMachine;
  }

  @Override
  public String getPartition() {
    return configuration.getPartition( );
  }

  @Override
  public Partition lookupPartition() {
    return Partitions.lookup( configuration );
  }

  @Override
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
  public int compareTo( @Nonnull final ProxyClusterProvider that ) {
    return this.getName( ).compareTo( that.getName( ) );
  }

  private enum ZoneRegistration implements Predicate<ProxyClusterProvider> {
    REGISTER {
      @Override
      public boolean apply( final ProxyClusterProvider input ) {
        final Cluster cluster = input.cluster;
        if ( cluster != null ) {
          ClusterRegistry.getInstance( ).register( cluster );
        }
        return true;
      }
    },
    DEREGISTER {
      @Override
      public boolean apply( final ProxyClusterProvider input ) {
        final Cluster cluster = input.cluster;
        if ( cluster != null ) {
          ClusterRegistry.getInstance( ).register( cluster );
        }
        return true;
      }
    }
  }

  private enum ServiceStateDispatch implements Predicate<ProxyClusterProvider>, RemoteCallback<ServiceTransitionType, ServiceTransitionType> {
    STARTED( ClusterStartServiceType.class ),
    ENABLED( ClusterEnableServiceType.class ) {
      @Override
      public boolean apply( final ProxyClusterProvider input ) {
        try {
          if ( Bootstrap.isOperational( ) ) {
            super.apply( input );
          }
          ProxyClusterProvider.ZoneRegistration.REGISTER.apply( input );
          return true;
        } catch ( final Exception t ) {
          return input.swallowException( t );
        }
      }
    },
    DISABLED( ClusterDisableServiceType.class ) {
      @Override
      public boolean apply( final ProxyClusterProvider input ) {
        try {
          if ( Bootstrap.isOperational( ) ) {
            super.apply( input );
          }
          ProxyClusterProvider.ZoneRegistration.DEREGISTER.apply( input );
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
    public boolean apply( final ProxyClusterProvider input ) {
      if ( Hosts.isCoordinator( ) ) {
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
    public void initialize( ServiceTransitionType request ) throws Exception {}

    @Override
    public void fireException( Throwable t ) {
      Logs.extreme( ).error( t, t );
    }
  }

  private enum LogRefresh implements Function<ProxyClusterProvider, TransitionAction<ProxyClusterProvider>> {
    CERTS( ProxyClusterCertsCallback.class );
    Class<? extends SubjectMessageCallback<Cluster,?,?>> refresh;

    LogRefresh( final Class<? extends SubjectMessageCallback<Cluster,?,?>>refresh ) {
      this.refresh = refresh;
    }

    @Override
    public TransitionAction<ProxyClusterProvider> apply( final ProxyClusterProvider cluster ) {
      final SubjectRemoteCallbackFactory<? extends RemoteCallback<?,?>, Cluster> factory = newSubjectMessageFactory( this.refresh, cluster );
      return new AbstractTransitionAction<ProxyClusterProvider>( ) {

        @Override
        public final void leave( final ProxyClusterProvider parent, final Callback.Completion transitionCallback ) {
          ProxyClusterProvider.fireCallback( parent, parent.getLogServiceConfiguration( ), false, factory, transitionCallback );
        }
      };
    }
  }

  private static class ServiceStateCallback extends SubjectMessageCallback<Cluster, ClusterDescribeServicesType, ClusterDescribeServicesResponseType> {
    public ServiceStateCallback( ) {
      this.setRequest( new ClusterDescribeServicesType( ) );
    }

    @Override
    public void fire( final ClusterDescribeServicesResponseType msg ) {
      List<ServiceStatusType> serviceStatuses = msg.getServiceStatuses();
      Cluster parent = this.getSubject();
      ProxyClusterProvider proxyClusterSpi = (ProxyClusterProvider) parent.getClusterProvider( );
      LOG.debug( "DescribeServices for " + parent.getFullName( ) );
      if ( serviceStatuses.isEmpty( ) ) {
        throw new NoSuchElementException( "Failed to find service info for cluster: " + parent.getFullName( ) );
      } else if ( !Bootstrap.isOperational( ) ) {
        return;
      } else {
        ServiceConfiguration config = parent.getConfiguration( );
        for ( ServiceStatusType status : serviceStatuses ) {
          if ( "self".equals( status.getServiceId( ).getName( ) ) || !config.getName( ).equals( status.getServiceId( ).getName( ) ) ) {
            status.setServiceId( TypeMappers.transform( parent.getConfiguration( ), ServiceId.class ) );
          }
          if ( status.getServiceId() == null || status.getServiceId().getName() == null || status.getServiceId().getType() == null ) {
            LOG.error( "Received invalid service id: " + status );
          } else if ( config.getName( ).equals( status.getServiceId( ).getName( ) )
              && Components.lookup( ProxyClusterController.class ).getName().equals( status.getServiceId( ).getType() ) ) {
            LOG.debug( "Found service info: " + status );
            Component.State serviceState = Component.State.valueOf( status.getLocalState( ) );
            Component.State localState = parent.getConfiguration( ).lookupState( );
            Faults.CheckException ex = Faults.transformToExceptions( ).apply( status );
            if ( Component.State.NOTREADY.equals( serviceState ) ) {
              throw new IllegalStateException( ex );
            } else if ( Component.State.ENABLED.equals( serviceState ) && Component.State.DISABLED.ordinal( ) >= localState.ordinal( ) ) {
              ProxyClusterProvider.ServiceStateDispatch.DISABLED.apply( proxyClusterSpi );
            } else if ( Component.State.DISABLED.equals( serviceState ) && Component.State.ENABLED.equals( localState ) ) {
              ProxyClusterProvider.ServiceStateDispatch.ENABLED.apply( proxyClusterSpi );
            } else if ( Component.State.LOADED.equals( serviceState ) && Component.State.NOTREADY.ordinal( ) <= localState.ordinal( ) ) {
              ProxyClusterProvider.ServiceStateDispatch.STARTED.apply( proxyClusterSpi );
            } else if ( Component.State.NOTREADY.ordinal( ) < serviceState.ordinal( ) ) {
              proxyClusterSpi.clearExceptions( );
            }
            return;
          }
        }
      }
      LOG.error("Failed to find service info for cluster: " + parent.getFullName()
          + " instead found service status for: "
          + serviceStatuses);
      throw new NoSuchElementException( "Failed to find service info for cluster: " + parent.getFullName( ) );
    }

    @Override
    public void setSubject( Cluster subject ) {
      this.getRequest( ).getServices( ).add( TypeMappers.transform( subject.getConfiguration( ), ServiceId.class ) );
      super.setSubject( subject );
    }
  }

  private enum Refresh implements Function<ProxyClusterProvider, TransitionAction<ProxyClusterProvider>> {
    RESOURCES( ResourceStateCallback.class ),
    INSTANCES( VmStateCallback.class ),
    SERVICEREADY( ServiceStateCallback.class );
    Class<? extends SubjectMessageCallback<Cluster,?,?>>  refresh;

    Refresh( final Class<? extends SubjectMessageCallback<Cluster,?,?>> refresh ) {
      this.refresh = refresh;
    }

    @SuppressWarnings( { "rawtypes", "unchecked" } )
    @Override
    public TransitionAction<ProxyClusterProvider> apply( final ProxyClusterProvider cluster ) {
      final SubjectRemoteCallbackFactory<? extends RemoteCallback<?,?>, Cluster> factory = newSubjectMessageFactory( this.refresh, cluster );
      return new AbstractTransitionAction<ProxyClusterProvider>( ) {

        @SuppressWarnings( "rawtypes" )
        @Override
        public final void leave( final ProxyClusterProvider parent, final Callback.Completion transitionCallback ) {
          ProxyClusterProvider.fireCallback( parent, factory, transitionCallback );
        }
      };
    }

    public void fire( ProxyClusterProvider input ) {
      final SubjectRemoteCallbackFactory<? extends RemoteCallback<?,?>, Cluster> factory = newSubjectMessageFactory( this.refresh, input );
      try {
        RemoteCallback messageCallback = factory.newInstance( );
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

  private static void fireCallback( final ProxyClusterProvider parent, final SubjectRemoteCallbackFactory<? extends RemoteCallback<?,?>, Cluster> factory, final Callback.Completion transitionCallback ) {
    fireCallback( parent, parent.getConfiguration( ), true, factory, transitionCallback );
  }

  private static void fireCallback( final ProxyClusterProvider parent,
                                    final ServiceConfiguration config,
                                    final boolean doCoordinatorCheck,
                                    final SubjectRemoteCallbackFactory<? extends RemoteCallback<?,?>, Cluster> factory,
                                    final Callback.Completion transitionCallback ) {
    RemoteCallback messageCallback = null;
    try {
      if ( !doCoordinatorCheck || checkCoordinator( transitionCallback ) ) {
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
      } else {
        transitionCallback.fire( );
      }
    } finally {
      if ( !transitionCallback.isDone( ) ) {
        LOG.debug( parent.getFullName( ) + " transition fell through w/o completing: " + messageCallback );
        Logs.extreme( ).debug( Exceptions.toUndeclared( parent.getFullName( ) + " transition fell through w/o completing: " + messageCallback ) );
        transitionCallback.fire( );
      }
    }
  }

  private static boolean checkCoordinator( final Callback.Completion transitionCallback ) {
    try {
      boolean coordinator = Hosts.isCoordinator( );
      if ( !coordinator ) {
        transitionCallback.fire( );
        return false;
      }
    } catch ( Exception ex ) {
      transitionCallback.fire( );
      return false;
    }
    return true;
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
    ENABLING,
    ENABLING_RESOURCES,
    ENABLING_VMS,
    ENABLING_VMS_PASS_TWO,
    /** Component.State.ENABLED -> Component.State.ENABLED **/
    ENABLED,
    ENABLED_SERVICE_CHECK,
    ENABLED_RSC,
    ENABLED_VMS,
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
    ENABLING_RESOURCES,
    ENABLING_VMS,
    ENABLING_VMS_PASS_TWO,

    ENABLED,
    ENABLED_VMS,
    ENABLED_SERVICES,
    ENABLED_RSC,

    DISABLE,
    DISABLEDCHECK,

    STOP,

  }

  private enum ErrorStateListeners implements Callback<ProxyClusterProvider> {
    FLUSHPENDING {
      @Override
      public void fire( final ProxyClusterProvider t ) {
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

  private void fireClockTick( final Hertz tick ) {
    try {
      try {
        this.configuration.lookupState( );
      } catch ( final NoSuchElementException ex1 ) {
        this.stop( );
        return;
      }
    } catch ( final Exception ex ) {
      LOG.error( ex, ex );
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
      State.ENABLING,
      State.ENABLING_RESOURCES,
      State.ENABLING_VMS,
      State.ENABLING_VMS_PASS_TWO,
      State.ENABLED };

  private static final State[] PATH_ENABLED_CHECK = new State[] { State.ENABLED,
      State.ENABLED_SERVICE_CHECK,
      State.ENABLED_RSC,
      State.ENABLED_VMS,
      State.ENABLED };

  private Callable<CheckedListenableFuture<ProxyClusterProvider>> enabledTransition( ) {
    if ( this.stateMachine.getState( ).ordinal( ) >= State.ENABLED.ordinal( ) ) {
      return Automata.sequenceTransitions( this, PATH_ENABLED_CHECK );
    } else {
      return Automata.sequenceTransitions( this, PATH_ENABLED );
    }
  }

  private Callable<CheckedListenableFuture<ProxyClusterProvider>> enablingTransition( ) {
    return Automata.sequenceTransitions( this, PATH_ENABLED );
  }

  private Callable<CheckedListenableFuture<ProxyClusterProvider>> disabledTransition( ) {
    if ( this.stateMachine.getState( ).ordinal( ) >= State.ENABLED.ordinal( ) ) {
      return Automata.sequenceTransitions( this, ObjectArrays.concat( PATH_ENABLED_CHECK, State.DISABLED ) );
    } else {
      return Automata.sequenceTransitions( this, ObjectArrays.concat( PATH_DISABLED, State.DISABLED ) );
    }
  }

  private Callable<CheckedListenableFuture<ProxyClusterProvider>> notreadyTransition( ) {
    if ( this.stateMachine.getState( ).ordinal( ) >= State.ENABLED.ordinal( ) ) {
      return Automata.sequenceTransitions( this, ObjectArrays.concat( PATH_ENABLED_CHECK, State.DISABLED ) );
    } else {
      return Automata.sequenceTransitions( this, PATH_DISABLED );
    }
  }

  private Callable<CheckedListenableFuture<ProxyClusterProvider>> startingTransition( ) {
    return Automata.sequenceTransitions( this, PATH_DISABLED );
  }

  private X509Certificate getClusterCertificate( ) {
    return Partitions.lookup( this.configuration ).getCertificate();
  }

  private X509Certificate getNodeCertificate( ) {
    return Partitions.lookup( this.configuration ).getNodeCertificate();
  }

  @TypeMapper
  public enum ServiceConfigurationToProxyClusterSpi implements Function<ClusterConfiguration,ClusterProvider> {
    @SuppressWarnings( "unused" )
    INSTANCE;

    @Nullable
    @Override
    public ClusterProvider apply( @Nullable final ClusterConfiguration configuration ) {
      return new ProxyClusterProvider( configuration );
    }
  }
}
