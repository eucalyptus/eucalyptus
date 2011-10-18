package com.eucalyptus.cluster.callback;

import java.util.List;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.LifecycleEvents;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.component.ServiceChecks;
import com.eucalyptus.component.ServiceChecks.CheckException;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.empyrean.DescribeServicesResponseType;
import com.eucalyptus.empyrean.DescribeServicesType;
import com.eucalyptus.empyrean.ServiceStatusType;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.async.SubjectMessageCallback;
import com.eucalyptus.util.fsm.Automata;

public class ServiceStateCallback extends SubjectMessageCallback<Cluster, DescribeServicesType, DescribeServicesResponseType> {
  private static Logger LOG = Logger.getLogger( ServiceStateCallback.class );
  
  public ServiceStateCallback( ) {
    this.setRequest( new DescribeServicesType( ) );
  }
  
  @Override
  public void fire( DescribeServicesResponseType msg ) {
    List<ServiceStatusType> serviceStatuses = msg.getServiceStatuses( );
    if ( serviceStatuses.isEmpty( ) ) {
      throw new NoSuchElementException( "Failed to find service info for cluster: " + this.getSubject( ).getConfiguration( ) );
    } else {
      ServiceConfiguration config = this.getSubject( ).getConfiguration( );
      for ( ServiceStatusType status : serviceStatuses ) {
        if ( config.getName( ).equals( status.getServiceId( ).getName( ) ) ) {
          LOG.debug( "Found service info: " + status );
          Component.State serviceState = Component.State.valueOf( status.getLocalState( ) );
          Component.State localState = this.getSubject( ).getConfiguration( ).lookupState( );
          Component.State proxyState = this.getSubject( ).getStateMachine( ).getState( ).proxyState( );
          CheckException ex = ServiceChecks.chainCheckExceptions( ServiceChecks.Functions.statusToCheckExceptions( this.getRequest( ).getCorrelationId( ) ).apply( status ) );
          if ( Component.State.NOTREADY.equals( serviceState ) ) {
            throw new IllegalStateException( ex );
          } else if ( Component.State.NOTREADY.equals( localState )
                      && Component.State.NOTREADY.ordinal( ) < serviceState.ordinal( ) ) {
            LifecycleEvents.fireExceptionEvent( this.getSubject( ).getConfiguration( ), ServiceChecks.Severity.DEBUG, ex );
            this.getSubject( ).clearExceptions( );
          } else if ( proxyState.ordinal( ) > serviceState.ordinal( ) || localState.ordinal( ) > serviceState.ordinal( ) ) {
            Threads.enqueue( this.getSubject( ).getConfiguration( ),
                             Automata.sequenceTransitions( this.getSubject( ),
                                                           Cluster.State.ENABLED,
                                                           Cluster.State.DISABLED ) );
          } else {
            LifecycleEvents.fireExceptionEvent( this.getSubject( ).getConfiguration( ), ServiceChecks.Severity.INFO, ex );
          }
        } else {
          LOG.error( "Found information for unknown service: " + status );
        }
      }
    }
  }
  
  @Override
  public void fireException( Throwable t ) {
    LOG.error( t, t );
  }
  
}
