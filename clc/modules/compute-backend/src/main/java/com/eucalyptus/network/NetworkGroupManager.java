/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;
import javax.persistence.EntityNotFoundException;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.ClientUnauthorizedComputeException;
import com.eucalyptus.compute.ComputeException;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.internal.network.NetworkGroup;
import com.eucalyptus.compute.common.internal.network.NetworkRule;
import com.eucalyptus.compute.common.internal.util.MetadataConstraintException;
import com.eucalyptus.compute.common.internal.util.MetadataException;
import com.eucalyptus.compute.common.internal.util.NoSuchMetadataException;
import com.eucalyptus.compute.ClientComputeException;
import com.eucalyptus.compute.common.IpPermissionType;
import com.eucalyptus.compute.common.UserIdGroupPairType;
import com.eucalyptus.compute.common.internal.identifier.InvalidResourceIdentifier;
import com.eucalyptus.compute.common.internal.identifier.ResourceIdentifiers;
import com.eucalyptus.compute.common.internal.vpc.Vpc;
import com.eucalyptus.compute.vpc.VpcConfiguration;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.Strings;
import com.eucalyptus.ws.EucalyptusWebServiceException;
import com.google.common.base.CharMatcher;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.eucalyptus.compute.common.backend.AuthorizeSecurityGroupEgressResponseType;
import com.eucalyptus.compute.common.backend.AuthorizeSecurityGroupEgressType;
import com.eucalyptus.compute.common.backend.AuthorizeSecurityGroupIngressResponseType;
import com.eucalyptus.compute.common.backend.AuthorizeSecurityGroupIngressType;
import com.eucalyptus.compute.common.backend.CreateSecurityGroupResponseType;
import com.eucalyptus.compute.common.backend.CreateSecurityGroupType;
import com.eucalyptus.compute.common.backend.DeleteSecurityGroupResponseType;
import com.eucalyptus.compute.common.backend.DeleteSecurityGroupType;
import com.eucalyptus.compute.common.backend.RevokeSecurityGroupEgressResponseType;
import com.eucalyptus.compute.common.backend.RevokeSecurityGroupEgressType;
import com.eucalyptus.compute.common.backend.RevokeSecurityGroupIngressResponseType;
import com.eucalyptus.compute.common.backend.RevokeSecurityGroupIngressType;

@ComponentNamed("computeNetworkGroupManager")
public class NetworkGroupManager {

