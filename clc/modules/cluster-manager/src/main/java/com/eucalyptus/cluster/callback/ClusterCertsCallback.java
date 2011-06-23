package com.eucalyptus.cluster.callback;

import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.util.InvalidCredentialsException;
import com.eucalyptus.util.async.FailedRequestException;
import edu.ucsb.eucalyptus.msgs.GetKeysResponseType;
import edu.ucsb.eucalyptus.msgs.GetKeysType;

public class ClusterCertsCallback extends ClusterLogMessageCallback<GetKeysType, GetKeysResponseType> {
  private static Logger LOG = Logger.getLogger( ClusterCertsCallback.class );

  public ClusterCertsCallback( ) {
    super( ( GetKeysType ) new GetKeysType( "self" ).regarding( ) );
  }
  
  /**
   * TODO: DOCUMENT
   * @param msg
   */
  @Override
  public void fire( GetKeysResponseType msg ) {
    if( !this.getSubject( ).checkCerts( msg.getCerts( ) ) ) {
      throw new InvalidCredentialsException( "Cluster credentials are invalid: " + this.getSubject( ).getName( ) );
//    } else if( !Bootstrap.isFinished( ) ) {
//      throw new InvalidCredentialsException( "Bootstrap hasn't finished yet -- stalling." );
    }
  }

  /**
   * @see com.eucalyptus.cluster.callback.StateUpdateMessageCallback#fireException(com.eucalyptus.util.async.FailedRequestException)
   * @param t
   */
  @Override
  public void fireException( FailedRequestException t ) {
    LOG.debug( "Request to " + this.getSubject( ).getName( ) + " failed: " + t.getMessage( ) );
  }

}
