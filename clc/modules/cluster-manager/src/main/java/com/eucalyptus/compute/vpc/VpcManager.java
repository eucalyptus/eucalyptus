/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
 ************************************************************************/
package com.eucalyptus.compute.vpc;

import static com.google.common.base.Objects.firstNonNull;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthQuotaException;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloud.util.NoSuchMetadataException;
import com.eucalyptus.cloud.util.ResourceAllocationException;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.ClientComputeException;
import com.eucalyptus.compute.ClientUnauthorizedComputeException;
import com.eucalyptus.compute.ComputeException;
import com.eucalyptus.compute.common.AccountAttributeSetItemType;
import com.eucalyptus.compute.common.AttributeBooleanValueType;
import com.eucalyptus.compute.common.CloudMetadata;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.DhcpConfigurationItemType;
import com.eucalyptus.compute.common.DhcpOptionsType;
import com.eucalyptus.compute.common.GroupItemType;
import com.eucalyptus.compute.common.GroupSetType;
import com.eucalyptus.compute.common.InternetGatewayType;
import com.eucalyptus.compute.common.NetworkAclType;
import com.eucalyptus.compute.common.NetworkInterfaceAttachmentType;
import com.eucalyptus.compute.common.NetworkInterfaceType;
import com.eucalyptus.compute.common.NullableAttributeValueType;
import com.eucalyptus.compute.common.ResourceTagSetItemType;
import com.eucalyptus.compute.common.ResourceTagSetType;
import com.eucalyptus.compute.common.RouteTableType;
import com.eucalyptus.compute.common.SubnetType;
import com.eucalyptus.compute.common.VpcTagged;
import com.eucalyptus.compute.common.VpcType;
import com.eucalyptus.compute.common.network.Networking;
import com.eucalyptus.compute.common.network.NetworkingFeature;
import com.eucalyptus.compute.identifier.InvalidResourceIdentifier;
import com.eucalyptus.compute.identifier.ResourceIdentifiers;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.network.IPRange;
import com.eucalyptus.network.NetworkGroup;
import com.eucalyptus.network.NetworkGroups;
import com.eucalyptus.network.NetworkPeer;
import com.eucalyptus.network.NetworkRule;
import com.eucalyptus.network.PrivateAddresses;
import com.eucalyptus.tags.*;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Cidr;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.RestrictedType;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.dns.DomainNames;
import com.eucalyptus.vm.VmInstances;
import com.google.common.base.Enums;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.net.InetAddresses;
import com.google.common.net.InternetDomainName;
import com.google.common.primitives.Ints;
import com.eucalyptus.compute.common.backend.*;


/**
 *
 */
@SuppressWarnings( { "UnnecessaryLocalVariable", "UnusedDeclaration" } )
@ComponentNamed
public class VpcManager {

  private static final Logger logger = Logger.getLogger( VpcManager.class );

  private final DhcpOptionSets dhcpOptionSets;
  private final InternetGateways internetGateways;
  private final NetworkAcls networkAcls;
  private final NetworkInterfaces networkInterfaces;
  private final RouteTables routeTables;
  private final SecurityGroups securityGroups;
  private final Subnets subnets;
  private final Vpcs vpcs;
  private final VpcInvalidator vpcInvalidator;

  @Inject
  public VpcManager( final DhcpOptionSets dhcpOptionSets,
                     final InternetGateways internetGateways,
                     final NetworkAcls networkAcls,
                     final NetworkInterfaces networkInterfaces,
                     final RouteTables routeTables,
                     final SecurityGroups securityGroups,
                     final Subnets subnets,
                     final Vpcs vpcs,
                     final VpcInvalidator vpcInvalidator ) {
    this.dhcpOptionSets = dhcpOptionSets;
    this.internetGateways = internetGateways;
    this.networkAcls = networkAcls;
    this.networkInterfaces = networkInterfaces;
    this.routeTables = routeTables;
    this.securityGroups = securityGroups;
    this.subnets = subnets;
    this.vpcs = vpcs;
    this.vpcInvalidator = vpcInvalidator;
  }

  public AcceptVpcPeeringConnectionResponseType acceptVpcPeeringConnection(AcceptVpcPeeringConnectionType request) throws EucalyptusCloudException {
    AcceptVpcPeeringConnectionResponseType reply = request.getReply( );
    return reply;
  }

  public AssignPrivateIpAddressesResponseType assignPrivateIpAddresses(AssignPrivateIpAddressesType request) throws EucalyptusCloudException {
    AssignPrivateIpAddressesResponseType reply = request.getReply( );
    return reply;
  }