  public CreateSecurityGroupResponseType create( final CreateSecurityGroupType request ) throws EucalyptusCloudException, MetadataException {
    final Context ctx = Contexts.lookup( );
    final UserFullName userFullName = ctx.getUserFullName();
    final AccountFullName accountFullName = userFullName.asAccountFullName( );
    final String groupName = request.getGroupName( );
    final String groupDescription = request.getGroupDescription( );
    if ( Strings.startsWith( "sg-" ).apply( groupName ) ) {
      throw new ClientComputeException("InvalidParameterValue", "Value ("+groupName+") for parameter GroupName is invalid. Group names may not be in the format sg-*" );
    }
    final String vpcId = Optional.fromNullable( ResourceIdentifiers.tryNormalize( ).apply( request.getVpcId( ) ) )
        .or( Optional.fromNullable( getDefaultVpcId( accountFullName ) ) ).orNull( );
    if ( vpcId == null && !CharMatcher.ASCII.matchesAllOf(groupName) ) {
      throw new ClientComputeException("InvalidParameterValue", "Value ("+groupName+") for parameter GroupName is invalid. Character sets beyond ASCII are not supported.");
    } else if ( vpcId != null && !NetworkGroups.VPC_GROUP_NAME_PATTERN.matcher( groupName ).matches( ) ) {
      throw new ClientComputeException("InvalidParameterValue", "Invalid security group name. Valid names are non-empty strings less than 256 characters from the following set:  a-zA-Z0-9. _-:/()#,@[]+=&;{}!$*");
    }
    if ( vpcId == null && !CharMatcher.ASCII.matchesAllOf( groupDescription ) ) {
      throw new ClientComputeException("InvalidParameterValue", "Value ("+groupDescription+") for parameter GroupDescription is invalid. Character sets beyond ASCII are not supported.");
    } else if ( vpcId != null && !NetworkGroups.VPC_GROUP_DESC_PATTERN.matcher( groupDescription ).matches( ) ) {
      throw new ClientComputeException("InvalidParameterValue", "Invalid security group description. Valid descriptions are strings less than 256 characters from the following set:  a-zA-Z0-9. _-:/()#,@[]+=&;{}!$*");
    }
    final CreateSecurityGroupResponseType reply = request.getReply( );
    try {
      Supplier<NetworkGroup> allocator = new Supplier<NetworkGroup>( ) {
        @Override
        public NetworkGroup get( ) {
          try ( final TransactionResource tx = Entities.transactionFor( NetworkGroup.class ) ) {
            if ( vpcId != null && Entities.count( NetworkGroup.namedForVpc( vpcId, null ) ) >= VpcConfiguration.getSecurityGroupsPerVpc( ) ) {
              throw Exceptions.toUndeclared( new ClientComputeException( "SecurityGroupLimitExceeded", "Security group limit exceeded for " + vpcId ) );
            }
            final Vpc vpc = vpcId == null ?
                null :
                Entities.uniqueResult( Vpc.exampleWithName( userFullName.asAccountFullName( ), vpcId ) );
            final NetworkGroup group = NetworkGroups.create( ctx.getUserFullName( ), vpc, groupName, groupDescription );
            if ( vpc != null ) {
              group.addNetworkRules( Lists.newArrayList(
                  NetworkRule.createEgress( null/*protocol name*/, -1, null/*low port*/, null/*high port*/, null/*peers*/, Collections.singleton( "0.0.0.0/0" ) )
              ) );
            }
            tx.commit();
            return group;
          } catch ( NoSuchElementException e ) {
            throw Exceptions.toUndeclared( new ClientComputeException( "InvalidVpcID.NotFound", "The vpc ('"+vpcId+"') was not found" ) );
          } catch ( TransactionException | MetadataException ex ) {
            throw Exceptions.toUndeclared( ex );
          }
        }
      };
      final NetworkGroup group = RestrictedTypes.allocateUnitlessResource( allocator );
      reply.setGroupId( group.getGroupId( ) );
      return reply;
    } catch ( final Exception ex ) {
      Exceptions.findAndRethrow( ex, ComputeException.class );
      String cause = Exceptions.causeString( ex );
      if ( cause.contains( "DuplicateMetadataException" ) )
          throw new ClientComputeException( "InvalidGroup.Duplicate", "The security group '" + groupName + "' already exists" );
      else
          throw new EucalyptusCloudException( "CreateSecurityGroup failed because: " + cause, ex );
    }
  }

  public DeleteSecurityGroupResponseType delete( final DeleteSecurityGroupType request ) throws EucalyptusCloudException, MetadataException {
    final Context ctx = Contexts.lookup( );
    final DeleteSecurityGroupResponseType reply = request.getReply( );

    final NetworkGroup group = lookupGroup( request.getGroupId(), request.getGroupName() );
    if ( !RestrictedTypes.filterPrivileged( ).apply( group ) ) {
      throw new ClientUnauthorizedComputeException( "Not authorized to delete network group " + group.getDisplayName() + " for " + ctx.getUser( ).getName( ) );
    }

    if ( NetworkGroups.defaultNetworkName( ).equals( group.getDisplayName( ) ) ) {
      if ( group.getVpcId( ) != null ) {
        throw new ClientComputeException( "CannotDelete", "Group ("+group.getGroupId()+") cannot be deleted, it is the default group for " + group.getVpcId( ) );
      } else {
        throw new ClientComputeException( "InvalidGroup.Reserved", "The security group 'default' is reserved" );
      }
    }

    if ( NetworkGroups.defaultNetworkName( ).equals( group.getDisplayName( ) ) ) {
      NetworkGroups.createDefault( AccountFullName.getInstance( group.getOwnerAccountNumber( ) ) );
    }
    try {
      NetworkGroups.delete( group.getGroupId( ) );
    } catch ( MetadataConstraintException e ) {
      throw new ClientComputeException(
          group.getVpcId( ) != null ? "DependencyViolation" : "InvalidGroup.InUse",
          "Specified group cannot be deleted because it is in use." );
    }
    return reply;
  }

