package com.eucalyptus.cluster.callback;

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.ClusterState;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.network.ExtantNetwork;
import com.eucalyptus.network.NetworkGroups;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.async.Callback;
import com.eucalyptus.util.async.FailedRequestException;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.DescribeNetworksResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeNetworksType;

public class NetworkStateCallback extends StateUpdateMessageCallback<Cluster, DescribeNetworksType, DescribeNetworksResponseType> {
  private static Logger LOG = Logger.getLogger( NetworkStateCallback.class );
  
  public NetworkStateCallback( ) {
    super( new DescribeNetworksType( ) {
      {
        regarding( );
        setClusterControllers( Lists.newArrayList( Clusters.getInstance( ).getClusterAddresses( ) ) );
        setNameserver( Internets.localHostAddress( ) );
      }
    } );
  }
  
  /**
   * @see com.eucalyptus.util.async.MessageCallback#fire(edu.ucsb.eucalyptus.msgs.BaseMessage)
   * @param reply
   */
  @Override
  public void fire( final DescribeNetworksResponseType reply ) {
    /** update network mode (yes/no vlan) **/
    this.getSubject( ).getState( ).setMode( reply.getUseVlans( ) );
    /** set vlan interval **/
    this.getSubject( ).getConfiguration( ).setMinVlan( reply.getVlanMin( ) );
    this.getSubject( ).getConfiguration( ).setMaxVlan( reply.getVlanMax( ) );
    ClusterState.trim( );
    /** verify network indexes **/
//    EntityWrapper<ExtantNetwork> db = EntityWrapper.get( ExtantNetwork.class );
//    List<ExtantNetwork> allNets = db.query( new ExtantNetwork( ) );
//    Transactions.each( new ExtantNetwork( ), new Callback<ExtantNetwork>( ) {
//      
//      @Override
//      public void fire( ExtantNetwork t ) {
//        for( Integer i = reply.getAddrIndexMin( ); i < reply.getAddrIndexMax( ); i++ ) {
//          Transactions.l
//        }
//      }
//      
//    } );
//    NetworkGroups.update( reply.getUseVlans( ), reply.getAddrsPerNet( ), reply.getAddrIndexMax( ), reply.getAddrIndexMax( ), reply.getActiveNetworks( ) );
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
