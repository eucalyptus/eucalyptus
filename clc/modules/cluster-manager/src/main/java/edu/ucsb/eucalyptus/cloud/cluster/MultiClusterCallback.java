/**
 * 
 */
package edu.ucsb.eucalyptus.cloud.cluster;

import java.util.List;

import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.util.LogUtil;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;

public abstract class MultiClusterCallback<TYPE extends BaseMessage,RTYPE extends BaseMessage> extends QueuedEventCallback<TYPE,RTYPE> {
  
  public abstract MultiClusterCallback<TYPE,RTYPE> newInstance( );
  
  public List<QueuedEventCallback> fireEventAsyncToAllClusters( final TYPE msg ) {
    List<QueuedEventCallback> callbackList = Lists.newArrayList( );
    for ( final Cluster c : Clusters.getInstance( ).listValues( ) ) {
      LOG.debug( "-> Sending " + msg.getClass( ).getSimpleName( ) + " network to: " + c.getUri( ) );
      LOG.debug( LogUtil.dumpObject( msg ) );
      try {
        MultiClusterCallback<TYPE,RTYPE> newThis = this.newInstance( );
        Clusters.dispatchEvent( c, newThis.regardingUser( msg ) );
        callbackList.add( newThis );
      } catch ( final Throwable e ) {
        LOG.error( "Error while sending to: " + c.getUri( ) + " " + msg.getClass( ).getSimpleName( ) );
      }
    }
    return callbackList;
  }
  
}