  public RevokeSecurityGroupIngressResponseType revokeSecurityGroupIngress( final RevokeSecurityGroupIngressType request ) throws EucalyptusCloudException {
      final RevokeSecurityGroupIngressResponseType reply = request.getReply( ).markFailed( );
      final Context ctx = Contexts.lookup( );
      final Predicate<Void> updateFunction = new Predicate<Void>( ) {
        @Override
        public boolean apply( @Nullable final Void aVoid ) {
          try {
            final NetworkGroup ruleGroup = lookupGroup( request.getGroupId(), request.getGroupName() );
            final List<IpPermissionType> ipPermissions = handleOldAndNewIpPermissions(
                ruleGroup.getVpcId( ) != null,
                request.getCidrIp(), request.getIpProtocol(), request.getFromPort(), request.getToPort(),
                request.getSourceSecurityGroupName(), request.getSourceSecurityGroupOwnerId(),
                request.getIpPermissions());
            if ( RestrictedTypes.filterPrivileged().apply( ruleGroup ) ) {
              try {
                NetworkGroups.resolvePermissions( ipPermissions, ctx.getUser( ).getAccountNumber( ), ruleGroup.getVpcId( ), true );
                if ( Iterators.removeAll( // iterator used to work around broken equals/hashCode in NetworkRule
                    ruleGroup.getNetworkRules( ).iterator( ),
                    NetworkGroups.ipPermissionsAsNetworkRules( ipPermissions, ruleGroup.getVpcId( ) != null, true ) ) ) {
                  ruleGroup.updateTimeStamps( );
                }
              } catch ( IllegalArgumentException e ) {
                throw new ClientComputeException( "InvalidPermission.Malformed", e.getMessage( ) ); 
              } catch ( NoSuchMetadataException e ) {
                throw new ClientComputeException( "InvalidGroup.NotFound", e.getMessage( ) );
              }
            } else {
              throw new ClientUnauthorizedComputeException(
                  "Not authorized to revoke network group "
                      + request.getGroupName() + " for "
                      + ctx.getUser().getName());
            }
          } catch ( final EntityNotFoundException e ) {
            // Another rule may have been deleted, retry
            throw new Entities.RetryTransactionException( e );
          } catch ( final Exception e ) {
            throw Exceptions.toUndeclared( e );
          }
          return true;
        }
      };

      try {
        reply.set_return( Entities.asDistinctTransaction( NetworkGroup.class, updateFunction ).apply( null ) );
        NetworkGroups.flushRules();
      } catch ( Exception ex ) {
        Exceptions.findAndRethrow( ex, EucalyptusCloudException.class, EucalyptusWebServiceException.class );
        Logs.exhaust( ).error( ex, ex );
        throw new EucalyptusCloudException( "RevokeSecurityGroupIngress failed because: " + ex.getMessage( ), ex );
      }
      return reply;
    }
  
