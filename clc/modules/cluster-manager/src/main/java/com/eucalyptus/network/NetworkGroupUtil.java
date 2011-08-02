package com.eucalyptus.network;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.IpPermissionType;
import edu.ucsb.eucalyptus.msgs.SecurityGroupItemType;
import edu.ucsb.eucalyptus.msgs.UserIdGroupPairType;

@Deprecated
public class NetworkGroupUtil {
  
  @Deprecated
  public static List<NetworkRulesGroup> getUserNetworkRulesGroup( UserFullName userFullName ) {
    EntityWrapper<NetworkRulesGroup> db = EntityWrapper.get( NetworkRulesGroup.class );
    List<NetworkRulesGroup> networkGroups = Lists.newArrayList( );
    try {
      networkGroups = db.query( new NetworkRulesGroup( userFullName ) );
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
    }
    return networkGroups;
  }
  
  @Deprecated
  public static NetworkRulesGroup getUserNetworkRulesGroup( UserFullName userFullName, String groupName ) throws EucalyptusCloudException {
    EntityWrapper<NetworkRulesGroup> db = EntityWrapper.get( NetworkRulesGroup.class );
    NetworkRulesGroup group = null;
    try {
      group = db.getUnique( new NetworkRulesGroup( userFullName, groupName ) );
      db.commit( );
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      throw e;
    }
    return group;
  }
  
  @Deprecated
  public static NetworkRulesGroup deleteUserNetworkRulesGroup( UserFullName userFullName, String groupName ) throws EucalyptusCloudException {
    EntityWrapper<NetworkRulesGroup> db = EntityWrapper.get( NetworkRulesGroup.class );
    NetworkRulesGroup group = null;
    try {
      group = db.getUnique( new NetworkRulesGroup( userFullName, groupName ) );
      db.delete( group );
      db.commit( );
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      throw e;
    } catch ( Throwable e ) {
      db.rollback( );
      throw new EucalyptusCloudException( e );
    }
    return group;
  }
  
  @Deprecated
  public static NetworkRulesGroup createUserNetworkRulesGroup( UserFullName userFullName, String groupName, String groupDescription ) throws EucalyptusCloudException {
    return NetworkGroups.create( userFullName, groupName, groupDescription );
  }
  
  @Deprecated
  public static List<SecurityGroupItemType> getUserNetworksAdmin( UserFullName userFullName, List<String> groupNames ) throws EucalyptusCloudException {
    List<SecurityGroupItemType> groupInfoList = Lists.newArrayList( );
    if ( groupNames.isEmpty( ) ) {
      try {
        for ( User u : Accounts.listAllUsers( ) ) {
          groupInfoList.addAll( NetworkGroupUtil.getUserNetworks( UserFullName.getInstance( u.getUserId( ) ), groupNames ) );
        }
      } catch ( AuthException ex ) {
        throw new EucalyptusCloudException( "Fail to get all users", ex );
      }
    } else {
      for ( String groupName : groupNames ) {
        groupInfoList.addAll( NetworkGroupUtil.getUserNetworks( userFullName, Lists.newArrayList( groupName ) ) );
      }
    }
    return groupInfoList;
  }
  
  @Deprecated
  public static List<SecurityGroupItemType> getUserNetworksAdmin( String adminGroupName ) throws EucalyptusCloudException {
    return getUserNetworks( UserFullName.getInstance( adminGroupName.replaceAll( "::\\w*", "" ) ),
                            Lists.newArrayList( adminGroupName.replaceFirst( "\\w*::", "" ) ) );
  }
  
  @Deprecated
  public static List<SecurityGroupItemType> getUserNetworks( UserFullName userFullName, List<String> groupNames ) throws EucalyptusCloudException {
    List<SecurityGroupItemType> groupInfoList = Lists.newArrayList( );
    List<NetworkRulesGroup> userGroups = Lists.newArrayList( );
    if ( groupNames.isEmpty( ) ) {
      userGroups.addAll( NetworkGroupUtil.getUserNetworkRulesGroup( userFullName ) );
    } else {
      for ( String groupName : groupNames ) {
        try {
          userGroups.add( NetworkGroupUtil.getUserNetworkRulesGroup( userFullName, groupName ) );
        } catch ( Exception e ) {}
      }
    }
    for ( NetworkRulesGroup group : NetworkGroupUtil.getUserNetworkRulesGroup( userFullName ) ) {
      groupInfoList.add( getAsSecurityGroupItemType( userFullName, group ) );
    }
    return groupInfoList;
  }
  
