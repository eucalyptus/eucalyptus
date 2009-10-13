package com.eucalyptus.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.CredentialProvider;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.entities.NetworkRule;
import com.eucalyptus.entities.NetworkRulesGroup;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.ws.util.Messaging;
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

public class NetworkGroupManager {
  private static Logger LOG = Logger.getLogger( NetworkGroupManager.class );
  public VmAllocationInfo verify( VmAllocationInfo vmAllocInfo ) throws EucalyptusCloudException {
    NetworkGroupUtil.makeDefault( vmAllocInfo.getRequest( ).getUserId( ) );//ensure the default group exists to cover some old broken installs
    ArrayList<String> networkNames = new ArrayList<String>( vmAllocInfo.getRequest().getGroupSet() );
    if ( networkNames.size() < 1 ){
      networkNames.add("default");
    }
    Map<String, NetworkRulesGroup> networkRuleGroups = new HashMap<String, NetworkRulesGroup>();
    for( String groupName : networkNames ) {
      NetworkRulesGroup group = NetworkGroupUtil.getUserNetworkRulesGroup( vmAllocInfo.getRequest( ).getUserId( ), groupName );
      networkRuleGroups.put( groupName, group );
      vmAllocInfo.getNetworks( ).add( group.getVmNetwork( ) );
    }
    ArrayList<String> userNetworks = new ArrayList<String>( networkRuleGroups.keySet() );
    if ( !userNetworks.containsAll( networkNames ) ) {
      networkNames.removeAll( userNetworks );
      throw new EucalyptusCloudException( "Failed to find " + networkNames );
    }
    return vmAllocInfo;
  }
  public CreateSecurityGroupResponseType create( CreateSecurityGroupType request ) throws EucalyptusCloudException {
    NetworkGroupUtil.makeDefault( request.getUserId( ) );//ensure the default group exists to cover some old broken installs
    CreateSecurityGroupResponseType reply = (CreateSecurityGroupResponseType)request.getReply( );
    try {
      NetworkRulesGroup newGroup = NetworkGroupUtil.createUserNetworkRulesGroup(request.getUserId( ),request.getGroupName( ), request.getGroupDescription( ));
      reply.set_return( true );
    } catch ( Exception e ) {
      LOG.debug(e,e);
      reply.set_return( false );
    }
    return reply;
  }
  public DeleteSecurityGroupResponseType delete( DeleteSecurityGroupType request ) throws EucalyptusCloudException {
    NetworkGroupUtil.makeDefault( request.getUserId( ) );//ensure the default group exists to cover some old broken installs
    DeleteSecurityGroupResponseType reply = (DeleteSecurityGroupResponseType) request.getReply( ); 
    if( request.isAdministrator( ) && request.getGroupName( ).indexOf( "-" ) != -1 ) {
      NetworkGroupUtil.deleteUserNetworkRulesGroup( request.getGroupName( ).split("-")[0], request.getGroupName( ).replaceFirst("\\w*-","") );      
    } else {
      try {
        NetworkGroupUtil.deleteUserNetworkRulesGroup( request.getUserId( ), request.getGroupName( ) );
        reply.set_return( true );
      } catch ( Exception e ) {
        LOG.debug(e,e);
        reply.set_return( true );
      }
    }
    return reply;
  }
  
  public DescribeSecurityGroupsResponseType describe( DescribeSecurityGroupsType request ) throws EucalyptusCloudException {
    NetworkGroupUtil.makeDefault( request.getUserId( ) );//ensure the default group exists to cover some old broken installs
    DescribeSecurityGroupsResponseType reply = ( DescribeSecurityGroupsResponseType ) request.getReply();
    if( request.isAdministrator( ) ) {
      try {
        reply.getSecurityGroupInfo( ).addAll( NetworkGroupUtil.getUserNetworksAdmin( request.getUserId( ), request.getSecurityGroupSet( ) ) );
      } catch ( Exception e ) {
        LOG.debug( e, e );
      }
    } else {
      try {
        reply.getSecurityGroupInfo( ).addAll( NetworkGroupUtil.getUserNetworks( request.getUserId( ), request.getSecurityGroupSet( ) ) );
      } catch ( Exception e ) {
        LOG.debug( e, e );
      }
    }
    return reply;
  }
  public RevokeSecurityGroupIngressResponseType revoke( RevokeSecurityGroupIngressType request ) throws EucalyptusCloudException {
    RevokeSecurityGroupIngressResponseType reply = ( RevokeSecurityGroupIngressResponseType ) request.getReply();
    NetworkRulesGroup ruleGroup = NetworkGroupUtil.getUserNetworkRulesGroup( request.getUserId( ), request.getGroupName( ) );
    List<NetworkRule> ruleList = Lists.newArrayList( );
    for ( IpPermissionType ipPerm : request.getIpPermissions() ) {
      ruleList.addAll( NetworkGroupUtil.getNetworkRules( ipPerm ) );
    }
    if ( ruleGroup.getNetworkRules().removeAll( ruleList ) ) {
      NetworkGroupUtil.getEntityWrapper().mergeAndCommit( ruleGroup );
    } else if ( request.getIpPermissions( ).size( ) == 1 && request.getIpPermissions( ).get( 0 ).getIpProtocol( ) == null ) {
      //LAME: this is for the query-based clients which send incomplete named-network requests.
      for( NetworkRule rule : ruleList ) {
        if ( ruleGroup.getNetworkRules().remove( rule ) ) {
          reply.set_return( true );
        }
      }
      if( reply.get_return( ) ) {
        NetworkGroupUtil.getEntityWrapper().mergeAndCommit( ruleGroup );
      }
    } else {
      reply.set_return( false );
      return reply;
    }
    Network changedNetwork = ruleGroup.getVmNetwork( );
    Messaging.dispatch( Component.cluster.getUri( ).toASCIIString( ), changedNetwork );
    reply.set_return( true );
    return reply;
  }

  public AuthorizeSecurityGroupIngressResponseType authorize( AuthorizeSecurityGroupIngressType request ) throws Exception{
    AuthorizeSecurityGroupIngressResponseType reply = ( AuthorizeSecurityGroupIngressResponseType ) request.getReply();
    EntityWrapper<NetworkRulesGroup> db = NetworkGroupUtil.getEntityWrapper( );
    NetworkRulesGroup ruleGroup = NetworkGroupUtil.getUserNetworkRulesGroup( request.getUserId( ), request.getGroupName( ) );
    List<NetworkRule> ruleList = Lists.newArrayList( );
    for ( IpPermissionType ipPerm : request.getIpPermissions() ) {
      ruleList.addAll( NetworkGroupUtil.getNetworkRules( ipPerm ) );
    }
    for ( NetworkRule newRule : ruleList ) {
      if ( ruleGroup.getNetworkRules().contains( newRule ) || !newRule.isValid() ) {
        reply.set_return( false );
        db.rollback( );
        return reply;
      }
    }
    ruleGroup.getNetworkRules().addAll( ruleList );
    db.merge( ruleGroup );
    db.commit( );
    Network changedNetwork = ruleGroup.getVmNetwork( );
    Messaging.dispatch( Component.cluster.getUri( ).toASCIIString( ), changedNetwork );
    reply.set_return( true );

    return reply;
  }
}
