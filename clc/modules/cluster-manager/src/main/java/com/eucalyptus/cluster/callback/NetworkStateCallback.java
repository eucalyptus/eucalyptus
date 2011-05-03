package com.eucalyptus.cluster.callback;

import java.net.SocketException;
import java.util.List;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.FakePrincipals;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.Networks;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.async.FailedRequestException;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.Network;
import edu.ucsb.eucalyptus.cloud.NetworkToken;
import edu.ucsb.eucalyptus.msgs.DescribeNetworksResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeNetworksType;
import edu.ucsb.eucalyptus.msgs.NetworkInfoType;

public class NetworkStateCallback extends StateUpdateMessageCallback<Cluster, DescribeNetworksType, DescribeNetworksResponseType> {
  private static Logger LOG = Logger.getLogger( NetworkStateCallback.class );
  
  public NetworkStateCallback( ) {
    this.setRequest( new DescribeNetworksType( ) {
      {
        regarding( );
        setClusterControllers( Lists.newArrayList( Clusters.getInstance( ).getClusterAddresses( ) ) );
        setNameserver( Internets.getAllAddresses( ).get( 0 ) );//TODO:GRZE:FIXTHISDFSDFSDF
      }
    } );
  }
  
  /**
   * @see com.eucalyptus.util.async.MessageCallback#fire(edu.ucsb.eucalyptus.msgs.BaseMessage)
   * @param reply
   */
  @Override
  public void fire( DescribeNetworksResponseType reply ) {
    for ( Network net : Networks.getInstance( ).listValues( ) ) {
      net.trim( reply.getAddrsPerNet( ) );
    }
    this.getSubject( ).getState( ).setAddressCapacity( reply.getAddrsPerNet( ) );
    this.getSubject( ).getState( ).setMode( reply.getUseVlans( ) );
    for ( NetworkInfoType netInfo : reply.getActiveNetworks( ) ) {
      Network net = null;
      try {
        net = Networks.getInstance( ).lookup( netInfo.getAccountId( ) + "-" + netInfo.getNetworkName( ) );
        if ( net.getVlan( ).equals( Integer.valueOf( 0 ) ) && net.initVlan( netInfo.getVlan( ) ) ) {
          NetworkToken netToken = new NetworkToken( this.getSubject( ).getName( ), netInfo.getAccountId( ), netInfo.getNetworkName( ), netInfo.getUuid( ), netInfo.getVlan( ) );
          netToken = net.addTokenIfAbsent( netToken );
        }
      } catch ( NoSuchElementException e1 ) {
        try {
          AccountFullName accountFn = Accounts.lookupAccountFullNameById( netInfo.getAccountId( ) );
          if( accountFn != null ) {
            net = new Network( accountFn, netInfo.getNetworkName( ), netInfo.getUuid( ) );
            if ( net.getVlan( ).equals( Integer.valueOf( 0 ) ) && net.initVlan( netInfo.getVlan( ) ) ) {
              NetworkToken netToken = new NetworkToken( this.getSubject( ).getName( ), netInfo.getAccountId( ), netInfo.getNetworkName( ), netInfo.getUuid( ), netInfo.getVlan( ) );
              netToken = net.addTokenIfAbsent( netToken );
            }
          }
        } catch ( Exception ex ) {
          LOG.error( ex );
        }
      }
    }
    
    for ( Network net : Networks.getInstance( ).listValues( Networks.State.ACTIVE ) ) {
      net.trim( reply.getAddrsPerNet( ) );
//TODO: update the network index/token state here.  ultimately needed for failure modes.
    }
    List<Cluster> ccList = Clusters.getInstance( ).listValues( );
    int ccNum = ccList.size( );
    for ( Cluster c : ccList ) {
      ccNum -= c.getState( ).getMode( );
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
