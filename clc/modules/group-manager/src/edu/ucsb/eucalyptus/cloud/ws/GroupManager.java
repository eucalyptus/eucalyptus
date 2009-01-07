/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.ws;

import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.cloud.entities.*;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.util.*;
import org.apache.axis2.AxisFault;
import org.apache.log4j.Logger;

import java.util.*;

public class GroupManager {

  private static Logger LOG = Logger.getLogger( GroupManager.class );

  public VmAllocationInfo verify( VmAllocationInfo vmAllocInfo ) throws EucalyptusCloudException {
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();

    UserInfo user = null;
    try { user = db.getUnique( new UserInfo( vmAllocInfo.getRequest().getUserId() ) ); }
    catch ( EucalyptusCloudException e ) { throw new EucalyptusCloudException( "Failed to find user: " + vmAllocInfo.getRequest().getUserId() ); }

    this.makeDefault( user.getUserName() );
    ArrayList<String> networkNames = new ArrayList<String>( vmAllocInfo.getRequest().getGroupSet() );
    Map<String, NetworkRulesGroup> networkRuleGroups = new HashMap<String, NetworkRulesGroup>();

    for ( NetworkRulesGroup networkName : user.getNetworkRulesGroup() ) networkRuleGroups.put( networkName.getName(), networkName );

    if ( networkNames.isEmpty() )
      networkNames.add( EucalyptusProperties.NETWORK_DEFAULT_NAME );

    ArrayList<String> userNetworks = new ArrayList<String>( networkRuleGroups.keySet() );

    if ( !userNetworks.containsAll( networkNames ) ) {
      networkNames.removeAll( userNetworks );
      db.rollback();
      throw new EucalyptusCloudException( "Failed to find " + networkNames );
    }

    for ( String networkName : networkNames )
      vmAllocInfo.getNetworks().add( networkRuleGroups.get( networkName ).getVmNetwork( vmAllocInfo.getRequest().getUserId() ) );

    if ( vmAllocInfo.getNetworks().size() < 1 )
      throw new EucalyptusCloudException( "Failed to find any specified networks? You sent: " + networkNames );

    return vmAllocInfo;
  }

  public CreateSecurityGroupResponseType CreateSecurityGroup( CreateSecurityGroupType request ) throws AxisFault {
    NetworkRulesGroup newGroup = new NetworkRulesGroup();
    newGroup.setName( request.getGroupName() );
    newGroup.setDescription( request.getGroupDescription() );

    this.makeDefault( request.getUserId() );
    CreateSecurityGroupResponseType reply = new CreateSecurityGroupResponseType();
    reply.setUserId( request.getUserId() );
    reply.setEffectiveUserId( request.getEffectiveUserId() );
    reply.setCorrelationId( request.getCorrelationId() );

    //:: find the user :://
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
    UserInfo user = null;
    boolean create = false;
    try {
      user = db.getUnique( new UserInfo( request.getUserId() ) );
      create = true;
      for ( NetworkRulesGroup g : user.getNetworkRulesGroup() )
        if ( g.getName().equals( newGroup.getName() ) ) {
          create = false;
          break;
        }
      if ( create )
        user.getNetworkRulesGroup().add( newGroup );
      reply.set_return( create );
      db.commit();
    }
    catch ( EucalyptusCloudException e ) {
      db.rollback();
      reply.set_return( false );
    }
    return reply;
  }

  public DeleteSecurityGroupResponseType DeleteSecurityGroup( DeleteSecurityGroupType request ) throws AxisFault {
    NetworkRulesGroup delGroup = new NetworkRulesGroup();
    delGroup.setName( request.getGroupName() );

    this.makeDefault( request.getUserId() );
    DeleteSecurityGroupResponseType reply = new DeleteSecurityGroupResponseType();
    reply.setUserId( request.getUserId() );
    reply.setEffectiveUserId( request.getEffectiveUserId() );
    reply.setCorrelationId( request.getCorrelationId() );
    reply.set_return( false );

    //:: find the user :://
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
    UserInfo user = null;
    try {
      user = db.getUnique( new UserInfo( request.getUserId() ) );
      NetworkRulesGroup target = null;
      for ( NetworkRulesGroup g : user.getNetworkRulesGroup() )
        if ( g.getName().equals( request.getGroupName() ) ) {
          target = g;
          break;
        }
      if ( target != null ) {
        user.getNetworkRulesGroup().remove( target );
        db.getEntityManager().remove( target );
        reply.set_return( true );
      }
      db.commit();
      return reply;
    }
    catch ( EucalyptusCloudException e ) {
      db.rollback();
      return reply;
    }
  }

  public DescribeSecurityGroupsResponseType DescribeSecurityGroups( DescribeSecurityGroupsType request ) throws AxisFault {
    DescribeSecurityGroupsResponseType reply = ( DescribeSecurityGroupsResponseType ) request.getReply();

    this.makeDefault( request.getUserId() );
    reply.getSecurityGroupInfo().addAll( getUserNetworks( request.getUserId(), request.getSecurityGroupSet() ) );
    return reply;
  }

