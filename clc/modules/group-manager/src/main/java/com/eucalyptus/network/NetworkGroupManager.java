package com.eucalyptus.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.context.ServiceContext;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.entities.NetworkRule;
import com.eucalyptus.entities.NetworkRulesGroup;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.ws.util.Messaging;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.Network;
import edu.ucsb.eucalyptus.cloud.VmAllocationInfo;
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

public class NetworkGroupManager {
  private static Logger LOG = Logger.getLogger( NetworkGroupManager.class );
  
  public VmAllocationInfo verify( VmAllocationInfo vmAllocInfo ) throws EucalyptusCloudException {
    NetworkGroupUtil.makeDefault( vmAllocInfo.getRequest( ).getUserId( ) );//ensure the default group exists to cover some old broken installs
    ArrayList<String> networkNames = new ArrayList<String>( vmAllocInfo.getRequest( ).getGroupSet( ) );
    if ( networkNames.size( ) < 1 ) {
      networkNames.add( "default" );
    }
    Map<String, NetworkRulesGroup> networkRuleGroups = new HashMap<String, NetworkRulesGroup>( );
    for ( String groupName : networkNames ) {
      NetworkRulesGroup group = NetworkGroupUtil.getUserNetworkRulesGroup( vmAllocInfo.getRequest( ).getUserId( ), groupName );
      networkRuleGroups.put( groupName, group );
      vmAllocInfo.getNetworks( ).add( group.getVmNetwork( ) );
    }
    ArrayList<String> userNetworks = new ArrayList<String>( networkRuleGroups.keySet( ) );
    if ( !userNetworks.containsAll( networkNames ) ) {
      networkNames.removeAll( userNetworks );
      throw new EucalyptusCloudException( "Failed to find " + networkNames );
    }
    return vmAllocInfo;
  }
  
  public CreateSecurityGroupResponseType create( CreateSecurityGroupType request ) throws EucalyptusCloudException {
    NetworkGroupUtil.makeDefault( request.getUserId( ) );//ensure the default group exists to cover some old broken installs
    CreateSecurityGroupResponseType reply = ( CreateSecurityGroupResponseType ) request.getReply( );
    NetworkRulesGroup newGroup = NetworkGroupUtil.createUserNetworkRulesGroup( request.getUserId( ), request.getGroupName( ), request.getGroupDescription( ) );
    reply.set_return( true );
    return reply;
  }
  
  public DeleteSecurityGroupResponseType delete( DeleteSecurityGroupType request ) throws EucalyptusCloudException {
    NetworkGroupUtil.makeDefault( request.getUserId( ) );//ensure the default group exists to cover some old broken installs
    DeleteSecurityGroupResponseType reply = ( DeleteSecurityGroupResponseType ) request.getReply( );
    if ( request.isAdministrator( ) && request.getGroupName( ).indexOf( "::" ) != -1 ) {
      NetworkGroupUtil.deleteUserNetworkRulesGroup( request.getGroupName( ).replaceAll( "::.*", "" ), request.getGroupName( ).replaceAll( "\\w*::", "" ) );
    } else {
      NetworkGroupUtil.deleteUserNetworkRulesGroup( request.getUserId( ), request.getGroupName( ) );
    }
    reply.set_return( true );
    return reply;
  }
  
  public DescribeSecurityGroupsResponseType describe( DescribeSecurityGroupsType request ) throws EucalyptusCloudException {
    NetworkGroupUtil.makeDefault( request.getUserId( ) );//ensure the default group exists to cover some old broken installs
    final List<String> groupNames = request.getSecurityGroupSet( );
    DescribeSecurityGroupsResponseType reply = ( DescribeSecurityGroupsResponseType ) request.getReply( );
    final List<SecurityGroupItemType> replyList = reply.getSecurityGroupInfo( );
    if ( request.isAdministrator( ) ) {
      try {
        for ( SecurityGroupItemType group : Iterables.filter( NetworkGroupUtil.getUserNetworksAdmin( request.getUserId( ), request.getSecurityGroupSet( ) ),
                                                              new Predicate<SecurityGroupItemType>( ) {
                                                                @Override
                                                                public boolean apply( SecurityGroupItemType arg0 ) {
                                                                  return groupNames.isEmpty( ) || groupNames.contains( arg0.getGroupName( ) );
                                                                }
                                                              } ) ) {
          replyList.add( group );
        }
      } catch ( Exception e ) {
        LOG.debug( e, e );
      }
    } else {
      try {
        for ( SecurityGroupItemType group : Iterables.filter( NetworkGroupUtil.getUserNetworks( request.getUserId( ), request.getSecurityGroupSet( ) ),
                                                              new Predicate<SecurityGroupItemType>( ) {
                                                                @Override
                                                                public boolean apply( SecurityGroupItemType arg0 ) {
                                                                  return groupNames.isEmpty( ) || groupNames.contains( arg0.getGroupName( ) );
                                                                }
                                                              } ) ) {
          replyList.add( group );
        }
      } catch ( Exception e ) {
        LOG.debug( e, e );
      }
    }
    return reply;
  }
  
