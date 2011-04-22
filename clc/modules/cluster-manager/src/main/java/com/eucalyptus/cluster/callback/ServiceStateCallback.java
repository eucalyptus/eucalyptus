package com.eucalyptus.cluster.callback;

import java.util.List;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.event.LifecycleEvents;
import com.eucalyptus.empyrean.DescribeServicesResponseType;
import com.eucalyptus.empyrean.DescribeServicesType;
import com.eucalyptus.empyrean.ServiceStatusType;
import com.eucalyptus.util.async.SubjectMessageCallback;

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
          this.getSubject( ).fireEvent( LifecycleEvents.info( this.getRequest( ).getCorrelationId( ), status ) );
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
