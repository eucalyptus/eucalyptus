/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
 *
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */

package edu.ucsb.eucalyptus.cloud.ws;

import com.google.common.collect.Lists;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import edu.ucsb.eucalyptus.cloud.Network;
import edu.ucsb.eucalyptus.cloud.VmAllocationInfo;
import edu.ucsb.eucalyptus.cloud.entities.IpRange;
import edu.ucsb.eucalyptus.cloud.entities.NetworkPeer;
import edu.ucsb.eucalyptus.cloud.entities.NetworkRule;
import edu.ucsb.eucalyptus.cloud.entities.NetworkRulesGroup;
import edu.ucsb.eucalyptus.cloud.entities.UserInfo;
import edu.ucsb.eucalyptus.msgs.AuthorizeSecurityGroupIngressResponseType;
import edu.ucsb.eucalyptus.msgs.AuthorizeSecurityGroupIngressType;
import edu.ucsb.eucalyptus.msgs.CreateSecurityGroupResponseType;
import edu.ucsb.eucalyptus.msgs.CreateSecurityGroupType;
import edu.ucsb.eucalyptus.msgs.DeleteSecurityGroupResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteSecurityGroupType;
import edu.ucsb.eucalyptus.msgs.DescribeSecurityGroupsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeSecurityGroupsType;
import edu.ucsb.eucalyptus.msgs.IpPermissionType;
import edu.ucsb.eucalyptus.msgs.RevokeSecurityGroupIngressResponseType;
import edu.ucsb.eucalyptus.msgs.RevokeSecurityGroupIngressType;
import edu.ucsb.eucalyptus.msgs.SecurityGroupItemType;
import edu.ucsb.eucalyptus.msgs.UserIdGroupPairType;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import com.eucalyptus.ws.util.Messaging;

import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  public CreateSecurityGroupResponseType CreateSecurityGroup( CreateSecurityGroupType request ) throws Exception {
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

  public DeleteSecurityGroupResponseType DeleteSecurityGroup( DeleteSecurityGroupType request ) throws Exception {
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

  public DescribeSecurityGroupsResponseType DescribeSecurityGroups( DescribeSecurityGroupsType request ) throws Exception {
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

  public RevokeSecurityGroupIngressResponseType RevokeSecurityGroupIngress( RevokeSecurityGroupIngressType request ) throws Exception {
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
      Network changedNetwork = ruleGroup.getVmNetwork( user.getUserName() );
      Messaging.dispatch( Component.clusters.getUri( ).toASCIIString( ), changedNetwork );
      db.commit();
    } else db.rollback();
    reply.set_return( foundAll );
    return reply;
  }

  public AuthorizeSecurityGroupIngressResponseType AuthorizeSecurityGroupIngress( AuthorizeSecurityGroupIngressType request ) throws Exception{
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
      Messaging.dispatch( Component.clusters.getUri( ).toASCIIString( ), changedNetwork );
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
