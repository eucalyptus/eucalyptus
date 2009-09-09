package com.eucalyptus.network;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.eucalyptus.entities.IpRange;
import com.eucalyptus.entities.NetworkPeer;
import com.eucalyptus.entities.NetworkRule;
import com.eucalyptus.entities.NetworkRulesGroup;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.ucsb.eucalyptus.cloud.entities.UserInfo;
import edu.ucsb.eucalyptus.msgs.IpPermissionType;
import edu.ucsb.eucalyptus.msgs.SecurityGroupItemType;
import edu.ucsb.eucalyptus.msgs.UserIdGroupPairType;

public class NetworkGroupUtil {
  
  public static EntityWrapper<NetworkRulesGroup> getEntityWrapper( ) {
    EntityWrapper<NetworkRulesGroup> db = new EntityWrapper<NetworkRulesGroup>( );
    return db;
  }
  
  public static List<NetworkRulesGroup> getUserNetworkRulesGroup( String userName ) {
    EntityWrapper<NetworkRulesGroup> db = NetworkGroupUtil.getEntityWrapper( );
    List<NetworkRulesGroup> networkGroups = Lists.newArrayList( Sets.newHashSet( db.query( new NetworkRulesGroup( userName ) ) ));
    db.commit( );
    return networkGroups;
  }

  public static NetworkRulesGroup getUserNetworkRulesGroup( String userName, String groupName ) throws EucalyptusCloudException {
    EntityWrapper<NetworkRulesGroup> db = NetworkGroupUtil.getEntityWrapper( );
    NetworkRulesGroup group = null;
    try {
      group = db.getUnique( new NetworkRulesGroup( userName, groupName ) );
      db.commit( );
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      throw e;
    }
    return group;
  }

  public static NetworkRulesGroup deleteUserNetworkRulesGroup( String userName, String groupName ) throws EucalyptusCloudException {
    EntityWrapper<NetworkRulesGroup> db = NetworkGroupUtil.getEntityWrapper( );
    NetworkRulesGroup group = null;
    try {
      group = db.getUnique( new NetworkRulesGroup( userName, groupName ) );
      db.delete( group );
      db.commit( );
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      throw e;
    }
    return group;
  }

  public static NetworkRulesGroup createUserNetworkRulesGroup( String userName, String groupName, String groupDescription ) throws EucalyptusCloudException {
    EntityWrapper<NetworkRulesGroup> db = NetworkGroupUtil.getEntityWrapper( );
    NetworkRulesGroup group = new NetworkRulesGroup( userName, groupName, groupDescription );
    try {
      db.add( group );
      db.commit( );
    } catch ( Exception e ) {
      db.rollback( );
      throw new EucalyptusCloudException( "Error adding network group: group named " +groupName+ " already exists", e );
    }
    return group;
  }
  
  protected static void makeDefault( String userId ) {
    try {
      createUserNetworkRulesGroup( userId, NetworkRulesGroup.NETWORK_DEFAULT_NAME, "default group" );
    } catch ( EucalyptusCloudException e1 ) {}
  }

  public static List<SecurityGroupItemType> getUserNetworks( String userId, List<String> groupNames ) throws EucalyptusCloudException {
    List<SecurityGroupItemType> groupInfoList = Lists.newArrayList();
    for ( NetworkRulesGroup group : NetworkGroupUtil.getUserNetworkRulesGroup( userId ) )
      if ( groupNames.isEmpty() || groupNames.contains( group.getDisplayName() ) ) {
        SecurityGroupItemType groupInfo = new SecurityGroupItemType();
        groupInfo.setGroupName( group.getDisplayName() );
        groupInfo.setGroupDescription( group.getDescription() );
        groupInfo.setOwnerId( userId );
        groupInfoList.add( groupInfo );
        for ( NetworkRule rule : group.getNetworkRules() ) {
          IpPermissionType ipPerm = new IpPermissionType( rule.getProtocol(), rule.getLowPort(), rule.getHighPort() );
          for ( IpRange ipRange : rule.getIpRanges() )
            ipPerm.getIpRanges().add( ipRange.getValue() );
          if ( !rule.getNetworkPeers().isEmpty() )
            for ( NetworkPeer peer : rule.getNetworkPeers() )
              ipPerm.getGroups().add( new UserIdGroupPairType( peer.getUserQueryKey(), peer.getGroupName() ) );
          groupInfo.getIpPermissions().add( ipPerm );
        }
      }
    return groupInfoList;
  }

  static List<NetworkRule> getNetworkRules( final IpPermissionType ipPerm ) {
    List<NetworkRule> ruleList = new ArrayList<NetworkRule>();
    if ( !ipPerm.getGroups().isEmpty() ) {
      if( ipPerm.getFromPort() == 0 && ipPerm.getToPort() == 0 ) {
        ipPerm.setToPort( 65535 );
      }
      //:: fixes handling of under-specified named-network rules sent by some clients :://
      if( ipPerm.getIpProtocol() == null ) {
        NetworkRule rule = new NetworkRule( "tcp", ipPerm.getFromPort(), ipPerm.getToPort() );
        rule.getNetworkPeers().addAll(  getNetworkPeers( ipPerm ) );
        ruleList.add( rule );
        NetworkRule rule1 = new NetworkRule( "udp", ipPerm.getFromPort(), ipPerm.getToPort() );
        rule1.getNetworkPeers().addAll(  getNetworkPeers( ipPerm ) );
        ruleList.add( rule1 );
        NetworkRule rule2 = new NetworkRule( "icmp", -1, -1 );
        rule2.getNetworkPeers().addAll( getNetworkPeers( ipPerm ) );
        ruleList.add( rule2 );
      } else {
        NetworkRule rule = new NetworkRule( ipPerm.getIpProtocol(), ipPerm.getFromPort(), ipPerm.getToPort() );
        rule.getNetworkPeers().addAll( getNetworkPeers( ipPerm ) );
        ruleList.add( rule );
      }
    } else if ( !ipPerm.getIpRanges().isEmpty() ) {
      List<IpRange> ipRanges = new ArrayList<IpRange>();
      for ( String range : ipPerm.getIpRanges() ) {
        String[] rangeParts = range.split( "/" );
        try {
          if( Integer.parseInt( rangeParts[1] ) > 32 || Integer.parseInt( rangeParts[1] ) < 0 ) continue;
          if( rangeParts.length != 2 ) continue;
          if( InetAddress.getByName( rangeParts[0] ) != null ) {
            ipRanges.add( new IpRange( range ) );
          }
        } catch ( NumberFormatException e ) {
        } catch ( UnknownHostException e ) {
        }
      }
      NetworkRule rule = new NetworkRule( ipPerm.getIpProtocol(), ipPerm.getFromPort(), ipPerm.getToPort(), ipRanges );
      ruleList.add( rule );
    }
    return ruleList;
  }

  private static List<NetworkPeer> getNetworkPeers( final IpPermissionType ipPerm ) {
    List<NetworkPeer> networkPeers = new ArrayList<NetworkPeer>();
    for ( UserIdGroupPairType peerInfo : ipPerm.getGroups() ) {
      networkPeers.add( new NetworkPeer( peerInfo.getSourceUserId(), peerInfo.getSourceGroupName() ) );
    }
    return networkPeers;
  }

}