  public AuthorizeSecurityGroupIngressResponseType authorizeSecurityGroupIngress( final AuthorizeSecurityGroupIngressType request ) throws Exception {
    final AuthorizeSecurityGroupIngressResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final Predicate<Void> updateFunction = new Predicate<Void>( ) {
      @Override
      public boolean apply( @Nullable final Void aVoid ) {
        try {
          final NetworkGroup ruleGroup = lookupGroup( request.getGroupId(), request.getGroupName() );
          if ( !RestrictedTypes.filterPrivileged( ).apply( ruleGroup ) ) {
            throw new ClientUnauthorizedComputeException( "Not authorized to authorize network group " + ruleGroup.getDisplayName() + " for " + ctx.getUser( ).getName() );
          }
          final List<NetworkRule> ruleList = Lists.newArrayList( );
          final List<IpPermissionType> ipPermissions = handleOldAndNewIpPermissions(
              ruleGroup.getVpcId( ) != null,
              request.getCidrIp(), request.getIpProtocol(), request.getFromPort(), request.getToPort(),
              request.getSourceSecurityGroupName(), request.getSourceSecurityGroupOwnerId(),
              request.getIpPermissions());
          try {
            NetworkGroups.resolvePermissions( ipPermissions, ctx.getUser().getAccountNumber(), ruleGroup.getVpcId(), false );
          } catch ( final NoSuchMetadataException e ) {
            throw new ClientComputeException( "InvalidGroup.NotFound", e.getMessage( ) );
          }
          for ( final IpPermissionType ipPerm : ipPermissions ) {
            if ( ipPerm.getCidrIpRanges().isEmpty() && ipPerm.getGroups().isEmpty() ) {
              continue; // see EUCA-5934
            }
            if ((ipPerm.getIpProtocol( ).equals( "icmp" ) || (ipPerm.getIpProtocol( ).equals( "1" ))) &&
                ( MoreObjects.firstNonNull( ipPerm.getFromPort( ), 0 ) > 255 || MoreObjects.firstNonNull( ipPerm.getToPort( ), 0 ) > 255)) {
              throw new ClientComputeException( "InvalidParameterValue", "ICMP code (" +
                  ( ( MoreObjects.firstNonNull( ipPerm.getFromPort( ), 0 ) > 255 ) ?
                      ipPerm.getFromPort( ) : ipPerm.getToPort( ) ) + ") " +
                        "out of range. Supported range for ICMP code and type is 0 to 255" );
            }
            if ( ipPerm.getIpProtocol( ) != null && !NetworkRule.PROTOCOL_PATTERN.matcher( ipPerm.getIpProtocol( ) ).matches( ) ) {
              throw new ClientComputeException("InvalidPermission.Malformed", "Invalid protocol ("+ipPerm.getIpProtocol( )+")" );
            }
            try {
              final List<NetworkRule> rules = NetworkGroups.ipPermissionAsNetworkRules( ipPerm, ruleGroup.getVpcId( ) != null, false );
              ruleList.addAll( rules );
            } catch ( final IllegalArgumentException ex ) {
              throw new ClientComputeException("InvalidPermission.Malformed", ex.getMessage( ) );
            }
          }
          if ( Iterables.any( ruleGroup.getNetworkRules( ), new Predicate<NetworkRule>( ) {
            @Override
            public boolean apply( final NetworkRule rule ) {
              return Iterables.any( ruleList, Predicates.equalTo( rule ) );
            }
          } ) ) {
            return false;
          } else {
            ruleGroup.addNetworkRules(  ruleList );
            if ( ruleGroup.getVpcId() != null && ruleGroup.getNetworkRules().size() > VpcConfiguration.getRulesPerSecurityGroup() ) {
              throw new ClientComputeException( "RulesPerSecurityGroupLimitExceeded", "Rules limit exceeded for " + request.getGroupId() );
            }
          }
        } catch ( final Exception e ) {
          throw Exceptions.toUndeclared( e );
        }
        return true;
      }
    };

    try {
      reply.set_return( Entities.asDistinctTransaction( NetworkGroup.class, updateFunction ).apply( null ) );
      NetworkGroups.flushRules( );
    } catch ( Exception ex ) {
      Exceptions.findAndRethrow( ex, EucalyptusCloudException.class, EucalyptusWebServiceException.class );
      Logs.exhaust( ).error( ex, ex );
      throw ex;
    }    
    return reply;
  }

