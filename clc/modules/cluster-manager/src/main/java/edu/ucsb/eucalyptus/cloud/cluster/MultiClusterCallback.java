/**
 * 
 */
package edu.ucsb.eucalyptus.cloud.cluster;

import java.util.List;

import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.util.LogUtil;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;

public abstract class MultiClusterCallback<TYPE extends EucalyptusMessage> extends QueuedEventCallback<TYPE> {
  
  public abstract MultiClusterCallback<TYPE> newInstance( );
  
  public List<QueuedEvent> fireEventAsyncToAllClusters( final TYPE msg ) {
    List<QueuedEvent> callbackList = Lists.newArrayList( );
    for ( final Cluster c : Clusters.getInstance( ).listValues( ) ) {
      LOG.debug( "-> Sending " + msg.getClass( ).getSimpleName( ) + " network to: " + c.getUri( ) );
      LOG.debug( LogUtil.dumpObject( msg ) );
      try {
        MultiClusterCallback<TYPE> newThis = this.newInstance( );
        QueuedEvent newEvent = QueuedEvent.make(newThis, msg );
        c.getMessageQueue().enqueue( newEvent );
        callbackList.add( newEvent );
      } catch ( final Throwable e ) {
        LOG.error( "Error while sending to: " + c.getUri( ) + " " + msg.getClass( ).getSimpleName( ) );
      }
    }
    return callbackList;
  }
  
}