  public static List<SecurityGroupItemType> getUserNetworks( String userId, List<String> groupNames ) throws EucalyptusCloudException {
    List<SecurityGroupItemType> groupInfoList = Lists.newArrayList();
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
    try {
      UserInfo user = db.getUnique( new UserInfo( userId ) );
      for ( NetworkRulesGroup group : user.getNetworkRulesGroup() )
        if ( groupNames.isEmpty() || groupNames.contains( group.getName() ) ) {
          SecurityGroupItemType groupInfo = new SecurityGroupItemType();
          groupInfo.setGroupName( group.getName() );
          groupInfo.setGroupDescription( group.getDescription() );
          groupInfo.setOwnerId( user.getUserName() );
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
      db.commit();
    }
    catch ( EucalyptusCloudException e ) {
      db.rollback();
      throw e;
    }
    return groupInfoList;
  }

  public RevokeSecurityGroupIngressResponseType RevokeSecurityGroupIngress( RevokeSecurityGroupIngressType request ) throws AxisFault {
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();

    RevokeSecurityGroupIngressResponseType reply = ( RevokeSecurityGroupIngressResponseType ) request.getReply();

    UserInfo user = db.getUnique( new UserInfo( request.getUserId() ) );
    boolean foundAll = false;
    int groupIndex = -1;
    NetworkRulesGroup ruleGroup = null;
    if ( ( groupIndex = user.getNetworkRulesGroup().indexOf( new NetworkRulesGroup( request.getGroupName() ) ) ) >= 0 ) {
      ruleGroup = user.getNetworkRulesGroup().get( groupIndex );
      for ( IpPermissionType ipPerm : request.getIpPermissions() ) {
        List<NetworkRule> ruleList = getNetworkRules( ipPerm );
        if ( ruleGroup.getNetworkRules().containsAll( getNetworkRules( ipPerm ) ) ) {
          for ( NetworkRule delRule : ruleList ) {
            int index = ruleGroup.getNetworkRules().indexOf( delRule );
            ruleGroup.getNetworkRules().remove( index );
            db.getEntityManager().persist( ruleGroup );
          }
          foundAll = true;
        } else
          foundAll = false;
      }
    }
    if ( foundAll ) {
      db.commit();
      Network changedNetwork = ruleGroup.getVmNetwork( user.getUserName() );
      Messaging.dispatch( EucalyptusProperties.CLUSTERSINK_REF, changedNetwork );
    } else db.rollback();
    reply.set_return( foundAll );
    return reply;
  }

  public AuthorizeSecurityGroupIngressResponseType AuthorizeSecurityGroupIngress( AuthorizeSecurityGroupIngressType request ) throws AxisFault {
    AuthorizeSecurityGroupIngressResponseType reply = ( AuthorizeSecurityGroupIngressResponseType ) request.getReply();
    reply.set_return( false );

    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
    UserInfo user = db.getUnique( new UserInfo( request.getUserId() ) );

    LOG.fatal( request );

    int groupIndex = -1;
    if ( ( groupIndex = user.getNetworkRulesGroup().indexOf( new NetworkRulesGroup( request.getGroupName() ) ) ) >= 0 ) {
      List<NetworkRule> ruleList = new ArrayList<NetworkRule>();
      NetworkRulesGroup userRulesList = user.getNetworkRulesGroup().get( groupIndex );
      for ( IpPermissionType ipPerm : request.getIpPermissions() )
        ruleList.addAll( getNetworkRules( ipPerm ) );
      for ( NetworkRule newRule : ruleList )
        if ( userRulesList.getNetworkRules().contains( newRule ) || !newRule.isValid() ) {
          reply.set_return( false );
          db.rollback();
          return reply;
        }
      userRulesList.getNetworkRules().addAll( ruleList );
      Network changedNetwork = userRulesList.getVmNetwork( user.getUserName() );
      Messaging.dispatch( EucalyptusProperties.CLUSTERSINK_REF, changedNetwork );
      reply.set_return( true );
      db.commit();
      return reply;
    }
    db.rollback();
    return reply;
  }

  private static List<NetworkRule> getNetworkRules( final IpPermissionType ipPerm ) {
    List<NetworkRule> ruleList = new ArrayList<NetworkRule>();
    if ( !ipPerm.getGroups().isEmpty() ) {
      List<NetworkPeer> networkPeers = new ArrayList<NetworkPeer>();
      for ( UserIdGroupPairType peerInfo : ipPerm.getGroups() ) {
        networkPeers.add( new NetworkPeer( peerInfo.getUserId(), peerInfo.getGroupName() ) );
      }
      NetworkRule rule = new NetworkRule( ipPerm.getIpProtocol(), ipPerm.getFromPort(), ipPerm.getToPort() );
      rule.getNetworkPeers().addAll( networkPeers );
      ruleList.add( rule );
    } else if ( !ipPerm.getIpRanges().isEmpty() ) {
      List<IpRange> ipRanges = new ArrayList<IpRange>();
      for ( String range : ipPerm.getIpRanges() ) {
        ipRanges.add( new IpRange( range ) );
      }
      NetworkRule rule = new NetworkRule( ipPerm.getIpProtocol(), ipPerm.getFromPort(), ipPerm.getToPort(), ipRanges );
      ruleList.add( rule );
    }
    return ruleList;
  }

  private static void makeDefault( String userId ) {
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
    try {
      UserInfo user = db.getUnique( new UserInfo( userId ) );
      boolean found = false;
      for ( NetworkRulesGroup group : user.getNetworkRulesGroup() )
        found |= EucalyptusProperties.NETWORK_DEFAULT_NAME.equals( group.getName() );
      if ( !found )
        user.getNetworkRulesGroup().add( NetworkRulesGroup.getDefaultGroup() );
    } catch ( EucalyptusCloudException e ) {
    } finally {
      db.commit();
    }

  }

}