  public AuthorizeSecurityGroupEgressResponseType authorizeSecurityGroupEgress(final AuthorizeSecurityGroupEgressType request) throws Exception {
    final AuthorizeSecurityGroupEgressResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    final Predicate<Void> updateFunction = new Predicate<Void>( ) {
      @Override
      public boolean apply( @Nullable final Void aVoid ) {
        try {
          final NetworkGroup ruleGroup = lookupGroup( request.getGroupId(), null );
          if ( !RestrictedTypes.filterPrivileged( ).apply( ruleGroup ) ) {
            throw new ClientUnauthorizedComputeException( "Not authorized to authorize network group " + ruleGroup.getDisplayName() + " for " + ctx.getUser( ).getName() );
          }
          if ( ruleGroup.getVpcId( ) == null ) {
            throw new ClientComputeException( "InvalidGroup.NotFound", "VPC security group ("+request.getGroupId()+") not found" );
          }
          final List<NetworkRule> ruleList = Lists.newArrayList( );
          final List<IpPermissionType> ipPermissions = request.getIpPermissions( );
          try {
            NetworkGroups.resolvePermissions( ipPermissions, ctx.getUser().getAccountNumber(), ruleGroup.getVpcId(), false );
          } catch ( final NoSuchMetadataException e ) {
            throw new ClientComputeException( "InvalidGroup.NotFound", e.getMessage( ) );
          }
          for ( final IpPermissionType ipPerm : ipPermissions ) {
            if ( ipPerm.getCidrIpRanges().isEmpty() && ipPerm.getGroups().isEmpty() ) {
              continue; // see EUCA-5934
            }
            if ((ipPerm.getIpProtocol( ).equals( "icmp" ) || (ipPerm.getIpProtocol( ).equals( "1" ))) &&
                ( MoreObjects.firstNonNull( ipPerm.getFromPort( ), 0 ) > 255 || MoreObjects.firstNonNull( ipPerm.getToPort( ), 0 ) > 255)) {
              throw new ClientComputeException( "InvalidParameterValue", "ICMP code (" +
                  ( ( MoreObjects.firstNonNull( ipPerm.getFromPort( ), 0 ) > 255 ) ?
                      ipPerm.getFromPort( ) : ipPerm.getToPort( ) ) + ") " +
                      "out of range. Supported range for ICMP code and type is 0 to 255" );
            }
            if ( ipPerm.getIpProtocol( ) == null || !NetworkRule.PROTOCOL_PATTERN.matcher( ipPerm.getIpProtocol( ) ).matches( ) ) {
              throw new ClientComputeException("InvalidPermission.Malformed", "Invalid protocol ("+ipPerm.getIpProtocol( )+")" );
            }
            try {
              final List<NetworkRule> rules = NetworkGroups.ipPermissionAsNetworkRules( ipPerm, ruleGroup.getVpcId( ) != null, false );
              for ( final NetworkRule rule : rules ) rule.setEgress( true );
              ruleList.addAll( rules );
            } catch ( final IllegalArgumentException ex ) {
              throw new ClientComputeException("InvalidPermission.Malformed", ex.getMessage( ) );
            }
          }
          if ( Iterables.any( ruleGroup.getNetworkRules( ), new Predicate<NetworkRule>( ) {
            @Override
            public boolean apply( final NetworkRule rule ) {
              return Iterables.any( ruleList, Predicates.equalTo( rule ) );
            }
          } ) ) {
            return false;
          } else {
            ruleGroup.addNetworkRules( ruleList );
            if ( ruleGroup.getVpcId( ) != null && ruleGroup.getNetworkRules( ).size( ) > VpcConfiguration.getRulesPerSecurityGroup( ) ) {
              throw new ClientComputeException("RulesPerSecurityGroupLimitExceeded", "Rules limit exceeded for " + request.getGroupId( ) );
            }
          }
        } catch ( final Exception e ) {
          throw Exceptions.toUndeclared( e );
        }
        return true;
      }
    };
    
    try {
      reply.set_return( Entities.asDistinctTransaction( NetworkGroup.class, updateFunction ).apply( null ) );
      NetworkGroups.flushRules();
    } catch ( Exception ex ) {
      Exceptions.findAndRethrow( ex, EucalyptusCloudException.class, EucalyptusWebServiceException.class );
      Logs.exhaust( ).error( ex, ex );
      throw ex;
    }
    return reply;
  }

  public RevokeSecurityGroupEgressResponseType revokeSecurityGroupEgress(final RevokeSecurityGroupEgressType request) throws EucalyptusCloudException {
    final RevokeSecurityGroupEgressResponseType reply = request.getReply( ).markFailed( );
    final Context ctx = Contexts.lookup( );
    final Predicate<Void> updateFunction = new Predicate<Void>( ) {
      @Override
      public boolean apply( @Nullable final Void aVoid ) {
        try {
          final NetworkGroup ruleGroup = lookupGroup( request.getGroupId(), null );
          final List<IpPermissionType> ipPermissions = request.getIpPermissions( );
          if ( RestrictedTypes.filterPrivileged().apply( ruleGroup ) ) {
            if ( ruleGroup.getVpcId( ) == null ) {
              throw new ClientComputeException( "InvalidGroup.NotFound", "VPC security group ("+request.getGroupId()+") not found" );
            }
            try {
              final List<NetworkRule> rules = NetworkGroups.ipPermissionsAsNetworkRules( ipPermissions, true, true );
              for ( final NetworkRule rule : rules ) rule.setEgress( true );
              NetworkGroups.resolvePermissions( ipPermissions, ctx.getUser( ).getAccountNumber( ), ruleGroup.getVpcId( ), true );
              if ( Iterators.removeAll( // iterator used to work around broken equals/hashCode in NetworkRule
                  ruleGroup.getNetworkRules( ).iterator( ),
                  rules ) ) {
                ruleGroup.updateTimeStamps( );
              }
            } catch ( IllegalArgumentException e ) {
              throw new ClientComputeException( "InvalidPermission.Malformed", e.getMessage( ) );
            } catch ( NoSuchMetadataException e ) {
              throw new ClientComputeException( "InvalidGroup.NotFound", e.getMessage( ) );
            }
          } else {
            throw new ClientUnauthorizedComputeException(
                "Not authorized to revoke network group "
                    + request.getGroupId() + " for "
                    + ctx.getUser().getName());
          }
        } catch ( final EntityNotFoundException e ) {
          // Another rule may have been deleted, retry
          throw new Entities.RetryTransactionException( e );
        } catch ( final Exception e ) {
          throw Exceptions.toUndeclared( e );
        }
        return true;
      }
    };

    try {
      reply.set_return( Entities.asDistinctTransaction( NetworkGroup.class, updateFunction ).apply( null ) );
      NetworkGroups.flushRules();
    } catch ( Exception ex ) {
      Exceptions.findAndRethrow( ex, EucalyptusCloudException.class, EucalyptusWebServiceException.class );
      Logs.exhaust( ).error( ex, ex );
      throw new EucalyptusCloudException( "RevokeSecurityGroupEgress failed because: " + ex.getMessage( ), ex );
    }
    return reply;
  }

