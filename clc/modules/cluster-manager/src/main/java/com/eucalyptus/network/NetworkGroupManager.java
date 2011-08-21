package com.eucalyptus.network;

import java.util.List;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloud.util.MetadataException;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
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
  
  public CreateSecurityGroupResponseType create( final CreateSecurityGroupType request ) throws EucalyptusCloudException, MetadataException {
    final Context ctx = Contexts.lookup( );
    
    /**
     * GRZE:WARN: do this /first/, ensure the default group exists to cover some old broken installs
     **/
    final String action = PolicySpec.requestToAction( request );
    if ( !ctx.hasAdministrativePrivileges( ) ) {
      if ( !Permissions.isAuthorized( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_SECURITYGROUP, "", ctx.getAccount( ), action, ctx.getUser( ) ) ) {
        throw new EucalyptusCloudException( "Not authorized to create network group for " + ctx.getUser( ) );
      }
      if ( !Permissions.canAllocate( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_SECURITYGROUP, "", action, ctx.getUser( ), 1L ) ) {
        throw new EucalyptusCloudException( "Quota exceeded to create network group for " + ctx.getUser( ) );
      }
    }
    final CreateSecurityGroupResponseType reply = ( CreateSecurityGroupResponseType ) request.getReply( );
    try {
      NetworkGroups.create( ctx.getUserFullName( ), request.getGroupName( ), request.getGroupDescription( ) );
      return reply;
    } catch ( Exception ex ) {
      throw new EucalyptusCloudException( "CreatSecurityGroup failed because: " + ex.getMessage( ), ex );
    }
  }
  
  public DeleteSecurityGroupResponseType delete( final DeleteSecurityGroupType request ) throws EucalyptusCloudException, MetadataException {
    final Context ctx = Contexts.lookup( );
    final DeleteSecurityGroupResponseType reply = ( DeleteSecurityGroupResponseType ) request.getReply( );
    if ( Contexts.lookup( ).hasAdministrativePrivileges( ) && ( request.getGroupName( ).indexOf( "::" ) != -1 ) ) {
      final String[] nameParts = request.getGroupName( ).split( "::" );
      if ( nameParts.length != 2 ) {
        throw new EucalyptusCloudException( "Request to delete group named: " + request.getGroupName( ) + " is malformed." );
      } else {
        final String accountId = nameParts[0];
        final String groupName = nameParts[1];
        try {
          final Account account = Accounts.lookupAccountById( accountId );
          for ( final User user : account.getUsers( ) ) {
            final UserFullName userFullName = UserFullName.getInstance( user );
            try {
              NetworkGroupUtil.getUserNetworkRulesGroup( userFullName, groupName );
              NetworkGroupUtil.deleteUserNetworkRulesGroup( userFullName, groupName );
            } catch ( final EucalyptusCloudException ex ) {
              //need to iterate over all users in the account and check each of their security groups
            }
          }
        } catch ( final AuthException ex ) {
          LOG.error( ex.getMessage( ) );
          throw new EucalyptusCloudException( "Deleting security failed because of: " + ex.getMessage( ) + " for request " + request.toSimpleString( ) );
        }
      }
    } else {
      if ( !Permissions.isAuthorized( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_SECURITYGROUP, request.getGroupName( ), ctx.getAccount( ),
                                      PolicySpec.requestToAction( request ), ctx.getUser( ) ) ) {
        throw new EucalyptusCloudException( "Not authorized to delete network group " + request.getGroupName( ) + " for " + ctx.getUser( ) );
      }
      NetworkGroupUtil.deleteUserNetworkRulesGroup( ctx.getUserFullName( ), request.getGroupName( ) );
    }
    reply.set_return( true );
    return reply;
  }
  
  enum UserAuthGroupFilter implements Predicate<NetworkGroup> {
    INSTANCE;
    @Override
    public boolean apply( NetworkGroup arg0 ) {
      final Context ctx = Contexts.lookup( );
      return Permissions.isAuthorized( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_SECURITYGROUP, arg0.getName( ), ctx.getAccount( ),
                                       PolicySpec.requestToAction( ctx.getRequest( ) ), ctx.getUser( ) );
    }
  }
  
  public static Predicate<NetworkGroup> userAuthFilter( ) {
    return UserAuthGroupFilter.INSTANCE;
  }
  
  public DescribeSecurityGroupsResponseType describe( final DescribeSecurityGroupsType request ) throws EucalyptusCloudException, MetadataException {
    final DescribeSecurityGroupsResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    NetworkGroups.createDefault( ctx.getUserFullName( ) );//ensure the default group exists to cover some old broken installs
    
    final List<String> groupNames = request.getSecurityGroupSet( );
    final Predicate<NetworkGroup> argListFilter = new Predicate<NetworkGroup>( ) {
      @Override
      public boolean apply( final NetworkGroup arg0 ) {
        return groupNames.isEmpty( ) || groupNames.contains( arg0.getName( ) );
      }
    };
    
    Predicate<NetworkGroup> netFilter = Predicates.and( argListFilter, userAuthFilter( ) );
    OwnerFullName ownerFn = AccountFullName.getInstance( ctx.getAccount( ) );
    if ( Contexts.lookup( ).hasAdministrativePrivileges( ) ) {
      ownerFn = null;
      netFilter = argListFilter;
    }
    
    final EntityTransaction db = Entities.get( NetworkGroup.class );
    try {
      final Iterable<NetworkGroup> matches = Iterables.filter( Entities.query( NetworkGroup.named( ownerFn, null ) ), netFilter );
      final Iterable<SecurityGroupItemType> transformed = Iterables.transform( matches, TypeMappers.lookup( NetworkGroup.class, SecurityGroupItemType.class ) );
      Iterables.addAll( reply.getSecurityGroupInfo( ), transformed );
    } finally {
      db.rollback( );
    }
    return reply;
  }
  
  public RevokeSecurityGroupIngressResponseType revoke( final RevokeSecurityGroupIngressType request ) throws EucalyptusCloudException, MetadataException {
    final Context ctx = Contexts.lookup( );
    final RevokeSecurityGroupIngressResponseType reply = ( RevokeSecurityGroupIngressResponseType ) request.getReply( );
    NetworkGroup ruleGroup = NetworkGroupUtil.getUserNetworkRulesGroup( ctx.getUserFullName( ), request.getGroupName( ) );
    if ( !ctx.hasAdministrativePrivileges( )
         && !Permissions.isAuthorized( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_SECURITYGROUP, request.getGroupName( ), ctx.getAccount( ),
                                       PolicySpec.requestToAction( request ), ctx.getUser( ) ) ) {
      throw new EucalyptusCloudException( "Not authorized to revoke network group " + request.getGroupName( ) + " for " + ctx.getUser( ) );
    }
    final List<NetworkRule> ruleList = Lists.newArrayList( );
    for ( final IpPermissionType ipPerm : request.getIpPermissions( ) ) {
      ruleList.addAll( NetworkGroupUtil.getNetworkRules( ipPerm ) );
    }
    final List<NetworkRule> filtered = Lists.newArrayList( Iterables.filter( ruleGroup.getNetworkRules( ), new Predicate<NetworkRule>( ) {
      @Override
      public boolean apply( final NetworkRule rule ) {
        for ( final NetworkRule r : ruleList ) {
          if ( r.equals( rule ) && r.getNetworkPeers( ).equals( rule.getNetworkPeers( ) ) && r.getIpRanges( ).equals( rule.getIpRanges( ) ) ) {
            return true;
          }
        }
        return false;
      }
    } ) );
    if ( filtered.size( ) == ruleList.size( ) ) {
      try {
        for ( final NetworkRule r : filtered ) {
          ruleGroup.getNetworkRules( ).remove( r );
        }
        ruleGroup = EntityWrapper.get( NetworkGroup.class ).mergeAndCommit( ruleGroup );
      } catch ( final Exception ex ) {
        Logs.extreme( ).error( ex, ex );
        throw new EucalyptusCloudException( "RevokeSecurityGroupIngress failed because: " + ex.getMessage( ), ex );
      }
      return reply;
    } else if ( ( request.getIpPermissions( ).size( ) == 1 ) && ( request.getIpPermissions( ).get( 0 ).getIpProtocol( ) == null ) ) {
      //LAME: this is for the query-based clients which send incomplete named-network requests.
      for ( final NetworkRule rule : ruleList ) {
        if ( ruleGroup.getNetworkRules( ).remove( rule ) ) {
          reply.set_return( true );
        }
      }
      if ( reply.get_return( ) ) {
        try {
          ruleGroup = EntityWrapper.get( ruleGroup ).mergeAndCommit( ruleGroup );
        } catch ( final Exception ex ) {
          Logs.extreme( ).error( ex, ex );
          throw new EucalyptusCloudException( "RevokeSecurityGroupIngress failed because: " + ex.getMessage( ), ex );
        }
      }
      return reply;
    } else {
      return reply.markFailed( );
    }
  }
  
  public AuthorizeSecurityGroupIngressResponseType authorize( final AuthorizeSecurityGroupIngressType request ) throws Exception {
    final Context ctx = Contexts.lookup( );
    final AuthorizeSecurityGroupIngressResponseType reply = ( AuthorizeSecurityGroupIngressResponseType ) request.getReply( );
    final NetworkGroup ruleGroup = NetworkGroupUtil.getUserNetworkRulesGroup( ctx.getUserFullName( ), request.getGroupName( ) );
    if ( !ctx.hasAdministrativePrivileges( )
         && !Permissions.isAuthorized( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_SECURITYGROUP, request.getGroupName( ), ctx.getAccount( ),
                                       PolicySpec.requestToAction( request ), ctx.getUser( ) ) ) {
      throw new EucalyptusCloudException( "Not authorized to authorize network group " + request.getGroupName( ) + " for " + ctx.getUser( ) );
    }
    final List<NetworkRule> ruleList = Lists.newArrayList( );
    for ( final IpPermissionType ipPerm : request.getIpPermissions( ) ) {
      try {
        ruleList.addAll( NetworkGroupUtil.getNetworkRules( ipPerm ) );
      } catch ( final IllegalArgumentException ex ) {
        LOG.error( ex.getMessage( ) );
        reply.set_return( false );
        return reply;
      }
    }
    if ( Iterables.any( ruleGroup.getNetworkRules( ), new Predicate<NetworkRule>( ) {
      @Override
      public boolean apply( final NetworkRule rule ) {
        for ( final NetworkRule r : ruleList ) {
          if ( r.equals( rule ) && r.getNetworkPeers( ).equals( rule.getNetworkPeers( ) ) && r.getIpRanges( ).equals( rule.getIpRanges( ) ) ) {
            return true || !r.isValid( );
          }
        }
        return false;
      }
    } ) ) {
      reply.set_return( false );
      return reply;
    } else {
      ruleGroup.getNetworkRules( ).addAll( ruleList );
      EntityWrapper.get( ruleGroup ).mergeAndCommit( ruleGroup );
      reply.set_return( true );
    }
    return reply;
  }
}