  @Deprecated
  public static SecurityGroupItemType getAsSecurityGroupItemType( UserFullName userFullName, NetworkRulesGroup group ) {
    SecurityGroupItemType groupInfo = new SecurityGroupItemType( );
    groupInfo.setGroupName( group.getDisplayName( ) );
    groupInfo.setGroupDescription( group.getDescription( ) );
    groupInfo.setAccountId( userFullName.getAccountNumber( ) );
    for ( NetworkRule rule : group.getNetworkRules( ) ) {
      IpPermissionType ipPerm = new IpPermissionType( rule.getProtocol( ), rule.getLowPort( ), rule.getHighPort( ) );
      for ( IpRange ipRange : rule.getIpRanges( ) )
        ipPerm.getIpRanges( ).add( ipRange.getValue( ) );
      if ( !rule.getNetworkPeers( ).isEmpty( ) )
        for ( NetworkPeer peer : rule.getNetworkPeers( ) )
          ipPerm.getGroups( ).add( new UserIdGroupPairType( peer.getUserQueryKey( ), peer.getGroupName( ) ) );
      groupInfo.getIpPermissions( ).add( ipPerm );
    }
    return groupInfo;
  }
  
  @Deprecated
  static List<NetworkRule> getNetworkRules( final IpPermissionType ipPerm ) throws IllegalArgumentException {
    List<NetworkRule> ruleList = new ArrayList<NetworkRule>( );
    if ( !ipPerm.getGroups( ).isEmpty( ) ) {
      if ( ipPerm.getFromPort( ) == 0 && ipPerm.getToPort( ) == 0 ) {
        ipPerm.setToPort( 65535 );
      }
      //:: fixes handling of under-specified named-network rules sent by some clients :://
      if ( ipPerm.getIpProtocol( ) == null ) {
        NetworkRule rule = new NetworkRule( "tcp", ipPerm.getFromPort( ), ipPerm.getToPort( ) );
        rule.getNetworkPeers( ).addAll( getNetworkPeers( ipPerm ) );
        ruleList.add( rule );
        NetworkRule rule1 = new NetworkRule( "udp", ipPerm.getFromPort( ), ipPerm.getToPort( ) );
        rule1.getNetworkPeers( ).addAll( getNetworkPeers( ipPerm ) );
        ruleList.add( rule1 );
        NetworkRule rule2 = new NetworkRule( "icmp", -1, -1 );
        rule2.getNetworkPeers( ).addAll( getNetworkPeers( ipPerm ) );
        ruleList.add( rule2 );
      } else {
        NetworkRule rule = new NetworkRule( ipPerm.getIpProtocol( ), ipPerm.getFromPort( ), ipPerm.getToPort( ) );
        rule.getNetworkPeers( ).addAll( getNetworkPeers( ipPerm ) );
        ruleList.add( rule );
      }
    } else if ( !ipPerm.getIpRanges( ).isEmpty( ) ) {
      List<IpRange> ipRanges = new ArrayList<IpRange>( );
      for ( String range : ipPerm.getIpRanges( ) ) {
        String[] rangeParts = range.split( "/" );
        try {
          if ( Integer.parseInt( rangeParts[1] ) > 32 || Integer.parseInt( rangeParts[1] ) < 0 ) continue;
          if ( rangeParts.length != 2 ) continue;
          if ( InetAddress.getByName( rangeParts[0] ) != null ) {
            ipRanges.add( new IpRange( range ) );
          }
        } catch ( NumberFormatException e ) {} catch ( UnknownHostException e ) {}
      }
      NetworkRule rule = new NetworkRule( ipPerm.getIpProtocol( ), ipPerm.getFromPort( ), ipPerm.getToPort( ), ipRanges );
      ruleList.add( rule );
    } else {
      throw new IllegalArgumentException( "Invalid Ip Permissions:  must specify either a source cidr or user" );
    }
    return ruleList;
  }
  
  @Deprecated
  private static List<NetworkPeer> getNetworkPeers( final IpPermissionType ipPerm ) {
    List<NetworkPeer> networkPeers = new ArrayList<NetworkPeer>( );
    for ( UserIdGroupPairType peerInfo : ipPerm.getGroups( ) ) {
      networkPeers.add( new NetworkPeer( peerInfo.getSourceUserId( ), peerInfo.getSourceGroupName( ) ) );
    }
    return networkPeers;
  }
  
}