  private List<IpPermissionType> handleOldAndNewIpPermissions(
      final boolean isVpcGroup,
      String cidrIp, String ipProtocol,
      Integer fromPort, Integer toPort, String sourceSecurityGroupName, String sourceSecurityGroupOwnerId,
      ArrayList<IpPermissionType> ipPermissions) throws MetadataException {

    // TODO: match AWS error messages (whenever possible)
    
    // Due to old api calls there are three possible (allowed) scenarios
    // 1) cidrIp, ip protocol, from port, to port must all be set
    // 2) sourceSecurityGroupName and sourceSecurityGroupOwnerId must be set
    // 3) ipPermissions must be set (size at least 1.
    // Exactly one of the above must be set, and no fields from any other condition must be set.
    // Easiest to start with condition 3
    
    HashMap<String, Object> condition1Params = Maps.newHashMap( );
    condition1Params.put("cidrIp", cidrIp);
    condition1Params.put("ipProtocol", ipProtocol);
    if ( !isVpcGroup ) {
      condition1Params.put("fromPort", fromPort);
      condition1Params.put("toPort", toPort);
    }

    HashMap<String, Object> condition2Params = Maps.newHashMap( );
    condition2Params.put("sourceSecurityGroupName", sourceSecurityGroupName);
    condition2Params.put("sourceSecurityGroupOwnerId", sourceSecurityGroupOwnerId);

    if (ipPermissions != null && ipPermissions.size() > 0) {
      for (String key: condition1Params.keySet()) {
        Object value = condition1Params.get(key);
        if (value != null) {
          throw new MetadataException("InvalidParameterCombination: " + key + " and ipPermissions must not both be set");
        }
      }
      for (String key: condition2Params.keySet()) {
        Object value = condition2Params.get(key);
        if (value != null) {
          throw new MetadataException("InvalidParameterCombination: " + key + " and ipPermissions must not both be set");
        }
      }
      return ipPermissions;
    }
    // now check 2
    String unsetCondition2Key = null;
    String setCondition2Key = null;
    for (String key: condition2Params.keySet()) {
      Object value = condition2Params.get(key);
      if (value == null && unsetCondition2Key == null) {
        unsetCondition2Key = key;
      } else if (value != null && setCondition2Key == null) {
        setCondition2Key = key;
      }
    }
    if (setCondition2Key != null) { 
      if (unsetCondition2Key != null) {
        throw new MetadataException( "MissingParameter: " + unsetCondition2Key + " must be set if " + setCondition2Key + " is set." );
      } else if ( isVpcGroup ) {
        throw new MetadataException( "MissingParameter: IpProtocol" );
      } else {
        // both conditions are set, make sure no condition 1 items are set...
        for (String key: condition1Params.keySet()) {
          Object value = condition1Params.get(key);
          if (value != null) {
            throw new MetadataException("InvalidParameterCombination: " + key + " and " + setCondition2Key + " must not both be set");
          }
        }
        // set a rule for tcp:1-65535, udp:1-65535, icmp: -1
        IpPermissionType tcpPermission = new IpPermissionType("tcp", 1, 65535);
        tcpPermission.setGroups(Lists.newArrayList(new UserIdGroupPairType(sourceSecurityGroupOwnerId, sourceSecurityGroupName, null)));
        IpPermissionType udpPermission = new IpPermissionType("udp", 1, 65535); 
        udpPermission.setGroups(Lists.newArrayList(new UserIdGroupPairType(sourceSecurityGroupOwnerId, sourceSecurityGroupName, null)));
        IpPermissionType icmpPermission = new IpPermissionType("icmp", -1, -1); 
        icmpPermission.setGroups(Lists.newArrayList(new UserIdGroupPairType(sourceSecurityGroupOwnerId, sourceSecurityGroupName, null)));
        return Lists.newArrayList(tcpPermission, udpPermission, icmpPermission);
      }
    }
    // now in condition 1.  make sure all fields set.
    // now check 2
    String unsetCondition1Key = null;
    String setCondition1Key = null;
    for (String key: condition1Params.keySet()) {
      Object value = condition1Params.get(key);
      if (value == null && unsetCondition1Key == null) {
        unsetCondition1Key = key;
      } else if (value != null && setCondition1Key == null) {
        setCondition1Key = key;
      }
    }
    if (setCondition1Key != null) { 
      if (unsetCondition1Key != null) {
        throw new MetadataException("MissingParameter: " + unsetCondition1Key + " must be set if " + setCondition1Key + " is set.");
      } else {
        // we have everything we need
        IpPermissionType permission = new IpPermissionType(ipProtocol, fromPort, toPort);
        permission.setCidrIpRanges(Lists.newArrayList(cidrIp));
        return Lists.newArrayList(permission);
      }
    }
    throw new MetadataException("Missing source specification: include source security group or CIDR information");
  }

