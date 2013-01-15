/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.network;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.cloud.CloudMetadatas;
import com.eucalyptus.cloud.util.MetadataException;
import com.eucalyptus.cloud.util.NoSuchMetadataException;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.records.Logs;
import com.eucalyptus.tags.Filter;
import com.eucalyptus.tags.Filters;
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
import com.google.common.collect.Sets;
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
      final NetworkGroup group = RestrictedTypes.allocateUnitlessResource( allocator );
      reply.setGroupId( group.getGroupId() );
      return reply;
    } catch ( final Exception ex ) {
      throw new EucalyptusCloudException( "CreateSecurityGroup failed because: " + Exceptions.causeString( ex ), ex );
    }
  }
  
  public DeleteSecurityGroupResponseType delete( final DeleteSecurityGroupType request ) throws EucalyptusCloudException, MetadataException {
    final Context ctx = Contexts.lookup( );
    final DeleteSecurityGroupResponseType reply = ( DeleteSecurityGroupResponseType ) request.getReply( );
    
    String lookUpGroup = request.getGroupName() != null ? request.getGroupName() : request.getGroupId();
    AccountFullName lookUpGroupAccount = ctx.getUserFullName( ).asAccountFullName( );
    
    NetworkGroup group;
    try {
        group = NetworkGroups.lookup( lookUpGroupAccount , lookUpGroup );
    } catch (MetadataException ex ) {
	  try {
	  group = NetworkGroups.lookupByGroupId(lookUpGroupAccount, lookUpGroup);
	  } catch ( NoSuchMetadataException ex1 ) {
	      throw ex1;
	  }
    } 
    
    if ( !RestrictedTypes.filterPrivileged( ).apply(group) ) {
      throw new EucalyptusCloudException( "Not authorized to delete network group " + group.getDisplayName() + " for " + ctx.getUser( ) );
    }
    NetworkGroups.delete( ctx.getUserFullName( ), group.getDisplayName());
    reply.set_return( true );
    return reply;
  }
  
  public DescribeSecurityGroupsResponseType describe( final DescribeSecurityGroupsType request ) throws EucalyptusCloudException, MetadataException, TransactionException {
      final DescribeSecurityGroupsResponseType reply = request.getReply( );
      final Context ctx = Contexts.lookup();
      final Set<String> nameOrIdSet = Sets.newHashSet();
      nameOrIdSet.addAll( request.getSecurityGroupSet( ) );
      nameOrIdSet.addAll( request.getSecurityGroupIdSet() );
      boolean showAll = nameOrIdSet.remove( "verbose" );
      NetworkGroups.createDefault( ctx.getUserFullName( ) ); //ensure the default group exists to cover some old broken installs

      final Filter filter = Filters.generate( request.getFilterSet(), NetworkGroup.class );
      final Predicate<? super NetworkGroup> requestedAndAccessible =
          CloudMetadatas.filteringFor( NetworkGroup.class )
              .byPredicate( Predicates.or(
                  CloudMetadatas.filterById( nameOrIdSet ),
                  CloudMetadatas.filterByProperty( nameOrIdSet, NetworkGroups.groupId() ) ) )
              .byPredicate( filter.asPredicate( ) )
              .byPredicate( Contexts.lookup().hasAdministrativePrivileges( ) ?
                  Predicates.<NetworkGroup>alwaysTrue( ) :
                  RestrictedTypes.<NetworkGroup>filterPrivileged( ) )
              .buildPredicate();

      final OwnerFullName ownerFn = Contexts.lookup( ).hasAdministrativePrivileges( ) && showAll ?
          null :
          AccountFullName.getInstance( ctx.getAccount( ) );

      final Iterable<SecurityGroupItemType> securityGroupItems = Transactions.filteredTransform(
          NetworkGroup.withOwner( ownerFn ),
          filter.asCriterion(),
          filter.getAliases(),
          requestedAndAccessible,
          TypeMappers.lookup( NetworkGroup.class, SecurityGroupItemType.class ) );

      Iterables.addAll( reply.getSecurityGroupInfo( ), securityGroupItems );

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
	  String lookUpGroup = request.getGroupName() != null ? request.getGroupName() : request.getGroupId();
	      AccountFullName lookUpGroupAccount = ctx.getUserFullName( ).asAccountFullName( );
	      String groupName; 
	      
	      try {
	          groupName = NetworkGroups.lookup( lookUpGroupAccount , lookUpGroup ).getDisplayName();
	      } catch (MetadataException ex ) {
		  try {
		  groupName = NetworkGroups.lookupByGroupId(lookUpGroupAccount, lookUpGroup).getDisplayName();
		  } catch ( NoSuchMetadataException ex1 ) {
		      throw ex1;
		  }
	      }
	      
	     final List<NetworkGroup> networkGroupList = NetworkGroups
		    .lookupAll(ctx.getUserFullName().asAccountFullName(),
			    groupName);
	     
	     
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
    
      String lookUpGroup = request.getGroupName() != null ? request.getGroupName() : request.getGroupId();
      AccountFullName lookUpGroupAccount = ctx.getUserFullName( ).asAccountFullName( );
      NetworkGroup ruleGroup; 
      
      try {
          ruleGroup = NetworkGroups.lookup( lookUpGroupAccount , lookUpGroup );
      } catch (MetadataException ex ) {
	  try {
	  ruleGroup = NetworkGroups.lookupByGroupId(lookUpGroupAccount, lookUpGroup);
	  } catch ( NoSuchMetadataException ex1 ) {
	      throw ex1;
	  }
      }
      
      if ( !RestrictedTypes.filterPrivileged( ).apply( ruleGroup ) ) {
        throw new EucalyptusCloudException( "Not authorized to authorize network group " + ruleGroup.getDisplayName() + " for " + ctx.getUser( ) );
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
