package com.eucalyptus.cluster.callback;

import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.ClusterConfiguration;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.network.NetworkGroups;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Internets;
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
    NetworkStateCallback.this.updateClusterConfiguration( reply );
    NetworkGroups.updateNetworkRangeConfiguration( );
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
  
  private void updateClusterConfiguration( final DescribeNetworksResponseType reply ) {
    
    EntityTransaction db = Entities.get( NetworkStateCallback.class );
    try {
      ClusterConfiguration config = Entities.uniqueResult( this.getSubject( ).getConfiguration( ) );
      config.setNetworkMode( reply.getMode( ) );
      config.setUseNetworkTags( reply.getUseVlans( ) == 1 );
      config.setMinNetworkTag( reply.getVlanMin( ) );
      config.setMaxNetworkTag( reply.getVlanMax( ) );
      config.setAddressesPerNetwork( reply.getAddrsPerNet( ) );
      config.setVnetNetmask( reply.getVnetNetmask( ) );
      config.setVnetSubnet( reply.getVnetSubnet( ) );
      db.commit( );
    } catch ( Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      db.rollback( );
      throw ex;
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