  /**
   * Caller must perform authorization checks
   */
  private static NetworkGroup lookupGroup( final String groupId,
                                           final String groupName ) throws EucalyptusCloudException, MetadataException {
    final Context ctx = Contexts.lookup( );
    final AccountFullName lookUpGroupAccount = ctx.getUserFullName( ).asAccountFullName();
    try ( final TransactionResource tx = Entities.transactionFor( NetworkGroup.class ) ){
      if ( groupName != null ) {
        final String defaultVpcId = getDefaultVpcId( lookUpGroupAccount );
        if ( defaultVpcId != null ) {
          return NetworkGroups.lookup( lookUpGroupAccount, defaultVpcId, groupName );
        } else {
          return NetworkGroups.lookup( lookUpGroupAccount, groupName );
        }
      } else if ( groupId != null ) {
        return NetworkGroups.lookupByGroupId(
            ctx.isAdministrator( ) ?
                null :
                lookUpGroupAccount,
            normalizeGroupIdentifier( groupId ) );
      } else {
        throw new EucalyptusCloudException( "Group id or name required" );
      }
    } catch ( NoSuchMetadataException e ) {
      throw new ClientComputeException(
          "InvalidGroup.NotFound",
          String.format( "The security group '%s' does not exist", Objects.firstNonNull( groupName, groupId ) ) );
    }
  }

  private static String getDefaultVpcId( final AccountFullName accountFullName ) {
    try ( final TransactionResource tx = Entities.transactionFor( Vpc.class ) ) {
      return Iterables.tryFind(
          Entities.query( Vpc.exampleDefault( accountFullName ) ),
          Predicates.alwaysTrue()
      ).transform( CloudMetadatas.toDisplayName() ).orNull();
    }
  }

  private static String normalizeIdentifier( final String identifier,
                                             final String prefix,
                                             final boolean required,
                                             final String message ) throws ClientComputeException {
    try {
      return com.google.common.base.Strings.emptyToNull( identifier ) == null && !required ?
          null :
          ResourceIdentifiers.parse( prefix, identifier ).getIdentifier( );
    } catch ( final InvalidResourceIdentifier e ) {
      throw new ClientComputeException( "InvalidGroupId.Malformed", String.format( message, e.getIdentifier( ) ) );
    }
  }

  private static String normalizeGroupIdentifier( final String identifier ) throws EucalyptusCloudException {
    return normalizeIdentifier(
        identifier, NetworkGroup.ID_PREFIX, true, "Invalid id: \"%s\" (expecting \"sg-...\")" );
  }

}