  public AssociateDhcpOptionsResponseType associateDhcpOptions(final AssociateDhcpOptionsType request) throws EucalyptusCloudException {
    final AssociateDhcpOptionsResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    final String dhcpOptionsId = "default".equals( request.getDhcpOptionsId( ) ) ? "default" : Identifier.dopt.normalize( request.getDhcpOptionsId( ) );
    final String vpcId = Identifier.vpc.normalize( request.getVpcId( ) );
    try {
      vpcs.updateByExample(
          Vpc.exampleWithName( accountFullName, vpcId ),
          accountFullName,
          request.getVpcId(),
          new Callback<Vpc>() {
            @Override
            public void fire( final Vpc vpc ) {
              if ( RestrictedTypes.filterPrivileged( ).apply( vpc ) ) try {
                final DhcpOptionSet dhcpOptionSet = "default".equals( dhcpOptionsId ) ?
                    dhcpOptionSets.lookupByExample( DhcpOptionSet.exampleDefault( accountFullName ), accountFullName, "default", Predicates.alwaysTrue( ), Functions.<DhcpOptionSet>identity() ):
                    dhcpOptionSets.lookupByName( accountFullName, dhcpOptionsId, Functions.<DhcpOptionSet>identity( ) );
                vpc.setDhcpOptionSet( dhcpOptionSet );
              } catch ( VpcMetadataNotFoundException e ) {
                throw Exceptions.toUndeclared( new ClientComputeException( "InvalidDhcpOptionsID.NotFound", "DHCP options not found '" + request.getDhcpOptionsId() + "'" ) );
              } catch ( Exception e ) {
                throw Exceptions.toUndeclared( e );
              } else {
                throw Exceptions.toUndeclared( new ClientUnauthorizedComputeException( "Not authorized to associate DHCP options" ) );
              }
            }
          } );
      invalidate( vpcId );
    } catch ( VpcMetadataNotFoundException e ) {
      throw new ClientComputeException( "InvalidVpcID.NotFound", "Vpc not found '" + request.getVpcId() + "'" );
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public AssociateRouteTableResponseType associateRouteTable(final AssociateRouteTableType request) throws EucalyptusCloudException {
    final AssociateRouteTableResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    final String routeTableId = Identifier.rtb.normalize( request.getRouteTableId() );
    final String subnetId = Identifier.subnet.normalize( request.getSubnetId() );
    try {
      final RouteTable routeTable = routeTables.updateByExample(
          RouteTable.exampleWithName( accountFullName, routeTableId ),
          accountFullName,
          request.getRouteTableId(),
          new Callback<RouteTable>() {
            @Override
            public void fire( final RouteTable routeTable ) {
              if ( RestrictedTypes.filterPrivileged( ).apply( routeTable ) ) try {
                final Subnet subnet = subnets.lookupByName( accountFullName, subnetId, Functions.<Subnet>identity( ) );

                if ( !subnet.getVpc( ).getDisplayName( ).equals( routeTable.getVpc( ).getDisplayName( ) ) ) {
                  throw Exceptions.toUndeclared( new ClientComputeException( "InvalidParameterValue",
                      "Route table "+routeTableId+" and subnet "+subnetId+" belong to different networks" ) );
                }

                if ( !Iterables.tryFind(
                    routeTable.getRouteTableAssociations( ),
                    CollectionUtils.propertyPredicate( subnetId, RouteTables.AssociationFilterStringFunctions.SUBNET_ID ) ).isPresent( ) ) {
                  routeTable.associate( subnet );
                }
              } catch ( VpcMetadataNotFoundException e ) {
                throw Exceptions.toUndeclared( new ClientComputeException( "InvalidSubnetID.NotFound", "Subnet ("+request.getSubnetId()+") not found" ) );
              } catch ( Exception e ) {
                throw Exceptions.toUndeclared( e );
              } else {
                throw Exceptions.toUndeclared( new ClientUnauthorizedComputeException( "Not authorized to associate route table" ) );
              }
            }
          } );
      final RouteTableAssociation association = Iterables.find(
          routeTable.getRouteTableAssociations( ),
          CollectionUtils.propertyPredicate( subnetId, RouteTables.AssociationFilterStringFunctions.SUBNET_ID ) );
      reply.setAssociationId( association.getAssociationId( ) );
      invalidate( subnetId );
    } catch ( VpcMetadataNotFoundException e ) {
      throw new ClientComputeException( "InvalidRouteTableID.NotFound", "Route table not found '" + request.getRouteTableId() + "'" );
    } catch ( Exception e ) {
      if ( Exceptions.isCausedBy( e, ConstraintViolationException.class ) ) {
        throw new ClientComputeException( "InvalidParameterValue", "Subnet "+subnetId+" already associated." );
      }
      throw handleException( e );
    }
    return reply;
  }

  public AttachInternetGatewayResponseType attachInternetGateway( final AttachInternetGatewayType request ) throws EucalyptusCloudException {
    final AttachInternetGatewayResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    final String gatewayId = Identifier.igw.normalize( request.getInternetGatewayId( ) );
    final String vpcId = Identifier.vpc.normalize( request.getVpcId( ) );
    try {
      internetGateways.updateByExample(
          InternetGateway.exampleWithName( accountFullName, gatewayId ),
          accountFullName,
          request.getInternetGatewayId(),
          new Callback<InternetGateway>() {
            @Override
            public void fire( final InternetGateway internetGateway ) {
              if ( RestrictedTypes.filterPrivileged( ).apply( internetGateway ) ) try {
                final Vpc vpc = vpcs.lookupByName( accountFullName, vpcId, Functions.<Vpc>identity( ) );
                if ( internetGateway.getVpc( ) != null ) {
                  throw Exceptions.toUndeclared( new ClientComputeException( "Resource.AlreadyAssociated",
                      "resource "+gatewayId+" is already attached to network " + internetGateway.getVpc( ).getDisplayName() ) );
                }
                internetGateway.setVpc( vpc );
              } catch ( VpcMetadataNotFoundException e ) {
                throw Exceptions.toUndeclared( new ClientComputeException( "InvalidVpcID.NotFound", "Vpc not found '" + request.getVpcId() + "'" ) );
              } catch ( Exception e ) {
                throw Exceptions.toUndeclared( e );
              } else {
                throw Exceptions.toUndeclared( new ClientUnauthorizedComputeException( "Not authorized to attach internet gateway" ) );
              }
            }
          } );
      invalidate( vpcId );
    } catch ( VpcMetadataNotFoundException e ) {
      throw new ClientComputeException( "InvalidInternetGatewayID.NotFound", "Internet gateway ("+request.getInternetGatewayId()+") not found " );
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public AttachNetworkInterfaceResponseType attachNetworkInterface(AttachNetworkInterfaceType request) throws EucalyptusCloudException {
    AttachNetworkInterfaceResponseType reply = request.getReply( );
    return reply;
  }

  public AttachVpnGatewayResponseType attachVpnGateway(AttachVpnGatewayType request) throws EucalyptusCloudException {
    AttachVpnGatewayResponseType reply = request.getReply( );
    return reply;
  }

  public CreateCustomerGatewayResponseType createCustomerGateway(CreateCustomerGatewayType request) throws EucalyptusCloudException {
    CreateCustomerGatewayResponseType reply = request.getReply( );
    return reply;
  }

  public CreateDhcpOptionsResponseType createDhcpOptions( final CreateDhcpOptionsType request ) throws EucalyptusCloudException {
    final CreateDhcpOptionsResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup();
    final Supplier<DhcpOptionSet> allocator = new Supplier<DhcpOptionSet>( ) {
      @Override
      public DhcpOptionSet get( ) {
        try {
          final DhcpOptionSet dhcpOptionSet = DhcpOptionSet.create( ctx.getUserFullName(), Identifier.dopt.generate( ) );
          for ( final DhcpConfigurationItemType item : request.getDhcpConfigurationSet( ).getItem( ) ) {
            final List<String> values = item.values( );
            boolean validValue = false;
            out:
            switch( item.getKey( ) ) {
              case DhcpOptionSets.DHCP_OPTION_DOMAIN_NAME:
                validValue = values.size( ) == 1 && InternetDomainName.isValid( values.get( 0 ) );
                break;
              case DhcpOptionSets.DHCP_OPTION_DOMAIN_NAME_SERVERS:
                validValue = values.size( ) == 1 && "AmazonProvidedDNS".equals( values.get( 0 ) );
                if ( validValue ) break; // else fallthrough
              case DhcpOptionSets.DHCP_OPTION_NTP_SERVERS: // fallthrough
              case DhcpOptionSets.DHCP_OPTION_NETBIOS_NAME_SERVERS:
                for ( final String value : values ) {
                  validValue = InetAddresses.isInetAddress( value );
                  if (!validValue) break out;
                }
                break;
              case DhcpOptionSets.DHCP_OPTION_NETBIOS_NODE_TYPE:
                validValue = values.size( ) == 1 && Optional.fromNullable( Ints.tryParse( values.get( 0 ) ) )
                    .transform( Functions.forPredicate( Predicates.in( Lists.newArrayList( 1, 2, 4, 8 ) ) ) ).or( false );
                break;
              default:
                throw new ClientComputeException( "InvalidParameterValue", "Value ("+item.getKey()+") for parameter name is invalid. Unknown DHCP option" );
            }
            if ( !validValue || values.isEmpty( ) ) {
              throw new ClientComputeException( "InvalidParameterValue", "Value ("+ Joiner.on(',').join( values )+") for parameter value is invalid. Invalid DHCP option value." );
            }
            dhcpOptionSet.getDhcpOptions( ).add( DhcpOption.create( dhcpOptionSet, item.getKey( ), item.values( ) ) );
          }
          return dhcpOptionSets.save( dhcpOptionSet );
        } catch ( Exception ex ) {
          throw Exceptions.toUndeclared( ex );
        }
      }
    };
    reply.setDhcpOptions( allocate( allocator, DhcpOptionSet.class, DhcpOptionsType.class ) );
    return reply;
  }

  public CreateInternetGatewayResponseType createInternetGateway( final CreateInternetGatewayType request ) throws EucalyptusCloudException {
    final CreateInternetGatewayResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup();
    final Supplier<InternetGateway> allocator = new Supplier<InternetGateway>( ) {
      @Override
      public InternetGateway get( ) {
        try {
          return internetGateways.save( InternetGateway.create( ctx.getUserFullName( ), Identifier.igw.generate( ) ) );
        } catch ( Exception ex ) {
          throw new RuntimeException( ex );
        }
      }
    };
    reply.setInternetGateway( allocate( allocator, InternetGateway.class, InternetGatewayType.class ) );
    return reply;
  }

  public CreateNetworkAclResponseType createNetworkAcl( final CreateNetworkAclType request ) throws EucalyptusCloudException {
    final CreateNetworkAclResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final String vpcId = Identifier.vpc.normalize( request.getVpcId( ) );
    final Supplier<NetworkAcl> allocator = new Supplier<NetworkAcl>( ) {
      @Override
      public NetworkAcl get( ) {
        try {
          final Vpc vpc = vpcs.lookupByName( ctx.getUserFullName( ).asAccountFullName( ), vpcId, Functions.<Vpc>identity( ) );
          final long networkAclsForVpc = networkAcls.countByExample(
              NetworkAcl.exampleWithOwner( ctx.getUserFullName( ).asAccountFullName( ) ),
              Restrictions.eq( "vpc.displayName", vpcId ),
              Collections.singletonMap( "vpc", "vpc" ) );
          if ( networkAclsForVpc >= VpcConfiguration.getNetworkAclsPerVpc( ) ) {
            throw new ClientComputeException( "NetworkAclLimitExceeded", "Network ACL limit exceeded for " + vpc.getDisplayName( ) );
          }
          return networkAcls.save( NetworkAcl.create( ctx.getUserFullName( ), vpc, Identifier.acl.generate( ), false ) );
        } catch ( VpcMetadataNotFoundException ex ) {
          throw Exceptions.toUndeclared( new ClientComputeException( "InvalidVpcID.NotFound", "Vpc not found '" + request.getVpcId() + "'" ) );
        } catch ( Exception ex ) {
          throw new RuntimeException( ex );
        }
      }
    };
    reply.setNetworkAcl( allocate( allocator, NetworkAcl.class, NetworkAclType.class ) );
    return reply;
  }

  public CreateNetworkAclEntryResponseType createNetworkAclEntry(final CreateNetworkAclEntryType request) throws EucalyptusCloudException {
    final CreateNetworkAclEntryResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName();
    final String networkAclId = Identifier.acl.normalize( request.getNetworkAclId() );
    final String cidr = request.getCidrBlock( );
    final Optional<Cidr> cidrOptional = Cidr.parse( ).apply( cidr );
    if ( !cidrOptional.isPresent( ) ) {
      throw new ClientComputeException( "InvalidParameterValue", "Cidr invalid: " + cidr );
    }
    final Optional<Integer> protocolOptional = protocolNumber( request.getProtocol( ) );
    if ( !protocolOptional.isPresent( ) ) {
      throw new ClientComputeException( "InvalidParameterValue", "Protocol invalid: " + request.getProtocol( ) );
    }
    if ( !Range.closed( 1, 32766 ).apply( request.getRuleNumber( ) ) ) {
      throw new ClientComputeException( "InvalidParameterValue", "Rule number invalid: " + request.getRuleNumber( ) );
    }
    final Supplier<NetworkAclEntry> allocator = transactional( new Supplier<NetworkAclEntry>( ) {
      @Override
      public NetworkAclEntry get( ) {
        try {
          networkAcls.updateByExample(
              NetworkAcl.exampleWithName( accountFullName, networkAclId ),
              accountFullName,
              request.getNetworkAclId(),
              new Callback<NetworkAcl>( ) {
                @Override
                public void fire( final NetworkAcl networkAcl ) {
                  if ( RestrictedTypes.filterPrivileged( ).apply( networkAcl ) ) try {
                    final List<NetworkAclEntry> entries = networkAcl.getEntries( );
                    final Optional<NetworkAclEntry> existingEntry =
                        Iterables.tryFind( entries, entryPredicate( request.getEgress( ), request.getRuleNumber( ) ) );

                    if ( existingEntry.isPresent( ) ) {
                      throw new ClientComputeException(
                          "NetworkAclEntryAlreadyExists",
                          "Entry exists with rule number: " + request.getRuleNumber( ) );
                    }

                    if ( CollectionUtils.reduce( entries, 0, CollectionUtils.count( entryPredicate( request.getEgress( ), null ) ) )
                        >= VpcConfiguration.getRulesPerNetworkAcl( ) ) {
                      throw new ClientComputeException(
                          "NetworkAclEntryLimitExceeded",
                          "Network ACL entry limit exceeded for " + request.getNetworkAclId() );
                    }

                    final NetworkAclEntry entry;
                    switch ( protocolOptional.get( ) ) {
                      case 1:
                        entry = NetworkAclEntry.createIcmpEntry(
                            networkAcl,
                            request.getRuleNumber(),
                            Enums.valueOfFunction( NetworkAclEntry.RuleAction.class ).apply( request.getRuleAction() ),
                            request.getEgress(),
                            cidr,
                            request.getIcmpTypeCode().getCode(),
                            request.getIcmpTypeCode().getType() );
                        break;
                      case 6:
                      case 17:
                        entry = NetworkAclEntry.createTcpUdpEntry(
                            networkAcl,
                            request.getRuleNumber(),
                            protocolOptional.get(),
                            Enums.valueOfFunction( NetworkAclEntry.RuleAction.class ).apply( request.getRuleAction() ),
                            request.getEgress(),
                            cidr,
                            request.getPortRange().getFrom(),
                            request.getPortRange().getTo() );
                        break;
                      default:
                        entry = NetworkAclEntry.createEntry(
                            networkAcl,
                            request.getRuleNumber( ),
                            protocolOptional.get(),
                            Enums.valueOfFunction( NetworkAclEntry.RuleAction.class ).apply( request.getRuleAction( ) ),
                            request.getEgress( ),
                            cidr );
                    }

                    entries.add( entry );
                    networkAcl.updateTimeStamps( ); // ensure version of table increments also
                  } catch ( Exception e ) {
                    throw Exceptions.toUndeclared( e );
                  } else {
                    throw Exceptions.toUndeclared( new ClientUnauthorizedComputeException( "Not authorized to create network ACL entry" ) );
                  }
                }
              }
          );
          return null;
        } catch ( Exception ex ) {
          throw new RuntimeException( ex );
        }
      }
    } );

    try {
      allocator.get( );
      invalidate( networkAclId );
    } catch ( Exception e ) {
      throw handleException( e );
    }

    return reply;
  }

  public CreateNetworkInterfaceResponseType createNetworkInterface( final CreateNetworkInterfaceType request ) throws EucalyptusCloudException {
    final CreateNetworkInterfaceResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName();
    final String subnetId = Identifier.subnet.normalize( request.getSubnetId( ) );
    final String privateIp = request.getPrivateIpAddress( ) != null ?
        request.getPrivateIpAddress( ) :
        request.getPrivateIpAddressesSet( ) != null && !request.getPrivateIpAddressesSet( ).getItem( ).isEmpty( ) ?
            request.getPrivateIpAddressesSet( ).getItem( ).get( 0 ).getPrivateIpAddress( ) :
            null;
    final Supplier<NetworkInterface> allocator = new Supplier<NetworkInterface>( ) {
      @Override
      public NetworkInterface get( ) {
        try {
          final Subnet subnet = subnets.lookupByName( accountFullName, subnetId, Functions.<Subnet>identity() );
          final Vpc vpc = subnet.getVpc( );
          final Set<NetworkGroup> groups = request.getGroupSet( )==null ?
              Sets.newHashSet( securityGroups.lookupDefault( vpc.getDisplayName( ), Functions.<NetworkGroup>identity( ) ) ) :
              Sets.newHashSet( Iterables.transform(
                  request.getGroupSet( ).groupIds( ),
                  RestrictedTypes.resolver( NetworkGroup.class ) ) );
          if ( groups.size( ) > VpcConfiguration.getSecurityGroupsPerNetworkInterface( ) ) {
            throw new ClientComputeException( "SecurityGroupsPerInterfaceLimitExceeded", "Security group limit exceeded" );
          }
          final String identifier = Identifier.eni.generate();
          if ( privateIp != null ) {
            final Cidr cidr = Cidr.parse( subnet.getCidr( ) );
            if ( !cidr.contains( privateIp ) ) {
              throw new ClientComputeException( "InvalidParameterValue", "Address does not fall within the subnet's address range" );
            } else if ( !Iterables.contains( Iterables.skip( IPRange.fromCidr( cidr ), 3 ), PrivateAddresses.asInteger( privateIp ) ) ) {
              throw new ClientComputeException( "InvalidParameterValue", "Address is in subnet's reserved address range" );
            }
          }
          final String mac = NetworkInterfaceHelper.mac( identifier );
          final String ip = NetworkInterfaceHelper.allocate( vpc.getDisplayName( ), subnet.getDisplayName( ), identifier, mac, privateIp );
          return networkInterfaces.save( NetworkInterface.create(
              ctx.getUserFullName(),
              vpc,
              subnet,
              groups,
              identifier,
              mac,
              ip,
              vpc.getDnsHostnames( ) ?
                  VmInstances.dnsName( ip, DomainNames.internalSubdomain( ) ) :
                  null,
              firstNonNull( request.getDescription( ), "" ) ) );
        } catch ( VpcMetadataNotFoundException ex ) {
          throw Exceptions.toUndeclared( new ClientComputeException( "InvalidSubnetID.NotFound", "Subnet not found '" + request.getSubnetId() + "'" ) );
        } catch ( ResourceAllocationException ex ) {
          throw Exceptions.toUndeclared( new ClientComputeException( "InvalidParameterValue", ex.getMessage( ) ) );
        } catch ( Exception ex ) {
          final NoSuchMetadataException e = Exceptions.findCause(  ex, NoSuchMetadataException.class );
          if ( e != null ) {
            throw Exceptions.toUndeclared( new ClientComputeException( "InvalidSecurityGroupID.NotFound", e.getMessage( ) ) );
          }
          throw new RuntimeException( ex );
        }
      }
    };
    reply.setNetworkInterface( allocate( allocator, NetworkInterface.class, NetworkInterfaceType.class ) );
    return reply;
  }

  public CreateRouteResponseType createRoute( final CreateRouteType request ) throws EucalyptusCloudException {
    final CreateRouteResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName();
    final String gatewayId = Identifier.igw.normalize( request.getGatewayId() );
    final String routeTableId = Identifier.rtb.normalize( request.getRouteTableId() );
    final String destinationCidr = request.getDestinationCidrBlock( );
    final Optional<Cidr> destinationCidrOption = Cidr.parse( ).apply( destinationCidr );
    if ( !destinationCidrOption.isPresent( ) ) {
      throw new ClientComputeException( "InvalidParameterValue", "Cidr invalid: " + destinationCidr );
    }
    final Supplier<Route> allocator = transactional( new Supplier<Route>( ) {
      @Override
      public Route get( ) {
        try {
          final InternetGateway internetGateway =
              internetGateways.lookupByName( accountFullName, gatewayId, Functions.<InternetGateway>identity() );
          routeTables.updateByExample(
              RouteTable.exampleWithName( accountFullName, routeTableId ),
              accountFullName,
              request.getRouteTableId(),
              new Callback<RouteTable>() {
                @Override
                public void fire( final RouteTable routeTable ) {
                  try {
                    if ( RestrictedTypes.filterPrivileged( ).apply( routeTable ) ) {
                      final Optional<Route> existingRoute =
                          Iterables.tryFind( routeTable.getRoutes( ), CollectionUtils.propertyPredicate(
                              destinationCidr,
                              RouteTables.RouteFilterStringFunctions.DESTINATION_CIDR ) );

                      if ( existingRoute.isPresent( ) ) {
                        throw new ClientComputeException(
                            "RouteAlreadyExists",
                            "Route exists for cidr: " + destinationCidr );
                      }

                      if ( routeTable.getRoutes( ).size( ) >= VpcConfiguration.getRoutesPerTable( ) ) {
                        throw new ClientComputeException(
                            "RouteLimitExceeded",
                            "Route limit exceeded for " + request.getRouteTableId() );
                      }

                      routeTable.getRoutes().add(
                          Route.create( routeTable, Route.RouteOrigin.CreateRoute, destinationCidr, internetGateway )
                      );
                      routeTable.updateTimeStamps( ); // ensure version of table increments also
                    } else {
                      throw new ClientUnauthorizedComputeException( "Not authorized to create route" );
                    }
                  } catch ( Exception e ) {
                    throw Exceptions.toUndeclared( e );
                  }
                }
              } );
          return null;
        } catch ( Exception ex ) {
          throw new RuntimeException( ex );
        }
      }
    } );

    try {
      allocator.get( );
      invalidate( routeTableId );
    } catch ( Exception e ) {
      throw handleException( e );
    }

    return reply;
  }

  public CreateRouteTableResponseType createRouteTable( final CreateRouteTableType request ) throws EucalyptusCloudException {
    final CreateRouteTableResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final String vpcId = Identifier.vpc.normalize( request.getVpcId( ) );
    final Supplier<RouteTable> allocator = new Supplier<RouteTable>( ) {
      @Override
      public RouteTable get( ) {
        try {
          final Vpc vpc = vpcs.lookupByName( ctx.getUserFullName().asAccountFullName(), vpcId, Functions.<Vpc>identity() );

          final long routeTablesForVpc = routeTables.countByExample(
              RouteTable.exampleWithOwner( ctx.getUserFullName( ).asAccountFullName( ) ),
              Restrictions.eq( "vpc.displayName", vpcId ),
              Collections.singletonMap( "vpc", "vpc" ) );
          if ( routeTablesForVpc >= VpcConfiguration.getRouteTablesPerVpc( ) ) {
            throw new ClientComputeException( " RouteTableLimitExceeded", "Route table limit exceeded for " + vpc.getDisplayName( ) );
          }
          return routeTables.save( RouteTable.create( ctx.getUserFullName( ), vpc, Identifier.rtb.generate(), vpc.getCidr(), false ) );
        } catch ( VpcMetadataNotFoundException ex ) {
          throw Exceptions.toUndeclared( new ClientComputeException( "InvalidVpcID.NotFound", "Vpc not found '" + request.getVpcId() + "'" ) );
        } catch ( Exception ex ) {
          throw new RuntimeException( ex );
        }
      }
    };
    reply.setRouteTable( allocate( allocator, RouteTable.class, RouteTableType.class ) );
    return reply;
  }

  public CreateSubnetResponseType createSubnet( final CreateSubnetType request ) throws EucalyptusCloudException {
    final CreateSubnetResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName();
    final String vpcId = Identifier.vpc.normalize( request.getVpcId( ) );
    final Optional<String> availabilityZone = Iterables.tryFind(
            Clusters.getInstance( ).listValues( ),
            Predicates.and(
                request.getAvailabilityZone( ) == null ?
                    Predicates.<RestrictedType>alwaysTrue( ) :
                    CollectionUtils.propertyPredicate( request.getAvailabilityZone( ), CloudMetadatas.toDisplayName( ) ),
                RestrictedTypes.filterPrivilegedWithoutOwner( ) ) ).transform( CloudMetadatas.toDisplayName( ) );
    final Optional<Cidr> subnetCidr = Cidr.parse( ).apply( request.getCidrBlock( ) );
    if ( !subnetCidr.isPresent( ) ) {
      throw new ClientComputeException( "InvalidParameterValue", "Cidr invalid: " + request.getCidrBlock( ) );
    }
    if ( !availabilityZone.isPresent( ) ) {
      throw new ClientComputeException( "InvalidParameterValue", "Availability zone invalid: " + request.getAvailabilityZone( ) );
    }
    final Supplier<Subnet> allocator = new Supplier<Subnet>( ) {
      @Override
      public Subnet get( ) {
        try {
          final Vpc vpc =
              vpcs.lookupByName( accountFullName, vpcId, Functions.<Vpc>identity( ) );
          final Iterable<Subnet> subnetsInVpc = subnets.listByExample(
              Subnet.exampleWithOwner( accountFullName ),
              CollectionUtils.propertyPredicate( vpc.getDisplayName(), Subnets.FilterStringFunctions.VPC_ID ),
              Functions.<Subnet>identity( ) );
          if ( Iterables.size( subnetsInVpc ) >= VpcConfiguration.getSubnetsPerVpc( ) ) {
            throw new ClientComputeException( "SubnetLimitExceeded", "Subnet limit exceeded for " + vpc.getDisplayName( ) );
          }
          if ( !Cidr.parse( vpc.getCidr( ) ).contains( subnetCidr.get( ) ) ) {
            throw new ClientComputeException( "InvalidParameterValue", "Cidr not valid for vpc " + request.getCidrBlock( ) );
          }
          final Iterable<Cidr> existingCidrs = Iterables.transform(
              subnetsInVpc, Functions.compose( Cidr.parseUnsafe(), Subnets.FilterStringFunctions.CIDR ) );
          if ( Iterables.any( existingCidrs, subnetCidr.get( ).contains( ) ) ||
              Iterables.any( existingCidrs, subnetCidr.get( ).containedBy() ) ) {
            throw new ClientComputeException( "InvalidSubnet.Conflict", "Cidr conflict for " + request.getCidrBlock( ) );
          }
          final NetworkAcl networkAcl = networkAcls.lookupDefault( vpc.getDisplayName(), Functions.<NetworkAcl>identity() );
          return subnets.save( Subnet.create(
              ctx.getUserFullName( ),
              vpc,
              networkAcl,
              Identifier.subnet.generate( ),
              request.getCidrBlock( ),
              availabilityZone.get() ) );
        } catch ( VpcMetadataNotFoundException ex ) {
          throw Exceptions.toUndeclared( new ClientComputeException( "InvalidVpcID.NotFound", "Vpc not found '" + request.getVpcId() + "'" ) );
        } catch ( Exception ex ) {
          throw new RuntimeException( ex );
        }
      }
    };
    reply.setSubnet( allocate( allocator, Subnet.class, SubnetType.class ) );
    invalidate( reply.getSubnet( ).getSubnetId( ) );
    return reply;
  }

  public CreateVpcResponseType createVpc( final CreateVpcType request ) throws EucalyptusCloudException {
    final CreateVpcResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final UserFullName userFullName = ctx.getUserFullName();
    final boolean createDefault = ctx.isAdministrator( ) && request.getCidrBlock( ).matches( "[0-9]{12}" );
    if ( !Cidr.parse().apply( request.getCidrBlock( ) ).transform( Cidr.prefix( ) )
        .transform( Functions.forPredicate( Range.closed( 16, 28 ) ) ).or( createDefault ) ) {
      throw new ClientComputeException( "InvalidVpcRange", "Cidr range invalid: " + request.getCidrBlock( ) );
    }
    final Supplier<Vpc> allocator = new Supplier<Vpc>( ) {
      @Override
      public Vpc get( ) {
        try {
          final String vpcCidr;
          final AccountFullName vpcAccountFullName;
          final UserFullName vpcOwnerFullName;
          Vpc vpc = null;
          RouteTable routeTable = null;
          NetworkAcl networkAcl = null;
          if ( createDefault ) {
            final Account account = Accounts.lookupAccountById( request.getCidrBlock( ) );
            vpcCidr = Vpcs.DEFAULT_VPC_CIDR;
            vpcAccountFullName = AccountFullName.getInstance( account );
            vpcOwnerFullName =
                UserFullName.getInstance( account.lookupAdmin( ) );

            // check for existing vpc
            try {
              vpc = vpcs.lookupDefault( vpcAccountFullName, Functions.<Vpc>identity() );
              routeTable = routeTables.lookupMain( vpc.getDisplayName( ), Functions.<RouteTable>identity( ) );
              networkAcl = networkAcls.lookupDefault( vpc.getDisplayName( ), Functions.<NetworkAcl>identity( ) );
            } catch ( final VpcMetadataNotFoundException e ) {
              // so create it
            }
          } else {
            vpcCidr = request.getCidrBlock( );
            vpcAccountFullName = userFullName.asAccountFullName( );
            vpcOwnerFullName = userFullName;
          }
          if ( vpc == null ) {
            DhcpOptionSet options;
            try {
              options = dhcpOptionSets.lookupByExample(
                  DhcpOptionSet.exampleDefault( vpcAccountFullName ),
                  vpcAccountFullName,
                  "default",
                  Predicates.alwaysTrue(),
                  Functions.<DhcpOptionSet>identity() );
            } catch ( VpcMetadataNotFoundException e ) {
              options = dhcpOptionSets.save( DhcpOptionSet.createDefault( vpcOwnerFullName, Identifier.dopt.generate() ) );
            }
            vpc =
                vpcs.save( Vpc.create( vpcOwnerFullName, Identifier.vpc.generate(), options, vpcCidr, createDefault ) );
            routeTable =
                routeTables.save( RouteTable.create( vpcOwnerFullName, vpc, Identifier.rtb.generate(), vpc.getCidr(), true ) );
            networkAcl =
                networkAcls.save( NetworkAcl.create( vpcOwnerFullName, vpc, Identifier.acl.generate(), true ) );
            final NetworkGroup group = NetworkGroup.create(
                vpcOwnerFullName,
                vpc,
                ResourceIdentifiers.generateString( NetworkGroup.ID_PREFIX ),
                NetworkGroups.defaultNetworkName(),
                "default VPC security group" );
            final Collection<NetworkPeer> peers = Lists.newArrayList(
                NetworkPeer.create( group.getOwnerAccountNumber(), group.getName(), group.getGroupId() ) );
            group.getNetworkRules().addAll( Lists.newArrayList(
                NetworkRule.create( null/*protocol name*/, -1, null/*low port*/, null/*high port*/, peers, null/*cidrs*/ ),
                NetworkRule.createEgress( null/*protocol name*/, -1, null/*low port*/, null/*high port*/, null/*peers*/, Collections.singleton( "0.0.0.0/0" ) )
            ) );
            securityGroups.save( group );
          }
          if ( createDefault && routeTable != null && networkAcl != null ) {
            // ensure there is an internet gateway for the vpc and a route in place
            InternetGateway internetGateway;
            try {
              internetGateway =
                  internetGateways.lookupByVpc( vpcAccountFullName, vpc.getDisplayName(), Functions.<InternetGateway>identity( ) );
            } catch ( final VpcMetadataNotFoundException e ) {
              internetGateway = internetGateways.save( InternetGateway.create( vpcOwnerFullName, Identifier.igw.generate( ) ) );
              internetGateway.setVpc( vpc );
            }

            final Optional<Route> defaultRoute =
                Iterables.tryFind( routeTable.getRoutes( ), CollectionUtils.propertyPredicate(
                    "0.0.0.0/0",
                    RouteTables.RouteFilterStringFunctions.DESTINATION_CIDR ) );

            if ( !defaultRoute.isPresent( ) ) {
              routeTable.getRoutes( ).add( Route.create( routeTable, Route.RouteOrigin.CreateRouteTable, "0.0.0.0/0", internetGateway ) );
              routeTable.updateTimeStamps( ); // ensure version of table increments also
            }

            // ensure there is a default subnet in each availability zone
            final Set<String> cidrsInUse = Sets.newHashSet( );
            final Set<String> zonesWithoutSubnets = Sets.newTreeSet( );
            for ( final String zone : Iterables.transform( Clusters.getInstance( ).listValues( ), CloudMetadatas.toDisplayName( ) ) ) {
              try {
                cidrsInUse.add( subnets.lookupDefault( vpcAccountFullName, zone, Subnets.FilterStringFunctions.CIDR ) );
              } catch ( final VpcMetadataNotFoundException e ) {
                zonesWithoutSubnets.add( zone );
              }
            }
            final List<String> subnetCidrs = Lists.newArrayList( Iterables.transform(
                Cidr.parseUnsafe( ).apply( Vpcs.DEFAULT_VPC_CIDR ).split( 16 ),
                Functions.toStringFunction( ) ) );
            subnetCidrs.removeAll( cidrsInUse );
            for ( final String zone : zonesWithoutSubnets ) {
              final Subnet subnet = subnets.save( Subnet.create(
                  vpcOwnerFullName,
                  vpc,
                  networkAcl,
                  Identifier.subnet.generate( ),
                  subnetCidrs.remove( 0 ),
                  zone ) );
              subnet.setDefaultForAz( true );
              subnet.setMapPublicIpOnLaunch( true );
            }
          }
          return vpc;
        } catch ( Exception ex ) {
          throw new RuntimeException( ex );
        }
      }
    };
    reply.setVpc( allocate( allocator, Vpc.class, VpcType.class ) );
    invalidate( reply.getVpc( ).getVpcId( ) );
    return reply;
  }

  public CreateVpcPeeringConnectionResponseType createVpcPeeringConnection(CreateVpcPeeringConnectionType request) throws EucalyptusCloudException {
    CreateVpcPeeringConnectionResponseType reply = request.getReply( );
    return reply;
  }

  public CreateVpnConnectionResponseType createVpnConnection(CreateVpnConnectionType request) throws EucalyptusCloudException {
    CreateVpnConnectionResponseType reply = request.getReply( );
    return reply;
  }

  public CreateVpnConnectionRouteResponseType createVpnConnectionRoute(CreateVpnConnectionRouteType request) throws EucalyptusCloudException {
    CreateVpnConnectionRouteResponseType reply = request.getReply( );
    return reply;
  }

  public CreateVpnGatewayResponseType createVpnGateway(CreateVpnGatewayType request) throws EucalyptusCloudException {
    CreateVpnGatewayResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteCustomerGatewayResponseType deleteCustomerGateway(DeleteCustomerGatewayType request) throws EucalyptusCloudException {
    DeleteCustomerGatewayResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteDhcpOptionsResponseType deleteDhcpOptions( final DeleteDhcpOptionsType request ) throws EucalyptusCloudException {
    final DeleteDhcpOptionsResponseType reply = request.getReply( );
    delete( Identifier.dopt, request.getDhcpOptionsId(), new Function<Pair<Optional<AccountFullName>,String>,DhcpOptionSet>( ) {
      @Override
      public DhcpOptionSet apply( final Pair<Optional<AccountFullName>,String> accountAndId ) {
        try {
          final DhcpOptionSet dhcpOptionSet = dhcpOptionSets.lookupByName(
              accountAndId.getLeft( ).orNull( ),
              accountAndId.getRight( ),
              Functions.<DhcpOptionSet>identity( ) );
          if ( RestrictedTypes.filterPrivileged( ).apply( dhcpOptionSet ) ) {
            dhcpOptionSets.delete( dhcpOptionSet );
          } else {
            throw Exceptions.toUndeclared( new ClientUnauthorizedComputeException( "Not authorized to delete DHCP options" ) );
          }
          return null;
        } catch ( Exception ex ) {
          throw new RuntimeException( ex );
        }
      }
    } );
    return reply;
  }

  public DeleteInternetGatewayResponseType deleteInternetGateway( final DeleteInternetGatewayType request ) throws EucalyptusCloudException {
    final DeleteInternetGatewayResponseType reply = request.getReply( );
    delete( Identifier.igw, request.getInternetGatewayId( ), new Function<Pair<Optional<AccountFullName>,String>,InternetGateway>( ) {
      @Override
      public InternetGateway apply( final Pair<Optional<AccountFullName>, String> accountAndId ) {
        try {
          final InternetGateway internetGateway = internetGateways.lookupByName(
              accountAndId.getLeft( ).orNull( ),
              accountAndId.getRight( ),
              Functions.<InternetGateway>identity( ) );
          if ( RestrictedTypes.filterPrivileged( ).apply( internetGateway ) ) {
            internetGateways.delete( internetGateway );
          } else {
            throw Exceptions.toUndeclared( new ClientUnauthorizedComputeException( "Not authorized to delete internet gateway" ) );
          }
        } catch ( VpcMetadataNotFoundException e ) {
          // so nothing to delete, move along
        } catch ( Exception ex ) {
          throw new RuntimeException( ex );
        }
        return null;
      }
    } );
    return reply;
  }

  public DeleteNetworkAclResponseType deleteNetworkAcl( final DeleteNetworkAclType request ) throws EucalyptusCloudException {
    final DeleteNetworkAclResponseType reply = request.getReply( );
    delete( Identifier.acl, request.getNetworkAclId( ), new Function<Pair<Optional<AccountFullName>,String>,NetworkAcl>( ) {
      @Override
      public NetworkAcl apply( final Pair<Optional<AccountFullName>, String> accountAndId ) {
        try {
          final NetworkAcl networkAcl = networkAcls.lookupByName(
              accountAndId.getLeft( ).orNull( ),
              accountAndId.getRight( ),
              Functions.<NetworkAcl>identity( ) );
          if ( RestrictedTypes.filterPrivileged( ).apply( networkAcl ) ) {
            if ( networkAcl.getDefaultForVpc( ) ) {
              throw new ClientComputeException(
                  "InvalidParameterValue",
                  "Cannot delete default network ACL " + accountAndId.getRight( ) );
            }
            networkAcls.delete( networkAcl );
          } else {
            throw new ClientUnauthorizedComputeException( "Not authorized to delete network ACL" );
          }
          return null;
        } catch ( Exception ex ) {
          throw Exceptions.toUndeclared( ex );
        }
      }
    } );
    return reply;
  }

  public DeleteNetworkAclEntryResponseType deleteNetworkAclEntry(final DeleteNetworkAclEntryType request) throws EucalyptusCloudException {
    final DeleteNetworkAclEntryResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.isAdministrator( ) ? null : ctx.getUserFullName( ).asAccountFullName( );
    final String networkAclId = Identifier.acl.normalize( request.getNetworkAclId() );
    try {
      networkAcls.updateByExample(
          NetworkAcl.exampleWithName( accountFullName, networkAclId ),
          accountFullName,
          request.getNetworkAclId(),
          new Callback<NetworkAcl>() {
            @Override
            public void fire( final NetworkAcl networkAcl ) {
              try {
                if ( RestrictedTypes.filterPrivileged( ).apply( networkAcl ) ) {
                  final Optional<NetworkAclEntry> entry = Iterables.tryFind(
                      networkAcl.getEntries( ),
                      entryPredicate( request.getEgress( ), request.getRuleNumber( ) ) );
                  if ( entry.isPresent( ) ) {
                    networkAcl.getEntries( ).remove( entry.get( ) );
                    networkAcl.updateTimeStamps( ); // ensure version of table increments also
                  } else {
                    throw new ClientComputeException(
                        "InvalidNetworkAclEntry.NotFound",
                        "Entry not found for number: " + request.getRuleNumber( ) );
                  }
                } else {
                  throw new ClientUnauthorizedComputeException( "Not authorized to delete network ACL entry" );
                }
              } catch ( Exception e ) {
                throw Exceptions.toUndeclared( e );
              }
            }
          }) ;
      invalidate( networkAclId );
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public DeleteNetworkInterfaceResponseType deleteNetworkInterface( final DeleteNetworkInterfaceType request ) throws EucalyptusCloudException {
    final DeleteNetworkInterfaceResponseType reply = request.getReply( );
    delete( Identifier.eni, request.getNetworkInterfaceId( ), new Function<Pair<Optional<AccountFullName>,String>,NetworkInterface>( ) {
      @Override
      public NetworkInterface apply( final Pair<Optional<AccountFullName>, String> accountAndId ) {
        try {
          final NetworkInterface networkInterface = networkInterfaces.lookupByName(
              accountAndId.getLeft( ).orNull( ),
              accountAndId.getRight( ),
              Functions.<NetworkInterface>identity( ) );
          if ( RestrictedTypes.filterPrivileged( ).apply( networkInterface ) ) {
            if ( networkInterface.isAttached( ) ) {
              throw new ClientComputeException( "" +
                  "InvalidNetworkInterface.InUse",
                  "The network interface is in use '"+request.getNetworkInterfaceId()+"'" );
            }
            NetworkInterfaceHelper.release( networkInterface );
            networkInterfaces.delete( networkInterface );
          } else {
            throw Exceptions.toUndeclared( new ClientUnauthorizedComputeException( "Not authorized to delete network interface" ) );
          }
          return null;
        } catch ( Exception ex ) {
          throw new RuntimeException( ex );
        }
      }
    } );
    return reply;
  }

  public DeleteRouteResponseType deleteRoute( final DeleteRouteType request ) throws EucalyptusCloudException {
    final DeleteRouteResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.isAdministrator( ) ? null : ctx.getUserFullName( ).asAccountFullName( );
    final String routeTableId = Identifier.rtb.normalize( request.getRouteTableId( ) );
    try {
      routeTables.updateByExample(
          RouteTable.exampleWithName( accountFullName, routeTableId ),
          accountFullName,
          request.getRouteTableId( ),
          new Callback<RouteTable>() {
            @Override
            public void fire( final RouteTable routeTable ) {
              try {
                if ( RestrictedTypes.filterPrivileged( ).apply( routeTable ) ) {
                  final Optional<Route> route = Iterables.tryFind(
                      routeTable.getRoutes( ),
                      CollectionUtils.propertyPredicate(
                          request.getDestinationCidrBlock( ),
                          RouteTables.RouteFilterStringFunctions.DESTINATION_CIDR ) );
                  if ( route.isPresent( ) ) {
                    routeTable.getRoutes( ).remove( route.get( ) );
                    routeTable.updateTimeStamps( ); // ensure version of table increments also
                  } else {
                    throw new ClientComputeException(
                        "InvalidRoute.NotFound",
                        "Route not found for cidr: " + request.getDestinationCidrBlock( ) );
                  }
                } else {
                  throw Exceptions.toUndeclared( new ClientUnauthorizedComputeException( "Not authorized to delete route" ) );
                }
              } catch ( Exception e ) {
                throw Exceptions.toUndeclared( e );
              }
            }
          }) ;
      invalidate( routeTableId );
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public DeleteRouteTableResponseType deleteRouteTable( final DeleteRouteTableType request ) throws EucalyptusCloudException {
    final DeleteRouteTableResponseType reply = request.getReply( );
    delete( Identifier.rtb, request.getRouteTableId( ), new Function<Pair<Optional<AccountFullName>,String>,RouteTable>( ) {
      @Override
      public RouteTable apply( final Pair<Optional<AccountFullName>, String> accountAndId ) {
        try {
          final RouteTable routeTable = routeTables.lookupByName(
              accountAndId.getLeft( ).orNull( ),
              accountAndId.getRight( ),
              Functions.<RouteTable>identity( ) );
          if ( RestrictedTypes.filterPrivileged( ).apply( routeTable ) ) {
            if ( routeTable.getMain( ) ) {
              throw new ClientComputeException(
                  "InvalidParameterValue",
                  "Cannot delete main route table " + accountAndId.getRight( ) );
            }
            routeTables.delete( routeTable );
          } else {
            throw new ClientUnauthorizedComputeException( "Not authorized to delete route table" );
          }
          return null;
        } catch ( Exception ex ) {
          throw Exceptions.toUndeclared( ex );
        }
      }
    } );
    return reply;
  }

  public DeleteSubnetResponseType deleteSubnet( final DeleteSubnetType request ) throws EucalyptusCloudException {
    final DeleteSubnetResponseType reply = request.getReply( );
    delete( Identifier.subnet, request.getSubnetId( ), new Function<Pair<Optional<AccountFullName>,String>,Subnet>( ) {
      @Override
      public Subnet apply( final Pair<Optional<AccountFullName>, String> accountAndId ) {
        try {
          final Subnet subnet = subnets.lookupByName(
              accountAndId.getLeft( ).orNull( ),
              accountAndId.getRight( ),
              Functions.<Subnet>identity( ) );
          if ( RestrictedTypes.filterPrivileged( ).apply( subnet ) ) {
            subnets.delete( subnet );
          } else {
            throw Exceptions.toUndeclared( new ClientUnauthorizedComputeException( "Not authorized to delete subnet" ) );
          }
          return null;
        } catch ( Exception ex ) {
          throw new RuntimeException( ex );
        }
      }
    } );
    return reply;
  }

  public DeleteVpcResponseType deleteVpc( final DeleteVpcType request ) throws EucalyptusCloudException {
    final DeleteVpcResponseType reply = request.getReply( );
    delete( Identifier.vpc, request.getVpcId( ), new Function<Pair<Optional<AccountFullName>,String>,Vpc>( ) {
      @Override
      public Vpc apply( final Pair<Optional<AccountFullName>, String> accountAndId ) {
        try {
          final Vpc vpc = vpcs.lookupByName(
              accountAndId.getLeft( ).orNull( ),
              accountAndId.getRight( ),
              Functions.<Vpc>identity( ) );
          if ( RestrictedTypes.filterPrivileged( ).apply( vpc ) ) {
            if ( Boolean.TRUE.equals( vpc.getDefaultVpc( ) ) &&  Contexts.lookup( ).isAdministrator( ) ) {
              final List<Subnet> defaultSubnets = subnets.listByExample(
                  Subnet.exampleDefault( AccountFullName.getInstance( vpc.getOwnerAccountNumber( ) ), null ),
                  Predicates.alwaysTrue( ),
                  Functions.<Subnet>identity( ) );
              for ( final Subnet subnet : defaultSubnets ) {
                subnets.delete( subnet );
              }
              try {
                final InternetGateway internetGateway =
                    internetGateways.lookupByVpc( null, vpc.getDisplayName( ), Functions.<InternetGateway>identity( ) );
                internetGateways.delete( internetGateway );
              } catch ( VpcMetadataNotFoundException e ) { /* so no need to delete */ }
            }
            try {
              networkAcls.delete( networkAcls.lookupDefault( vpc.getDisplayName(), Functions.<NetworkAcl>identity() ) );
            } catch ( VpcMetadataNotFoundException e ) { /* so no need to delete */ }
            try {
              routeTables.delete( routeTables.lookupMain( vpc.getDisplayName(), Functions.<RouteTable>identity() ) );
            } catch ( VpcMetadataNotFoundException e ) { /* so no need to delete */ }
            try {
              securityGroups.delete( securityGroups.lookupDefault( vpc.getDisplayName(), Functions.<NetworkGroup>identity() ) );
            } catch ( VpcMetadataNotFoundException e ) { /* so no need to delete */ }
            vpcs.delete( vpc );
          } else {
            throw Exceptions.toUndeclared( new ClientUnauthorizedComputeException( "Not authorized to delete vpc" ) );
          }
          return null;
        } catch ( Exception ex ) {
          throw new RuntimeException( ex );
        }
      }
    } );
    return reply;
  }

  public DeleteVpcPeeringConnectionResponseType deleteVpcPeeringConnection(DeleteVpcPeeringConnectionType request) throws EucalyptusCloudException {
    DeleteVpcPeeringConnectionResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteVpnConnectionResponseType deleteVpnConnection(DeleteVpnConnectionType request) throws EucalyptusCloudException {
    DeleteVpnConnectionResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteVpnConnectionRouteResponseType deleteVpnConnectionRoute(DeleteVpnConnectionRouteType request) throws EucalyptusCloudException {
    DeleteVpnConnectionRouteResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteVpnGatewayResponseType deleteVpnGateway(DeleteVpnGatewayType request) throws EucalyptusCloudException {
    DeleteVpnGatewayResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeAccountAttributesResponseType describeAccountAttributes(final DescribeAccountAttributesType request) throws EucalyptusCloudException {
    final DescribeAccountAttributesResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    final List<String> platforms = Lists.newArrayList( );
    final Set<NetworkingFeature> features = Networking.getInstance( ).describeFeatures( );
    if ( features.contains( NetworkingFeature.Classic ) ) platforms.add( "EC2" );
    if ( features.contains( NetworkingFeature.Vpc ) ) platforms.add( "VPC" );
    String vpcId = "none";
    try {
      vpcId = vpcs.lookupDefault( accountFullName, CloudMetadatas.toDisplayName( ) );
    } catch ( VpcMetadataNotFoundException e) {
      // no default vpc
    } catch ( Exception e ) {
      throw handleException( e );
    }
    final Map<String,List<String>> attributes = ImmutableMap.of(
        "supported-platforms", platforms,
        "default-vpc", Lists.newArrayList( vpcId )
    );
    final Set<String> requestedAttributes = Sets.newHashSet( request.attributeNames( ) );
    for ( final Map.Entry<String,List<String>> attributeEntry : attributes.entrySet( ) ) {
      if ( requestedAttributes.isEmpty( ) || requestedAttributes.contains( attributeEntry.getKey( ) ) ) {
        reply.getAccountAttributeSet().getItem().add(
            new AccountAttributeSetItemType( attributeEntry.getKey( ), attributeEntry.getValue( ) ) );
      }
    }
    return reply;
  }

  public DescribeCustomerGatewaysResponseType describeCustomerGateways(DescribeCustomerGatewaysType request) throws EucalyptusCloudException {
    DescribeCustomerGatewaysResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeDhcpOptionsResponseType describeDhcpOptions( final DescribeDhcpOptionsType request ) throws EucalyptusCloudException {
    final DescribeDhcpOptionsResponseType reply = request.getReply( );
    describe(
        Identifier.dopt,
        request.dhcpOptionsIds( ),
        request.getFilterSet( ),
        DhcpOptionSet.class,
        DhcpOptionsType.class,
        reply.getDhcpOptionsSet( ).getItem( ),
        DhcpOptionsType.id( ),
        dhcpOptionSets );
    return reply;
  }

  public DescribeInternetGatewaysResponseType describeInternetGateways( final DescribeInternetGatewaysType request ) throws EucalyptusCloudException {
    final DescribeInternetGatewaysResponseType reply = request.getReply( );
    describe(
        Identifier.igw,
        request.internetGatewayIds( ),
        request.getFilterSet( ),
        InternetGateway.class,
        InternetGatewayType.class,
        reply.getInternetGatewaySet( ).getItem( ),
        InternetGatewayType.id( ),
        internetGateways );
    return reply;
  }

  public DescribeNetworkAclsResponseType describeNetworkAcls( final DescribeNetworkAclsType request ) throws EucalyptusCloudException {
    final DescribeNetworkAclsResponseType reply = request.getReply( );
    describe(
        Identifier.acl,
        request.networkAclIds( ),
        request.getFilterSet( ),
        NetworkAcl.class,
        NetworkAclType.class,
        reply.getNetworkAclSet( ).getItem( ),
        NetworkAclType.id( ),
        networkAcls );
    return reply;
  }

  public DescribeNetworkInterfaceAttributeResponseType describeNetworkInterfaceAttribute(final DescribeNetworkInterfaceAttributeType request) throws EucalyptusCloudException {
    final DescribeNetworkInterfaceAttributeResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    try {
      final NetworkInterface networkInterface =
          networkInterfaces.lookupByName( accountFullName, Identifier.eni.normalize( request.getNetworkInterfaceId() ), new Function<NetworkInterface,NetworkInterface>( ){
            @Override
            public NetworkInterface apply( final NetworkInterface networkInterface ) {
              Entities.initialize( networkInterface.getNetworkGroups( ) );
              return networkInterface;
            }
          } );
      if ( RestrictedTypes.filterPrivileged( ).apply( networkInterface ) ) {
        switch ( request.getAttribute() ) {
          case "attachment":
            if ( networkInterface.isAttached( ) )
            reply.setAttachment( TypeMappers.transform( networkInterface.getAttachment( ), NetworkInterfaceAttachmentType.class ) );
            break;
          case "description":
            reply.setDescription( new NullableAttributeValueType( ) );
            reply.getDescription().setValue( networkInterface.getDescription( ) );
            break;
          case "groupSet":
            reply.setGroupSet( new GroupSetType( Collections2.transform(
                networkInterface.getNetworkGroups( ),
                TypeMappers.lookup( NetworkGroup.class, GroupItemType.class ) ) ) );
            break;
          case "sourceDestCheck":
            reply.setSourceDestCheck( new AttributeBooleanValueType() );
            reply.getSourceDestCheck().setValue( networkInterface.getSourceDestCheck( ) );
            break;
          default:
            throw new ClientComputeException( "InvalidParameterValue", "Value ("+request.getAttribute( )+") for parameter attribute is invalid. Unknown network interface attribute"  );
        }
      }
    } catch ( final Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public DescribeNetworkInterfacesResponseType describeNetworkInterfaces( final DescribeNetworkInterfacesType request ) throws EucalyptusCloudException {
    final DescribeNetworkInterfacesResponseType reply = request.getReply( );
    describe(
        Identifier.eni,
        request.networkInterfaceIds( ),
        request.getFilterSet( ),
        NetworkInterface.class,
        NetworkInterfaceType.class,
        reply.getNetworkInterfaceSet( ).getItem( ),
        NetworkInterfaceType.id( ),
        networkInterfaces );
    return reply;
  }

  public DescribeRouteTablesResponseType describeRouteTables( final DescribeRouteTablesType request ) throws EucalyptusCloudException {
    final DescribeRouteTablesResponseType reply = request.getReply( );
    describe(
        Identifier.rtb,
        request.routeTableIds( ),
        request.getFilterSet( ),
        RouteTable.class,
        RouteTableType.class,
        reply.getRouteTableSet( ).getItem( ),
        RouteTableType.id( ),
        routeTables );
    return reply;
  }

  public DescribeSubnetsResponseType describeSubnets( final DescribeSubnetsType request ) throws EucalyptusCloudException {
    final DescribeSubnetsResponseType reply = request.getReply( );
    describe(
        Identifier.subnet,
        request.subnetIds( ),
        request.getFilterSet( ),
        Subnet.class,
        SubnetType.class,
        reply.getSubnetSet( ).getItem( ),
        SubnetType.id( ),
        subnets );
    return reply;
  }

  public DescribeVpcAttributeResponseType describeVpcAttribute(final DescribeVpcAttributeType request) throws EucalyptusCloudException {
    final DescribeVpcAttributeResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    try {
      final Vpc vpc =
          vpcs.lookupByName( accountFullName, Identifier.vpc.normalize( request.getVpcId( ) ), Functions.<Vpc>identity( ) );
      if ( RestrictedTypes.filterPrivileged( ).apply( vpc ) ) {
        reply.setVpcId( vpc.getDisplayName( ) );
        switch ( request.getAttribute( ) ) {
          case "enableDnsSupport":
            reply.setEnableDnsSupport( new AttributeBooleanValueType( ) );
            reply.getEnableDnsSupport( ).setValue( vpc.getDnsEnabled( ) );
            break;
          case "enableDnsHostnames":
            reply.setEnableDnsHostnames( new AttributeBooleanValueType( ) );
            reply.getEnableDnsHostnames( ).setValue( vpc.getDnsHostnames( ) );
            break;
          default:
            throw new ClientComputeException( "InvalidParameterValue", "Value ("+request.getAttribute( )+") for parameter attribute is invalid. Unknown vpc attribute"  );
        }
      }
    } catch ( final Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public DescribeVpcPeeringConnectionsResponseType describeVpcPeeringConnections(DescribeVpcPeeringConnectionsType request) throws EucalyptusCloudException {
    DescribeVpcPeeringConnectionsResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeVpcsResponseType describeVpcs( final DescribeVpcsType request ) throws EucalyptusCloudException {
    final DescribeVpcsResponseType reply = request.getReply( );
    describe(
        Identifier.vpc,
        request.vpcIds( ),
        request.getFilterSet( ),
        Vpc.class,
        VpcType.class,
        reply.getVpcSet( ).getItem( ),
        VpcType.id( ),
        vpcs );
    return reply;
  }

  public DescribeVpnConnectionsResponseType describeVpnConnections(DescribeVpnConnectionsType request) throws EucalyptusCloudException {
    DescribeVpnConnectionsResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeVpnGatewaysResponseType describeVpnGateways(DescribeVpnGatewaysType request) throws EucalyptusCloudException {
    DescribeVpnGatewaysResponseType reply = request.getReply( );
    return reply;
  }

  public DetachInternetGatewayResponseType detachInternetGateway(final DetachInternetGatewayType request) throws EucalyptusCloudException {
    final DetachInternetGatewayResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    final String gatewayId = Identifier.igw.normalize( request.getInternetGatewayId( ) );
    final String vpcId = Identifier.vpc.normalize( request.getVpcId( ) );
    try {
      internetGateways.updateByExample(
          InternetGateway.exampleWithName( accountFullName, gatewayId ),
          accountFullName,
          request.getInternetGatewayId(),
          new Callback<InternetGateway>() {
            @Override
            public void fire( final InternetGateway internetGateway ) {
              if ( RestrictedTypes.filterPrivileged( ).apply( internetGateway ) ) try {
                final Vpc vpc = vpcs.lookupByName( accountFullName, vpcId, Functions.<Vpc>identity( ) );
                if ( internetGateway.getVpc( ) == null || !vpc.getDisplayName( ).equals( internetGateway.getVpc( ).getDisplayName( ) ) ) {
                  throw Exceptions.toUndeclared( new ClientComputeException( "Gateway.NotAttached",
                      "resource "+gatewayId+" is not attached to network " + vpcId ) );
                }
                internetGateway.setVpc( null );
              } catch ( VpcMetadataNotFoundException e ) {
                throw Exceptions.toUndeclared( new ClientComputeException( "InvalidVpcID.NotFound", "Vpc not found '" + request.getVpcId() + "'" ) );
              } catch ( Exception e ) {
                throw Exceptions.toUndeclared( e );
              } else {
                throw Exceptions.toUndeclared( new ClientUnauthorizedComputeException( "Not authorized to detach internet gateway" ) );
              }
            }
          } );
      invalidate( vpcId );
    } catch ( VpcMetadataNotFoundException e ) {
      throw new ClientComputeException( "InvalidInternetGatewayID.NotFound", "Internet gateway ("+request.getInternetGatewayId()+") not found " );
    } catch ( Exception e ) {
      throw handleException( e );
    }

    return reply;
  }

  public DetachNetworkInterfaceResponseType detachNetworkInterface(DetachNetworkInterfaceType request) throws EucalyptusCloudException {
    DetachNetworkInterfaceResponseType reply = request.getReply( );
    return reply;
  }

  public DetachVpnGatewayResponseType detachVpnGateway(DetachVpnGatewayType request) throws EucalyptusCloudException {
    DetachVpnGatewayResponseType reply = request.getReply( );
    return reply;
  }

  public DisableVgwRoutePropagationResponseType disableVgwRoutePropagation(DisableVgwRoutePropagationType request) throws EucalyptusCloudException {
    DisableVgwRoutePropagationResponseType reply = request.getReply( );
    return reply;
  }

  public DisassociateRouteTableResponseType disassociateRouteTable(final DisassociateRouteTableType request) throws EucalyptusCloudException {
    final DisassociateRouteTableResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    final String associationId = Identifier.rtbassoc.normalize( request.getAssociationId() );
    try {
      final String subnetId = routeTables.updateByAssociationId(
          associationId,
          accountFullName,
          new Function<RouteTable,String>() {
            @Override
            public String apply( final RouteTable routeTable ) {
              if ( RestrictedTypes.filterPrivileged( ).apply( routeTable ) ) try {
                final RouteTableAssociation association = Iterables.find(
                    routeTable.getRouteTableAssociations( ),
                    CollectionUtils.propertyPredicate( associationId, RouteTables.AssociationFilterStringFunctions.ASSOCIATION_ID ) );
                if ( association.getMain( ) ) {
                  throw new ClientComputeException( "InvalidParameterValue", "Cannot disassociate the main route table association "+request.getAssociationId( ) );
                }
                routeTable.disassociate( associationId );
                return association.getSubnetId( );
              } catch ( Exception e ) {
                throw Exceptions.toUndeclared( e );
              }
              return null;
            }
          } );
      if ( subnetId != null ) invalidate( subnetId );
    } catch ( VpcMetadataNotFoundException e ) {
      throw new ClientComputeException( "InvalidAssociationID.NotFound", "Route table association ("+request.getAssociationId( )+") not found " );
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public EnableVgwRoutePropagationResponseType enableVgwRoutePropagation(EnableVgwRoutePropagationType request) throws EucalyptusCloudException {
    EnableVgwRoutePropagationResponseType reply = request.getReply( );
    return reply;
  }

  public ModifyNetworkInterfaceAttributeResponseType modifyNetworkInterfaceAttribute(final ModifyNetworkInterfaceAttributeType request) throws EucalyptusCloudException {
    final ModifyNetworkInterfaceAttributeResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    final String eniId = Identifier.eni.normalize( request.getNetworkInterfaceId( ) );
    try {
      final AtomicBoolean invalidate = new AtomicBoolean( false );
      networkInterfaces.updateByExample(
          NetworkInterface.exampleWithName( accountFullName, eniId ),
          accountFullName,
          request.getNetworkInterfaceId(),
          new Callback<NetworkInterface>() {
            @Override
            public void fire( final NetworkInterface networkInterface ) {
              if ( RestrictedTypes.filterPrivileged( ).apply( networkInterface ) ) {
                if ( request.getAttachment( ) != null ) {
                  if ( networkInterface.isAttached( ) &&
                      networkInterface.getAttachment( ).getAttachmentId( ).equals( request.getAttachment().getAttachmentId( ) ) ) {
                    networkInterface.getAttachment( ).setDeleteOnTerminate( request.getAttachment( ).getDeleteOnTermination( ) );
                  }
                } else if ( request.getDescription( ) != null ) {
                  networkInterface.setDescription( request.getDescription( ).getValue( ) );
                } else if ( request.getGroupSet( ) != null && !request.getGroupSet( ).getItem( ).isEmpty( ) ) {
                  try {
                    networkInterface.setNetworkGroups( Sets.newHashSet( Iterables.transform(
                        request.getGroupSet( ).groupIds( ),
                        RestrictedTypes.resolver( NetworkGroup.class ) ) ) );
                    if ( networkInterface.getNetworkGroups( ).size( ) > VpcConfiguration.getSecurityGroupsPerNetworkInterface( ) ) {
                      throw Exceptions.toUndeclared( new ClientComputeException( "SecurityGroupsPerInterfaceLimitExceeded", "Security group limit exceeded" ) );
                    }
                    if ( networkInterface.isAttached( ) && networkInterface.getAttachment( ).getDeviceIndex( ) == 0 ) {
                      final Set<NetworkGroup> instanceGroups = networkInterface.getAttachment( ).getInstance( ).getNetworkGroups( );
                      instanceGroups.clear( );
                      instanceGroups.addAll( networkInterface.getNetworkGroups( ) );
                    }
                    invalidate.set( true );
                  } catch ( RuntimeException e ) {
                    final NoSuchMetadataException nsme = Exceptions.findCause(  e, NoSuchMetadataException.class );
                    if ( nsme != null ) {
                      throw Exceptions.toUndeclared( new ClientComputeException( "InvalidSecurityGroupID.NotFound", nsme.getMessage( ) ) );
                    }
                    throw e;
                  }
                } else if ( request.getSourceDestCheck( ) != null ) {
                  networkInterface.setSourceDestCheck( request.getSourceDestCheck( ).getValue( ) );
                  invalidate.set( true );
                } else {
                  throw Exceptions.toUndeclared( new ClientComputeException( "MissingParameter", "Missing attribute value" ) );
                }
              } else {
                throw Exceptions.toUndeclared( new ClientUnauthorizedComputeException( "Not authorized to modify network interface attribute" ) );
              }
            }
          } );
      if ( invalidate.get( ) ) invalidate( eniId );
    } catch ( final Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public ModifySubnetAttributeResponseType modifySubnetAttribute( final ModifySubnetAttributeType request ) throws EucalyptusCloudException {
    final ModifySubnetAttributeResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    try {
      subnets.updateByExample(
          Subnet.exampleWithName( ctx.isAdministrator( ) ? null : accountFullName, Identifier.subnet.normalize( request.getSubnetId( ) ) ),
          accountFullName,
          request.getSubnetId( ),
          new Callback<Subnet>() {
            @Override
            public void fire( final Subnet subnet ) {
              if ( RestrictedTypes.filterPrivileged( ).apply( subnet ) ) {
                final AttributeBooleanValueType value = request.getMapPublicIpOnLaunch( );
                if ( value != null && value.getValue( ) != null ) {
                  subnet.setMapPublicIpOnLaunch( value.getValue( ) );
                }
              } else {
                throw Exceptions.toUndeclared( new ClientUnauthorizedComputeException( "Not authorized to modify subnet attribute" ) );
              }
            }
          } );
    } catch( Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public ModifyVpcAttributeResponseType modifyVpcAttribute(final ModifyVpcAttributeType request) throws EucalyptusCloudException {
    final ModifyVpcAttributeResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    try {
      vpcs.updateByExample(
          Vpc.exampleWithName( ctx.isAdministrator( ) ? null : accountFullName, Identifier.vpc.normalize( request.getVpcId( ) ) ),
          accountFullName,
          request.getVpcId( ),
          new Callback<Vpc>() {
            @Override
            public void fire( final Vpc vpc ) {
              if ( RestrictedTypes.filterPrivileged( ).apply( vpc ) ) {
                final AttributeBooleanValueType dnsHostnames = request.getEnableDnsHostnames( );
                if ( dnsHostnames != null && dnsHostnames.getValue( ) != null ) {
                  vpc.setDnsHostnames( dnsHostnames.getValue( ) );
                }
                final AttributeBooleanValueType dnsSupport = request.getEnableDnsSupport( );
                if ( dnsSupport != null && dnsSupport.getValue( ) != null ) {
                  vpc.setDnsEnabled( dnsSupport.getValue( ) );
                }
              } else {
                throw Exceptions.toUndeclared( new ClientUnauthorizedComputeException( "Not authorized to modify VPC attribute" ) );
              }
            }
          } );
    } catch( Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public RejectVpcPeeringConnectionResponseType rejectVpcPeeringConnection(RejectVpcPeeringConnectionType request) throws EucalyptusCloudException {
    RejectVpcPeeringConnectionResponseType reply = request.getReply( );
    return reply;
  }

  public ReplaceNetworkAclAssociationResponseType replaceNetworkAclAssociation(final ReplaceNetworkAclAssociationType request) throws EucalyptusCloudException {
    final ReplaceNetworkAclAssociationResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    final String networkAclId = Identifier.acl.normalize( request.getNetworkAclId( ) );
    final String associationId = Identifier.aclassoc.normalize( request.getAssociationId( ) );
    try {
      final Subnet subnet = subnets.updateByExample(
          Subnet.exampleWithNetworkAclAssociation( accountFullName, associationId ),
          accountFullName,
          request.getAssociationId( ),
          new Callback<Subnet>() {
            @Override
            public void fire( final Subnet subnet ) {
              if ( RestrictedTypes.filterPrivileged( ).apply( subnet ) ) try {
                final NetworkAcl networkAcl = networkAcls.lookupByName( accountFullName, networkAclId, Functions.<NetworkAcl>identity( ) );
                if ( !subnet.getVpc( ).getDisplayName( ).equals( networkAcl.getVpc( ).getDisplayName( ) ) ) {
                  throw new ClientComputeException( "InvalidParameterValue",
                      "Network ACL "+networkAclId+" and subnet "+subnet.getDisplayName( )+" belong to different networks" );
                }
                subnet.setNetworkAcl( networkAcl );
                subnet.setNetworkAclAssociationId( Identifier.aclassoc.generate() );
              } catch ( VpcMetadataNotFoundException e ) {
                throw Exceptions.toUndeclared( new ClientComputeException( "InvalidNetworkAclID.NotFound", "Network ACL not found '" + request.getAssociationId() + "'" ) );
              } catch ( Exception e ) {
                throw Exceptions.toUndeclared( e );
              } else {
                throw Exceptions.toUndeclared( new ClientUnauthorizedComputeException( "Not authorized to replace network ACL association" ) );
              }
            }
          } );
      invalidate( networkAclId );
      reply.setNewAssociationId( subnet.getNetworkAclAssociationId( ) );
    } catch ( VpcMetadataNotFoundException e ) {
      throw new ClientComputeException( "InvalidAssociationID.NotFound", "Network ACL association ("+request.getAssociationId( )+") not found " );
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public ReplaceNetworkAclEntryResponseType replaceNetworkAclEntry(final ReplaceNetworkAclEntryType request) throws EucalyptusCloudException {
    final ReplaceNetworkAclEntryResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName();
    final String networkAclId = Identifier.acl.normalize( request.getNetworkAclId( ) );
    final String cidr = request.getCidrBlock( );
    final Optional<Cidr> cidrOptional = Cidr.parse( ).apply( cidr );
    if ( !cidrOptional.isPresent( ) ) {
      throw new ClientComputeException( "InvalidParameterValue", "Cidr invalid: " + cidr );
    }
    final Optional<Integer> protocolOptional = protocolNumber( request.getProtocol() );
    if ( !protocolOptional.isPresent( ) ) {
      throw new ClientComputeException( "InvalidParameterValue", "Protocol invalid: " + request.getProtocol( ) );
    }
    if ( !Range.closed( 1, 32766 ).apply( request.getRuleNumber( ) ) ) {
      throw new ClientComputeException( "InvalidParameterValue", "Rule number invalid: " + request.getRuleNumber( ) );
    }
    try {
      networkAcls.updateByExample(
          NetworkAcl.exampleWithName( accountFullName, networkAclId ),
          accountFullName,
          request.getNetworkAclId(),
          new Callback<NetworkAcl>( ) {
            @Override
            public void fire( final NetworkAcl networkAcl ) {
              if ( RestrictedTypes.filterPrivileged( ).apply( networkAcl ) ) try {
                final List<NetworkAclEntry> entries = networkAcl.getEntries( );
                final Optional<NetworkAclEntry> oldEntry =
                    Iterables.tryFind( entries, entryPredicate( request.getEgress( ), request.getRuleNumber( ) ) );

                if ( !oldEntry.isPresent( ) ) {
                  throw new ClientComputeException(
                      "InvalidNetworkAclEntry.NotFound",
                      "Entry not found for rule number: " + request.getRuleNumber( ) );
                }

                final NetworkAclEntry entry;
                switch ( protocolOptional.get( ) ) {
                  case 1:
                    entry = NetworkAclEntry.createIcmpEntry(
                        networkAcl,
                        request.getRuleNumber(),
                        Enums.valueOfFunction( NetworkAclEntry.RuleAction.class ).apply( request.getRuleAction() ),
                        request.getEgress(),
                        cidr,
                        request.getIcmpTypeCode().getCode(),
                        request.getIcmpTypeCode().getType() );
                    break;
                  case 6:
                  case 17:
                    entry = NetworkAclEntry.createTcpUdpEntry(
                        networkAcl,
                        request.getRuleNumber(),
                        protocolOptional.get(),
                        Enums.valueOfFunction( NetworkAclEntry.RuleAction.class ).apply( request.getRuleAction() ),
                        request.getEgress(),
                        cidr,
                        request.getPortRange().getFrom(),
                        request.getPortRange().getTo() );
                    break;
                  default:
                    entry = NetworkAclEntry.createEntry(
                        networkAcl,
                        request.getRuleNumber( ),
                        protocolOptional.get(),
                        Enums.valueOfFunction( NetworkAclEntry.RuleAction.class ).apply( request.getRuleAction( ) ),
                        request.getEgress( ),
                        cidr );
                }

                entries.set(
                    entries.indexOf( oldEntry.get() ),
                    entry );
                networkAcl.updateTimeStamps( ); // ensure version of table increments also
              } catch ( Exception e ) {
                throw Exceptions.toUndeclared( e );
              } else {
                throw Exceptions.toUndeclared( new ClientUnauthorizedComputeException( "Not authorized to replace network ACL entry" ) );
              }
            }
          }
      );
      invalidate( networkAclId );
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public ReplaceRouteResponseType replaceRoute(final ReplaceRouteType request) throws EucalyptusCloudException {
    final ReplaceRouteResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName();
    final String gatewayId = Identifier.igw.normalize( request.getGatewayId( ) );
    final String routeTableId = Identifier.rtb.normalize( request.getRouteTableId( ) );
    final String destinationCidr = request.getDestinationCidrBlock( );
    final Optional<Cidr> destinationCidrOption = Cidr.parse( ).apply( destinationCidr );
    if ( !destinationCidrOption.isPresent( ) ) {
      throw new ClientComputeException( "InvalidParameterValue", "Cidr invalid: " + destinationCidr );
    }
    try {
      routeTables.updateByExample(
          RouteTable.exampleWithName( accountFullName, routeTableId ),
          accountFullName,
          request.getRouteTableId( ),
          new Callback<RouteTable>( ) {
            @Override
            public void fire( final RouteTable routeTable ) {
              if ( RestrictedTypes.filterPrivileged( ).apply( routeTable ) ) try {
                final InternetGateway internetGateway =
                    internetGateways.lookupByName( accountFullName, gatewayId, Functions.<InternetGateway>identity() );

                final List<Route> routes = routeTable.getRoutes( );
                final Optional<Route> oldRoute =
                    Iterables.tryFind( routes, CollectionUtils.propertyPredicate(
                        destinationCidr,
                        RouteTables.RouteFilterStringFunctions.DESTINATION_CIDR ) );

                if ( !oldRoute.isPresent( ) ) {
                  throw new ClientComputeException(
                      "InvalidRoute.NotFound",
                      "Route not found for cidr: " + destinationCidr );
                }

                routes.set(
                    routes.indexOf( oldRoute.get() ),
                    Route.create( routeTable, Route.RouteOrigin.CreateRoute, destinationCidr, internetGateway ) );
                routeTable.updateTimeStamps( ); // ensure version of table increments also
              } catch ( Exception e ) {
                throw Exceptions.toUndeclared( e );
              } else {
                throw Exceptions.toUndeclared( new ClientUnauthorizedComputeException( "Not authorized to replace route" ) );
              }
            }
          }
      );
      invalidate( routeTableId );
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public ReplaceRouteTableAssociationResponseType replaceRouteTableAssociation(final ReplaceRouteTableAssociationType request) throws EucalyptusCloudException {
    final ReplaceRouteTableAssociationResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    final String routeTableId = Identifier.rtb.normalize( request.getRouteTableId( ) );
    final String associationId = Identifier.rtbassoc.normalize( request.getAssociationId() );
    try {
      final String newAssociationId = routeTables.updateByAssociationId(
          associationId,
          accountFullName,
          new Function<RouteTable,String>() {
            @Override
            public String apply( final RouteTable routeTable ) {
              if ( RestrictedTypes.filterPrivileged( ).apply( routeTable ) ) try {
                final RouteTable newRouteTable = routeTables.lookupByName( accountFullName, routeTableId, Functions.<RouteTable>identity( ) );
                final RouteTableAssociation association = Iterables.find(
                    routeTable.getRouteTableAssociations( ),
                    CollectionUtils.propertyPredicate( associationId, RouteTables.AssociationFilterStringFunctions.ASSOCIATION_ID ) );
                if ( !newRouteTable.getVpc( ).getDisplayName( ).equals( routeTable.getVpc( ).getDisplayName( ) ) ) {
                  throw Exceptions.toUndeclared( new ClientComputeException( "InvalidParameterValue",
                      "Route table "+routeTableId+" belongs to different network" ) );
                }
                final RouteTableAssociation oldAssociation = routeTable.disassociate( associationId );
                Entities.delete( oldAssociation );
                Entities.flush( oldAssociation );
                final RouteTableAssociation newAssociation;
                if ( association.getMain( ) ) { // replacing main route table for VPC
                  newAssociation = newRouteTable.associateMain( );
                } else {
                  newAssociation = newRouteTable.associate( association.getSubnet( ) );
                }
                return newAssociation.getAssociationId( );
              } catch ( VpcMetadataNotFoundException e ) {
                throw Exceptions.toUndeclared( new ClientComputeException( "InvalidRouteTableID.NotFound", "Route table not found '" + request.getRouteTableId() + "'" ) );
              } catch ( Exception e ) {
                throw Exceptions.toUndeclared( e );
              } else {
                throw Exceptions.toUndeclared( new ClientUnauthorizedComputeException( "Not authorized to replace route table association" ) );
              }
            }
          } );
      invalidate( routeTableId );
      reply.setNewAssociationId( newAssociationId );
    } catch ( VpcMetadataNotFoundException e ) {
      throw new ClientComputeException( "InvalidAssociationID.NotFound", "Route table association ("+request.getAssociationId( )+") not found " );
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public ResetNetworkInterfaceAttributeResponseType resetNetworkInterfaceAttribute(final ResetNetworkInterfaceAttributeType request) throws EucalyptusCloudException {
    final ResetNetworkInterfaceAttributeResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    final String eniId = Identifier.eni.normalize( request.getNetworkInterfaceId( ) );
    try {
      networkInterfaces.updateByExample(
          NetworkInterface.exampleWithName( accountFullName, eniId ),
          accountFullName,
          request.getNetworkInterfaceId(),
          new Callback<NetworkInterface>() {
            @Override
            public void fire( final NetworkInterface networkInterface ) {
              if ( RestrictedTypes.filterPrivileged( ).apply( networkInterface ) ) {
                switch ( request.getAttribute() ) {
                  case "sourceDestCheck":
                    networkInterface.setSourceDestCheck( true );
                    break;
                  default:
                    throw Exceptions.toUndeclared( new ClientComputeException(
                        "InvalidParameterValue",
                        "Value ("+request.getAttribute( )+") for parameter attribute is invalid. Unknown network interface attribute"
                    ) );
                }
              } else {
                throw Exceptions.toUndeclared( new ClientUnauthorizedComputeException( "Not authorized to reset network interface attribute" ) );
              }
            }
          } );
      invalidate( eniId );
    } catch ( final Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public UnassignPrivateIpAddressesResponseType unassignPrivateIpAddresses(UnassignPrivateIpAddressesType request) throws EucalyptusCloudException {
    UnassignPrivateIpAddressesResponseType reply = request.getReply( );
    return reply;
  }

  private enum Identifier {
    acl( "networkAcl" ),
    aclassoc( "networkAclAssociation" ),
    dopt( "DHCPOption" ),
    eni( "networkInterface" ),
    igw( "internetGateway" ),
    rtb( "routeTable" ),
    rtbassoc( "routeTableAssociation" ),
    subnet( "subnet" ),
    vpc( "vpc" ),
    ;

    private final String code;
    private final String defaultParameter;
    private final String defaultListParameter;

    Identifier( final String defaultParameter ) {
      this( defaultParameter, defaultParameter + "s" );
    }

    Identifier( final String defaultParameter, final String defaultListParameter ) {
      this.code = "InvalidParameterValue";
      this.defaultParameter = defaultParameter;
      this.defaultListParameter = defaultListParameter;
    }

    public String generate( ) {
      return ResourceIdentifiers.generateString( name( ) );
    }

    public String normalize( final String identifier ) throws EucalyptusCloudException {
      return normalize( identifier, defaultParameter );
    }

    public String normalize( final String identifier, final String parameter ) throws EucalyptusCloudException {
      return normalize( Collections.singleton( identifier ), parameter ).get( 0 );
    }

    public List<String> normalize( final Iterable<String> identifiers ) throws EucalyptusCloudException {
      return normalize( identifiers, defaultListParameter );
    }

    public List<String> normalize( final Iterable<String> identifiers,
                                   final String parameter ) throws EucalyptusCloudException {
      try {
        return ResourceIdentifiers.normalize( name( ), identifiers );
      } catch ( final InvalidResourceIdentifier e ) {
        throw new ClientComputeException(
            code,
            "Value ("+e.getIdentifier()+") for parameter "+parameter+" is invalid. Expected: '"+name()+"-...'." );
      }
    }
  }

  private <T extends AbstractPersistent & RestrictedType,AT> AT allocate(
      final Supplier<T> allocator,
      final Class<T> type,
      final Class<AT> apiType
  ) throws EucalyptusCloudException {
    try {
      return TypeMappers.transform( RestrictedTypes.allocateUnitlessResources( type, 1, transactional( allocator ) ).get( 0 ), apiType );
    } catch ( Exception e ) {
      throw handleException( e, true );
    }
  }

  private <E extends AbstractPersistent> void delete(
      final Identifier identifier,
      final String idParam,
      final Function<Pair<Optional<AccountFullName>,String>,E> deleter
  ) throws EucalyptusCloudException {
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountName = ctx.isAdministrator( ) ? null : ctx.getUserFullName( ).asAccountFullName( );
    final String id = identifier.normalize( idParam );
    try {
      transactional( deleter ).apply( Pair.lopair( accountName, id ) );
    } catch ( Exception e ) {
      if ( Exceptions.isCausedBy( e, ConstraintViolationException.class ) ) {
        throw new ClientComputeException( "DependencyViolation", "Resource ("+idParam+") is in use" );
      }
      if ( !Exceptions.isCausedBy( e, VpcMetadataNotFoundException.class ) ) {
        throw handleException( e );
      } // else ignore missing on delete?
    }
  }

  private static <AP extends AbstractPersistent & CloudMetadata, AT extends VpcTagged> void describe(
      final Identifier identifier,
      final Collection<String> ids,
      final Collection<com.eucalyptus.compute.common.Filter> filters,
      final Class<AP> persistent,
      final Class<AT> api,
      final List<AT> results,
      final Function<AT,String> idFunction,
      final Lister<AP> lister ) throws EucalyptusCloudException {
    final boolean showAll = ids.remove( "verbose" );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    final OwnerFullName ownerFullName = ctx.isAdministrator( ) && showAll ? null : accountFullName;
    final com.eucalyptus.tags.Filter filter = Filters.generate( filters, persistent );
    final Predicate<? super AP> requestedAndAccessible = CloudMetadatas.filteringFor( persistent )
        .byId( identifier.normalize( ids ) )
        .byPredicate( filter.asPredicate( ) )
        .byPrivileges()
        .buildPredicate();

    try {
      results.addAll( lister.list(
          ownerFullName,
          filter.asCriterion( ),
          filter.getAliases( ),
          requestedAndAccessible,
          TypeMappers.lookup( persistent, api ) ) );

      populateTags( accountFullName, persistent, results, idFunction );
    } catch ( Exception e ) {
      throw handleException( e );
    }
  }

  protected <E extends AbstractPersistent> Supplier<E> transactional( final Supplier<E> supplier ) {
    return Entities.asTransaction( supplier );
  }

  protected <E extends AbstractPersistent,P> Function<P,E> transactional( final Function<P,E> function ) {
    return Entities.asTransaction( function );
  }

  private static <VT extends VpcTagged> void populateTags( final AccountFullName accountFullName,
                                                           final Class<? extends CloudMetadata> resourceType,
                                                           final List<? extends VT> items,
                                                           final Function<? super VT, String> idFunction ) {
    final Map<String,List<Tag>> tagsMap = TagSupport.forResourceClass( resourceType )
        .getResourceTagMap( accountFullName, Iterables.transform( items, idFunction ) );
    for ( final VT item : items ) {
      final ResourceTagSetType tags = new ResourceTagSetType( );
      Tags.addFromTags( tags.getItem(), ResourceTagSetItemType.class, tagsMap.get( idFunction.apply( item ) ) );
      if ( !tags.getItem().isEmpty() ) {
        item.setTagSet( tags );
      }
    }
  }

  private static Optional<Integer> protocolNumber( final String protocol ) {
    switch ( Objects.toString( protocol, "-1" ).toLowerCase( ) ) {
      case "tcp":
      case "6":
        return Optional.of( 6 );
      case "udp":
      case "17":
        return Optional.of( 17 );
      case "icmp":
      case "1":
        return Optional.of( 1 );
      default:
        return Iterables.tryFind( Optional.fromNullable( Ints.tryParse( protocol ) ).asSet(), Range.closed( -1, 255 ) );
    }
  }

  private static Predicate<NetworkAclEntry> entryPredicate( final Boolean egress,
                                                            final Integer ruleNumber ) {
    return Predicates.and(
        ruleNumber == null ?
            Predicates.<NetworkAclEntry>alwaysTrue( ) :
            CollectionUtils.propertyPredicate(
              ruleNumber,
              NetworkAcls.NetworkAclEntryFilterIntegerFunctions.RULE_NUMBER ),
        CollectionUtils.propertyPredicate(
            egress,
            NetworkAcls.NetworkAclEntryFilterBooleanFunctions.EGRESS )
    );
  }

  private void invalidate( final String resourceIdentifier ) {
    vpcInvalidator.invalidate( resourceIdentifier );
  }

  private static ComputeException handleException( final Exception e ) throws ComputeException {
    throw handleException( e, false );
  }

  /**
   * Method always throws, signature allows use of "throw handleException ..."
   */
  private static ComputeException handleException( final Exception e,
                                                   final boolean isCreate ) throws ComputeException {
    final ComputeException cause = Exceptions.findCause( e, ComputeException.class );
    if ( cause != null ) {
      throw cause;
    }

    final AuthQuotaException quotaCause = Exceptions.findCause( e, AuthQuotaException.class );
    if ( quotaCause != null ) {
      String code = "ResourceLimitExceeded";
      switch( quotaCause.getType( ) ) {
        case "vpc":
          code = "VpcLimitExceeded";
          break;
        case "internet-gateway":
          code = "InternetGatewayLimitExceeded";
          break;
      }
      throw new ClientComputeException( code, "Request would exceed quota for type: " + quotaCause.getType() );
    }

    logger.error( e, e );

    final ComputeException exception = new ComputeException( "InternalError", String.valueOf(e.getMessage()) );
    if ( Contexts.lookup( ).hasAdministrativePrivileges() ) {
      exception.initCause( e );
    }
    throw exception;
  }
}
