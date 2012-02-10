package com.eucalyptus.cluster.callback;

import org.apache.log4j.Logger;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.util.async.FailedRequestException;
import com.eucalyptus.util.async.SubjectMessageCallback;
import edu.ucsb.eucalyptus.msgs.DescribePublicAddressesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribePublicAddressesType;

public class PublicAddressStateCallback extends SubjectMessageCallback<Cluster, DescribePublicAddressesType, DescribePublicAddressesResponseType> {
  private static Logger LOG = Logger.getLogger( PublicAddressStateCallback.class );
  
  public PublicAddressStateCallback( ) {
    super( ( DescribePublicAddressesType ) new DescribePublicAddressesType( ).regarding( ) );
  }
  
  @Override
  public void fire( DescribePublicAddressesResponseType reply ) {
    this.getSubject( ).getState( ).setPublicAddressing( true );
    try {
      Addresses.getAddressManager( ).update( this.getSubject( ), reply.getAddresses( ) );
    } catch ( Exception ex ) {
      LOG.error( ex );
    }
  }
  
  @Override
  public void fireException( Throwable t ) {
    if( t instanceof FailedRequestException ) {
      LOG.warn( "Response from cluster [" + this.getSubject( ).getName( ) + "]: " + t.getMessage( ) );
    } else {
      LOG.warn( "[" + this.getSubject( ).getName( ) + "]: " + t.getMessage( ) );
    }
    if( t instanceof FailedRequestException ) {
      this.getSubject( ).getState( ).setPublicAddressing( false );
    } else {
      LOG.error( t, t );
    }
  }
  
}