  public RevokeSecurityGroupIngressResponseType revoke( RevokeSecurityGroupIngressType request ) throws EucalyptusCloudException {
    NetworkGroupUtil.makeDefault( request.getUserId( ) );//ensure the default group exists to cover some old broken installs
    RevokeSecurityGroupIngressResponseType reply = ( RevokeSecurityGroupIngressResponseType ) request.getReply( );
    NetworkRulesGroup ruleGroup = NetworkGroupUtil.getUserNetworkRulesGroup( request.getUserId( ), request.getGroupName( ) );
    final List<NetworkRule> ruleList = Lists.newArrayList( );
    for ( IpPermissionType ipPerm : request.getIpPermissions( ) ) {
      ruleList.addAll( NetworkGroupUtil.getNetworkRules( ipPerm ) );
    }
    List<NetworkRule> filtered = Lists.newArrayList( Iterables.filter( ruleGroup.getNetworkRules( ), new Predicate<NetworkRule>( ) {
      @Override
      public boolean apply( NetworkRule rule ) {
        for ( NetworkRule r : ruleList ) {
          if ( r.equals( rule ) && r.getNetworkPeers( ).equals( rule.getNetworkPeers( ) ) && r.getIpRanges( ).equals( rule.getIpRanges( ) ) ) {
            return true;
          }
        }
        return false;
      }
    } ) );
    if ( filtered.size( ) == ruleList.size( ) ) {
      for ( NetworkRule r : filtered ) {
        ruleGroup.getNetworkRules( ).remove( r );
      }
      NetworkGroupUtil.getEntityWrapper( ).mergeAndCommit( ruleGroup );
    } else if ( request.getIpPermissions( ).size( ) == 1 && request.getIpPermissions( ).get( 0 ).getIpProtocol( ) == null ) {
      //LAME: this is for the query-based clients which send incomplete named-network requests.
      for ( NetworkRule rule : ruleList ) {
        if ( ruleGroup.getNetworkRules( ).remove( rule ) ) {
          reply.set_return( true );
        }
      }
      if ( reply.get_return( ) ) {
        NetworkGroupUtil.getEntityWrapper( ).mergeAndCommit( ruleGroup );
      }
    } else {
      reply.set_return( false );
      return reply;
    }
    Network changedNetwork = ruleGroup.getVmNetwork( );
    ServiceContext.dispatch( "ClusterSink", changedNetwork );
    reply.set_return( true );
    return reply;
  }
  
  public AuthorizeSecurityGroupIngressResponseType authorize( AuthorizeSecurityGroupIngressType request ) throws Exception {
    NetworkGroupUtil.makeDefault( request.getUserId( ) );//ensure the default group exists to cover some old broken installs
    AuthorizeSecurityGroupIngressResponseType reply = ( AuthorizeSecurityGroupIngressResponseType ) request.getReply( );
    EntityWrapper<NetworkRulesGroup> db = NetworkGroupUtil.getEntityWrapper( );
    NetworkRulesGroup ruleGroup = NetworkGroupUtil.getUserNetworkRulesGroup( request.getUserId( ), request.getGroupName( ) );
    final List<NetworkRule> ruleList = Lists.newArrayList( );
    for ( IpPermissionType ipPerm : request.getIpPermissions( ) ) {
      ruleList.addAll( NetworkGroupUtil.getNetworkRules( ipPerm ) );
    }
    if ( Iterables.any( ruleGroup.getNetworkRules( ), new Predicate<NetworkRule>( ) {
      @Override
      public boolean apply( NetworkRule rule ) {
        for ( NetworkRule r : ruleList ) {
          if ( r.equals( rule ) && r.getNetworkPeers( ).equals( rule.getNetworkPeers( ) ) && r.getIpRanges( ).equals( rule.getIpRanges( ) ) ) {
            return true || !r.isValid( );
          }
        }
        return false;
      }
    } ) ) {
      reply.set_return( false );
      db.rollback( );
      return reply;
    }
    ruleGroup.getNetworkRules( ).addAll( ruleList );
    db.merge( ruleGroup );
    db.commit( );
    Network changedNetwork = ruleGroup.getVmNetwork( );
    ServiceContext.dispatch( "ClusterSink", changedNetwork );
    reply.set_return( true );
    
    return reply;
  }
}
