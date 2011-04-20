package com.eucalyptus.cluster.callback;

import org.apache.log4j.Logger;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.empyrean.DescribeServicesResponseType;
import com.eucalyptus.empyrean.DescribeServicesType;
import com.eucalyptus.util.async.SubjectMessageCallback;

public class ServiceStateCallback extends SubjectMessageCallback<Cluster, DescribeServicesType, DescribeServicesResponseType> {
  private static Logger LOG = Logger.getLogger( ServiceStateCallback.class );
  
  public ServiceStateCallback( ) {
    this.setRequest( new DescribeServicesType( ) );
  }
  
  @Override
  public void fire( DescribeServicesResponseType msg ) {
    LOG.debug( msg );
  }
  
}
