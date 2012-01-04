package com.eucalyptus.network;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.cloud.util.MetadataException;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
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
    
    final CreateSecurityGroupResponseType reply = ( CreateSecurityGroupResponseType ) request.getReply( );
    try {
      Supplier<NetworkGroup> allocator = new Supplier<NetworkGroup>( ) {
        
        @Override
        public NetworkGroup get( ) {
          try {
            return NetworkGroups.create( ctx.getUserFullName( ), request.getGroupName( ), request.getGroupDescription( ) );
          } catch ( MetadataException ex ) {
            throw new RuntimeException( ex );
          }
        }
      };
      RestrictedTypes.allocateUnitlessResource( allocator );
      return reply;
    } catch ( final Exception ex ) {
      throw new EucalyptusCloudException( "CreateSecurityGroup failed because: " + Exceptions.causeString( ex ), ex );
    }
  }
  
  public DeleteSecurityGroupResponseType delete( final DeleteSecurityGroupType request ) throws EucalyptusCloudException, MetadataException {
    final Context ctx = Contexts.lookup( );
    final DeleteSecurityGroupResponseType reply = ( DeleteSecurityGroupResponseType ) request.getReply( );
    if ( !RestrictedTypes.filterPrivileged( ).apply( NetworkGroups.lookup( ctx.getUserFullName( ).asAccountFullName( ), request.getGroupName( ) ) ) ) {
      throw new EucalyptusCloudException( "Not authorized to delete network group " + request.getGroupName( ) + " for " + ctx.getUser( ) );
    }
    NetworkGroups.delete( ctx.getUserFullName( ), request.getGroupName( ) );
    reply.set_return( true );
    return reply;
  }
  
  public DescribeSecurityGroupsResponseType describe( final DescribeSecurityGroupsType request ) throws EucalyptusCloudException, MetadataException {
    final DescribeSecurityGroupsResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    boolean showAll = request.getSecurityGroupSet( ).remove( "verbose" );
    NetworkGroups.createDefault( ctx.getUserFullName( ) );//ensure the default group exists to cover some old broken installs
   
    final List<String> groupNames = request.getSecurityGroupSet( );
    final Predicate<NetworkGroup> argListFilter = new Predicate<NetworkGroup>( ) {
      @Override
      public boolean apply( final NetworkGroup arg0 ) {
        return groupNames.isEmpty( ) || groupNames.contains( arg0.getDisplayName( ) );
      }
    };
    
    Predicate<NetworkGroup> netFilter = Predicates.and( argListFilter, RestrictedTypes.filterPrivileged( ) );
    OwnerFullName ownerFn = AccountFullName.getInstance( ctx.getAccount( ) );
    if ( Contexts.lookup( ).hasAdministrativePrivileges( ) ) {
      if ( showAll ) {
        ownerFn = null;
      }
      netFilter = argListFilter;
    }
    
    final EntityTransaction db = Entities.get( NetworkGroup.class );
    try {
      final List<NetworkGroup> networks = Entities.query( NetworkGroup.named( ownerFn, null ) );
      final Iterable<NetworkGroup> matches = Iterables.filter( networks, netFilter );
      final Iterable<SecurityGroupItemType> transformed = Iterables.transform( matches, TypeMappers.lookup( NetworkGroup.class, SecurityGroupItemType.class ) );
      Iterables.addAll( reply.getSecurityGroupInfo( ), transformed );
      db.commit( );
    } catch ( final Exception ex ) {
      db.rollback( );
    }
    
    return reply;
  }
  
  public RevokeSecurityGroupIngressResponseType revoke( final RevokeSecurityGroupIngressType request ) throws EucalyptusCloudException, MetadataException {
      
      final Context ctx = Contexts.lookup( );
      final RevokeSecurityGroupIngressResponseType reply = request.getReply( );
      reply.markFailed( );
      final List<IpPermissionType> ipPermissions = request.getIpPermissions( );
      final List<NetworkRule> revokedRuleList = NetworkGroups.ipPermissionsAsNetworkRules( ipPermissions );
      
      EntityTransaction db = Entities.get( NetworkGroup.class );
      
      try {      
	 
	    final List<NetworkGroup> networkGroupList = NetworkGroups
		    .lookupAll(ctx.getUserFullName().asAccountFullName(),
			    request.getGroupName());

	    for (NetworkGroup networkGroup : networkGroupList) {

		if (RestrictedTypes.filterPrivileged().apply(networkGroup)) {

		    for (Iterator< NetworkRule > it = networkGroup.getNetworkRules().iterator(); it.hasNext() ;) {
			
			NetworkRule rule = it.next();
			if (revokedRuleList.contains(rule)) {
			  it.remove();
			}
		    }

		} else {
		    throw new EucalyptusCloudException(
			    "Not authorized to revoke" + "network group "
				    + request.getGroupName() + " for "
				    + ctx.getUser());

		}
	    }
        reply.set_return(true);    
        db.commit( );
      } catch ( Exception ex ) {
        Logs.exhaust( ).error( ex, ex );
        db.rollback( );
        throw new EucalyptusCloudException( "RevokeSecurityGroupIngress failed because: " + ex.getMessage( ), ex );
      }
      return reply;
    }
  
  public AuthorizeSecurityGroupIngressResponseType authorize( final AuthorizeSecurityGroupIngressType request ) throws Exception {
    final Context ctx = Contexts.lookup( );
    final AuthorizeSecurityGroupIngressResponseType reply = request.getReply( );
    
    EntityTransaction db = Entities.get( NetworkGroup.class );
    try {
      final NetworkGroup ruleGroup = NetworkGroups.lookup( ctx.getUserFullName( ).asAccountFullName( ), request.getGroupName( ) );
      if ( !RestrictedTypes.filterPrivileged( ).apply( ruleGroup ) ) {
        throw new EucalyptusCloudException( "Not authorized to authorize network group " + request.getGroupName( ) + " for " + ctx.getUser( ) );
      }
      final List<NetworkRule> ruleList = Lists.newArrayList( );
      for ( final IpPermissionType ipPerm : request.getIpPermissions( ) ) {
        try {
          ruleList.addAll( NetworkGroups.IpPermissionTypeAsNetworkRule.INSTANCE.apply( ipPerm ) );
        } catch ( final IllegalArgumentException ex ) {
          LOG.error( ex.getMessage( ) );
          reply.set_return( false );
          db.rollback( );
          return reply;
        }
      }
      if ( Iterables.any( ruleGroup.getNetworkRules( ), new Predicate<NetworkRule>( ) {
        @Override
        public boolean apply( final NetworkRule rule ) {
          return Iterables.any( ruleList, Predicates.equalTo( rule ) );
        }
      } ) ) {
        reply.set_return( false );
        db.rollback( );
        return reply;
      } else {
        ruleGroup.getNetworkRules( ).addAll( ruleList );
        reply.set_return( true );
      }
      db.commit( );
      return reply;
    } catch ( Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      db.rollback( );
      throw ex;
    }
  }
}
