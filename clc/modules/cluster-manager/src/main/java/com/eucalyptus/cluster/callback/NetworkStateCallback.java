package com.eucalyptus.cluster.callback;

import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.ClusterConfiguration;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.network.NetworkGroups;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.async.FailedRequestException;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
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
        setDnsDomainName( SystemConfiguration.getSystemConfiguration( ).getDnsDomain( ).replaceAll("^\\.","") );
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
  }
  
  private void updateClusterConfiguration( final DescribeNetworksResponseType reply ) {
    EntityTransaction db = Entities.get( ClusterConfiguration.class );
    try {
      ClusterConfiguration config = Entities.uniqueResult( this.getSubject( ).getConfiguration( ) );
      config.setNetworkMode( reply.getMode( ) );
      config.setUseNetworkTags( reply.getUseVlans( ) == 1 );
      config.setMinNetworkTag( reply.getVlanMin( ) );
      config.setMaxNetworkTag( reply.getVlanMax( ) );
      config.setMinNetworkIndex( ( long ) reply.getAddrIndexMin( ).intValue( ) );
      config.setMaxNetworkIndex( ( long ) reply.getAddrIndexMax( ).intValue( ) );
      config.setAddressesPerNetwork( reply.getAddrsPerNet( ) );
      config.setVnetNetmask( reply.getVnetNetmask( ) );
      config.setVnetSubnet( reply.getVnetSubnet( ) );
      db.commit( );
    } catch ( Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      db.rollback( );
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
