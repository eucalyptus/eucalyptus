/*************************************************************************
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.compute.vpc;

import static com.google.common.base.MoreObjects.firstNonNull;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthQuotaException;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.compute.common.NatGatewayType;
import com.eucalyptus.compute.common.internal.network.NetworkCidr;
import com.eucalyptus.compute.common.internal.util.NoSuchMetadataException;
import com.eucalyptus.compute.common.internal.util.ResourceAllocationException;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.ClientComputeException;
import com.eucalyptus.compute.ClientUnauthorizedComputeException;
import com.eucalyptus.compute.ComputeException;
import com.eucalyptus.compute.common.AttributeBooleanValueType;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.DhcpConfigurationItemType;
import com.eucalyptus.compute.common.DhcpOptionsType;
import com.eucalyptus.compute.common.InternetGatewayType;
import com.eucalyptus.compute.common.NetworkAclType;
import com.eucalyptus.compute.common.NetworkInterfaceType;
import com.eucalyptus.compute.common.RouteTableType;
import com.eucalyptus.compute.common.SubnetType;
import com.eucalyptus.compute.common.VpcType;
import com.eucalyptus.compute.common.internal.identifier.InvalidResourceIdentifier;
import com.eucalyptus.compute.common.internal.identifier.ResourceIdentifiers;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vpc.DhcpOption;
import com.eucalyptus.compute.common.internal.vpc.DhcpOptionSet;
import com.eucalyptus.compute.common.internal.vpc.DhcpOptionSets;
import com.eucalyptus.compute.common.internal.vpc.InternetGateway;
import com.eucalyptus.compute.common.internal.vpc.InternetGateways;
import com.eucalyptus.compute.common.internal.vpc.NatGateway;
import com.eucalyptus.compute.common.internal.vpc.NatGateways;
import com.eucalyptus.compute.common.internal.vpc.NetworkAcl;
import com.eucalyptus.compute.common.internal.vpc.NetworkAclEntry;
import com.eucalyptus.compute.common.internal.vpc.NetworkAcls;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaceAttachment;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaces;
import com.eucalyptus.compute.common.internal.vpc.Route;
import com.eucalyptus.compute.common.internal.vpc.RouteTable;
import com.eucalyptus.compute.common.internal.vpc.RouteTableAssociation;
import com.eucalyptus.compute.common.internal.vpc.RouteTables;
import com.eucalyptus.compute.common.internal.vpc.SecurityGroups;
import com.eucalyptus.compute.common.internal.vpc.Subnet;
import com.eucalyptus.compute.common.internal.vpc.Subnets;
import com.eucalyptus.compute.common.internal.vpc.Vpc;
import com.eucalyptus.compute.common.internal.vpc.VpcMetadataNotFoundException;
import com.eucalyptus.compute.common.internal.vpc.Vpcs;
import com.eucalyptus.compute.common.network.Networking;
import com.eucalyptus.compute.common.network.NetworkingFeature;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.network.IPRange;
import com.eucalyptus.compute.common.internal.network.NetworkGroup;
import com.eucalyptus.network.NetworkGroups;
import com.eucalyptus.compute.common.internal.network.NetworkPeer;
import com.eucalyptus.compute.common.internal.network.NetworkRule;
import com.eucalyptus.network.PrivateAddresses;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Cidr;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.Pair;
import com.eucalyptus.auth.type.RestrictedType;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.dns.DomainNames;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.vmtypes.VmTypes;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
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
@SuppressWarnings( { "UnnecessaryLocalVariable", "UnusedDeclaration", "Guava", "Convert2Lambda", "RedundantTypeArguments", "StaticPseudoFunctionalStyleMethod" } )
@ComponentNamed("computeVpcManager")
public class VpcManager {

  private static final Logger logger = Logger.getLogger( VpcManager.class );

  private final DhcpOptionSets dhcpOptionSets;
  private final InternetGateways internetGateways;
  private final NatGateways natGateways;
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
                     final NatGateways natGateways,
                     final NetworkAcls networkAcls,
                     final NetworkInterfaces networkInterfaces,
                     final RouteTables routeTables,
                     final SecurityGroups securityGroups,
                     final Subnets subnets,
                     final Vpcs vpcs,
                     final VpcInvalidator vpcInvalidator ) {
    this.dhcpOptionSets = dhcpOptionSets;
    this.internetGateways = internetGateways;
    this.natGateways = natGateways;
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
                    null :
                    dhcpOptionSets.lookupByName( accountFullName, dhcpOptionsId, Functions.identity( ) );
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
      final RouteTable routeTable = routeTables.withRetries( ).updateByExample(
          RouteTable.exampleWithName( accountFullName, routeTableId ),
          accountFullName,
          request.getRouteTableId(),
          new Callback<RouteTable>() {
            @Override
            public void fire( final RouteTable routeTable ) {
              if ( RestrictedTypes.filterPrivileged( ).apply( routeTable ) ) try {
                final Subnet subnet = subnets.lookupByName( accountFullName, subnetId, Functions.identity( ) );

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
                final Vpc vpc = vpcs.lookupByName( accountFullName, vpcId, Functions.identity( ) );
                if ( internetGateway.getVpc( ) != null ) {
                  throw Exceptions.toUndeclared( new ClientComputeException( "Resource.AlreadyAssociated",
                      "resource "+gatewayId+" is already attached to network " + internetGateway.getVpc( ).getDisplayName() ) );
                }
                try {
                  internetGateways.lookupByVpc( null, vpcId, Functions.identity( ) );
                  throw Exceptions.toUndeclared( new ClientComputeException( "InvalidParameterValue",
                      "Network "+vpcId+" already has an internet gateway attached" ) );
                } catch ( final VpcMetadataNotFoundException e ) {
                  // expected
                }
                internetGateway.setVpc( vpc );
              } catch ( final VpcMetadataNotFoundException e ) {
                throw Exceptions.toUndeclared( new ClientComputeException( "InvalidVpcID.NotFound", "Vpc not found '" + request.getVpcId() + "'" ) );
              } catch ( final Exception e ) {
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

  public AttachNetworkInterfaceResponseType attachNetworkInterface(
      final AttachNetworkInterfaceType request
  ) throws EucalyptusCloudException {
    final AttachNetworkInterfaceResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    final String networkInterfaceId = Identifier.eni.normalize( request.getNetworkInterfaceId( ) );
    final String instanceId = Identifier.i.normalize( request.getInstanceId( ) );
    final Integer deviceIndex = request.getDeviceIndex( );
    final VmInstance[] vmRef = new VmInstance[1];
    try {
      final NetworkInterface eni = networkInterfaces.updateByExample(
          NetworkInterface.exampleWithName( accountFullName, networkInterfaceId ),
          accountFullName,
          request.getNetworkInterfaceId( ),
          new Callback<NetworkInterface>() {
            @Override
            public void fire( final NetworkInterface networkInterface ) {
              if ( RestrictedTypes.filterPrivileged( ).apply( networkInterface ) ) try {
                final VmInstance vm = RestrictedTypes.resolver( VmInstance.class ).apply( instanceId );
                if ( networkInterface.isAttached( ) ) {
                  throw Exceptions.toUndeclared( new ClientComputeException( "InvalidNetworkInterface.InUse",
                      "Network interface "+networkInterfaceId+" is already attached to instance " +
                          networkInterface.getAttachment( ).getInstanceId( ) ) );
                }
                if ( deviceIndex < 0 || deviceIndex > 31 ) {
                  throw new ClientComputeException( "InvalidParameterValue", "Invalid network device index '"+deviceIndex+"'" );
                }
                //noinspection ConstantConditions
                for ( final NetworkInterface eni : vm.getNetworkInterfaces( ) ) {
                  if ( eni.isAttached( ) && deviceIndex.equals( eni.getAttachment( ).getDeviceIndex( ) ) ) {
                    throw new ClientComputeException( "InvalidParameterValue", "Device index in use" );
                  }
                }
                if ( !vm.getPartition( ).equals( networkInterface.getAvailabilityZone( ) ) ) {
                  throw new ClientComputeException( "InvalidParameterValue", "Network interface zone invalid for instance" );
                }
                final String instanceVpcId = vm.getVpcId( );
                if ( instanceVpcId==null || ( !ctx.isPrivileged( ) &&
                    !instanceVpcId.equals( networkInterface.getVpc( ).getDisplayName( ) ) ) ) {
                  throw new ClientComputeException( "InvalidParameterValue", "Network interface vpc invalid for instance" );
                }
                final int interfaceCount = vm.getNetworkInterfaces( ).size( ) + 1;
                final int maxInterfaces = VmTypes.lookup( vm.getInstanceType( ) ).getNetworkInterfaces( );
                if ( interfaceCount > maxInterfaces ) {
                  throw new ClientComputeException(
                      "AttachmentLimitExceeded",
                      "Interface count "+interfaceCount+" exceeds the limit for " + vm.getInstanceType( ) );
                }
                final NetworkInterfaceAttachment.Status initialState;
                if ( VmInstance.VmState.RUNNING.apply( vm ) ) {
                  vmRef[0] = vm;
                  initialState = NetworkInterfaceAttachment.Status.attaching;
                } else if ( VmInstance.VmState.STOPPED.apply( vm ) ) {
                  initialState = NetworkInterfaceAttachment.Status.attached;
                } else {
                  throw new ClientComputeException(
                      "IncorrectState", "Instance '"+instanceId+"' is not 'running' or 'stopped'" );
                }
                networkInterface.attach( NetworkInterfaceAttachment.create(
                    Identifier.eni_attach.generate( ctx.getUser( ) ),
                    vm,
                    vm.getDisplayName( ),
                    vm.getOwnerAccountNumber( ),
                    deviceIndex,
                    initialState,
                    new Date( ),
                    false
                )  );
              } catch ( NoSuchElementException e ) {
                throw Exceptions.toUndeclared( new ClientComputeException( "InvalidInstanceID.NotFound",
                    "Instance not found '" + request.getInstanceId() + "'" ) );
              } catch ( Exception e ) {
                throw Exceptions.toUndeclared( e );
              } else {
                throw Exceptions.toUndeclared(
                    new ClientUnauthorizedComputeException( "Not authorized to attach network interface" ) );
              }
            }
          } );
      if ( vmRef[0] != null ) {
        NetworkInterfaceHelper.start( eni, vmRef[0] );
        invalidate( vmRef[0].getVpcId( ) );
      }
      reply.setAttachmentId( eni.getAttachment( ).getAttachmentId( ) );
    } catch ( VpcMetadataNotFoundException e ) {
      throw new ClientComputeException( "InvalidNetworkInterfaceID.NotFound",
          "Network interface ("+request.getNetworkInterfaceId()+") not found " );
    } catch ( Exception e ) {
      throw handleException( e );
    }
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
          final DhcpOptionSet dhcpOptionSet = DhcpOptionSet.create( ctx.getUserFullName(), Identifier.dopt.generate( ctx.getUser( ) ) );
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
            dhcpOptionSet.getDhcpOptions( ).add( DhcpOption.create( dhcpOptionSet, item.getKey(), item.values() ) );
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
          return internetGateways.save( InternetGateway.create( ctx.getUserFullName( ), Identifier.igw.generate( ctx.getUser( ) ) ) );
        } catch ( Exception ex ) {
          throw new RuntimeException( ex );
        }
      }
    };
    reply.setInternetGateway( allocate( allocator, InternetGateway.class, InternetGatewayType.class ) );
    return reply;
  }

  public CreateNatGatewayResponseType createNatGateway(
      final CreateNatGatewayType request
  ) throws EucalyptusCloudException {
    final CreateNatGatewayResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup();
    final String allocationId = Identifier.eipalloc.normalize( request.getAllocationId( ) );
    final String subnetId = Identifier.subnet.normalize( request.getSubnetId( ) );
    final String clientToken = Strings.emptyToNull( request.getClientToken( ) );
    final Supplier<NatGateway> allocator = new Supplier<NatGateway>( ) {
      @Override
      public NatGateway get( ) {
        try {
          final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
          if ( clientToken != null ) try {
            return natGateways.lookupByClientToken( accountFullName, clientToken, Functions.identity( ) );
          } catch ( final VpcMetadataNotFoundException e ) {
            // create new NAT gateway with the given client token
          }

          if (!Networking.getInstance( ).supports( NetworkingFeature.Vpc )) {
            throw new ClientComputeException( "Unsupported",
                "EC2-VPC platform required for NAT gateway, not supported with EC2-Classic" );
          }

          final Subnet subnet = subnets.lookupByName( accountFullName, subnetId, Functions.identity( ) );
          final long natGatewayCount = natGateways.countByZone( accountFullName, subnet.getAvailabilityZone( ) );
          if ( natGatewayCount >= VpcConfiguration.getNatGatewaysPerAvailabilityZone( ) ) {
            throw new ClientComputeException( "NatGatewayLimitExceeded",
                "NAT gateway limit exceeded for availability zone " + subnet.getAvailabilityZone( ) );
          }

          final String natGatewayId = Identifier.nat.generate( ctx.getUser( ) );
          final Vpc vpc = subnet.getVpc( );
          return natGateways.save(
              NatGateway.create( ctx.getUserFullName( ), vpc, subnet, natGatewayId, clientToken, allocationId ) );
        } catch ( final VpcMetadataNotFoundException e ) {
          throw Exceptions.toUndeclared(
              new ClientComputeException( "InvalidSubnetID.NotFound",  "Subnet not found '" + subnetId + "'" ) );
        } catch ( final Exception ex ) {
          throw Exceptions.toUndeclared( ex );
        }
      }
    };
    reply.setClientToken( clientToken );
    reply.setNatGateway( allocate( allocator, NatGateway.class, NatGatewayType.class ) );
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
          final Vpc vpc = vpcs.lookupByName( ctx.getUserFullName( ).asAccountFullName( ), vpcId, Functions.identity( ) );
          final long networkAclsForVpc = networkAcls.countByExample(
              NetworkAcl.exampleWithOwner( ctx.getUserFullName( ).asAccountFullName( ) ),
              Restrictions.eq( "vpc.displayName", vpcId ),
              Collections.singletonMap( "vpc", "vpc" ) );
          if ( networkAclsForVpc >= VpcConfiguration.getNetworkAclsPerVpc( ) ) {
            throw new ClientComputeException( "NetworkAclLimitExceeded", "Network ACL limit exceeded for " + vpc.getDisplayName( ) );
          }
          return networkAcls.save( NetworkAcl.create( ctx.getUserFullName( ), vpc, Identifier.acl.generate( ctx.getUser( ) ), false ) );
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
    final Optional<Cidr> cidrOptional = Cidr.parseLax( ).apply( cidr );
    if ( !cidrOptional.isPresent( ) ) {
      throw new ClientComputeException( "InvalidParameterValue", "Cidr invalid: " + cidr );
    }
    final String aclCidr = cidrOptional.get( ).toString( );
    final Optional<Integer> protocolOptional = protocolNumber( request.getProtocol( ) );
    if ( !protocolOptional.isPresent( ) ) {
      throw new ClientComputeException( "InvalidParameterValue", "Protocol invalid: " + request.getProtocol( ) );
    }
    if ( !Range.closed( 1, 32766 ).contains( request.getRuleNumber( ) ) ) {
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
                @SuppressWarnings( "OptionalGetWithoutIsPresent" )
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
                            FUtils.valueOfFunction( NetworkAclEntry.RuleAction.class ).apply( request.getRuleAction() ),
                            request.getEgress(),
                            aclCidr,
                            request.getIcmpTypeCode().getCode(),
                            request.getIcmpTypeCode().getType() );
                        break;
                      case 6:
                      case 17:
                        entry = NetworkAclEntry.createTcpUdpEntry(
                            networkAcl,
                            request.getRuleNumber(),
                            protocolOptional.get(),
                            FUtils.valueOfFunction( NetworkAclEntry.RuleAction.class ).apply( request.getRuleAction() ),
                            request.getEgress(),
                            aclCidr,
                            request.getPortRange().getFrom(),
                            request.getPortRange().getTo() );
                        break;
                      default:
                        entry = NetworkAclEntry.createEntry(
                            networkAcl,
                            request.getRuleNumber( ),
                            protocolOptional.get(),
                            FUtils.valueOfFunction( NetworkAclEntry.RuleAction.class ).apply( request.getRuleAction( ) ),
                            request.getEgress( ),
                            aclCidr );
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
          final Subnet subnet =
              subnets.lookupByName( ctx.isPrivileged( ) ? null : accountFullName, subnetId, Functions.identity( ) );
          final Vpc vpc = subnet.getVpc( );
          final Set<NetworkGroup> groups = request.getGroupSet( )==null || request.getGroupSet( ).groupIds( ).isEmpty( ) ?
              Sets.newHashSet( securityGroups.lookupDefault( vpc.getDisplayName( ), Functions.identity( ) ) ) :
              Sets.newHashSet( Iterables.transform(
                  Identifier.sg.normalize( request.getGroupSet( ).groupIds( ) ),
                  RestrictedTypes.resolver( NetworkGroup.class ) ) );
          if ( groups.size( ) > VpcConfiguration.getSecurityGroupsPerNetworkInterface( ) ) {
            throw new ClientComputeException( "SecurityGroupsPerInterfaceLimitExceeded", "Security group limit exceeded" );
          }
          if ( !Collections.singleton( vpc.getDisplayName( ) ).equals(
              Sets.newHashSet( Iterables.transform( groups, NetworkGroup.vpcId( ) ) ) ) ) {
            throw Exceptions.toUndeclared( new ClientComputeException( "InvalidParameterValue", "Invalid security groups (inconsistent VPC)" ) );
          }
          final String identifier = Identifier.eni.generate( ctx.getUser( ) );
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
          final NetworkInterface networkInterface = networkInterfaces.save( NetworkInterface.create(
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
          PrivateAddresses.associate( ip, networkInterface );
          return networkInterface;
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
    final String gatewayId = request.getGatewayId( ) == null ?
        null : Identifier.igw.normalize( request.getGatewayId( ) );
    final String natGatewayId = request.getNatGatewayId( ) == null ?
        null : Identifier.nat.normalize( request.getNatGatewayId( ) );
    final String instanceId = request.getInstanceId( ) == null ?
        null : Identifier.i.normalize( request.getInstanceId( ) );
    final String networkInterfaceId = request.getNetworkInterfaceId( ) == null ?
        null : Identifier.eni.normalize( request.getNetworkInterfaceId( ) );
    final String routeTableId = Identifier.rtb.normalize( request.getRouteTableId() );
    final String destinationCidr = request.getDestinationCidrBlock( );
    final Optional<Cidr> destinationCidrOption = Cidr.parseLax( ).apply( destinationCidr );
    if ( !destinationCidrOption.isPresent( ) ) {
      throw new ClientComputeException( "InvalidParameterValue", "Cidr invalid: " + destinationCidr );
    }
    final String routeCidr = destinationCidrOption.get( ).toString( );
    if ( gatewayId == null && natGatewayId == null && instanceId == null && networkInterfaceId == null ) {
      throw new ClientComputeException( "InvalidParameterCombination", "Route target required" );
    }
    final Supplier<Route> allocator = transactional( new Supplier<Route>( ) {
      @Override
      public Route get( ) {
        try {
          final NetworkInterface networkInterface;
          try {
            networkInterface = networkInterfaceId == null ? null :
              networkInterfaces.lookupByName( accountFullName, networkInterfaceId, Functions.identity( ) );
          } catch ( VpcMetadataNotFoundException e ) {
            throw new ClientComputeException( "InvalidNetworkInterfaceID.NotFound",
                "Network interface not found '" + request.getNetworkInterfaceId( ) + "'" );
          }
          final VmInstance instance;
          try {
            instance = instanceId == null ? null : VmInstances.lookup( instanceId );
          } catch ( NoSuchElementException e ) {
            throw new ClientComputeException( "InvalidInstanceID.NotFound",
                "Instance not found '" + request.getInstanceId( ) + "'" );
          }
          final InternetGateway internetGateway;
          try {
            internetGateway = gatewayId == null ?
                null : internetGateways.lookupByName( accountFullName, gatewayId, Functions.identity( ) );
          } catch ( VpcMetadataNotFoundException e ) {
            throw new ClientComputeException( "InvalidInternetGatewayID.NotFound",
                "Internet gateway not found '" + request.getGatewayId( ) + "'" );
          }
          final NatGateway natGateway;
          try {
            natGateway = natGatewayId == null ? null :
              natGateways.lookupByName( accountFullName, natGatewayId, Functions.identity() );
          } catch ( VpcMetadataNotFoundException e ) {
            throw new ClientComputeException( "InvalidNatGatewayID.NotFound",
                "NAT gateway not found '" + request.getNatGatewayId( ) + "'" );
          }
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
                              routeCidr,
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

                      final Route route = createRouteForTarget(
                          routeTable, routeCidr, networkInterface, instance, natGateway, internetGateway, gatewayId );

                      routeTable.getRoutes().add( route );
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
        } catch ( VpcMetadataNotFoundException e ) {
          throw Exceptions.toUndeclared( new ClientComputeException( "InvalidRouteTableID.NotFound", "" +
              "Route table not found '" + request.getRouteTableId() + "'" ) );
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
          final Vpc vpc = vpcs.lookupByName( ctx.getUserFullName().asAccountFullName(), vpcId, Functions.identity() );

          final long routeTablesForVpc = routeTables.countByExample(
              RouteTable.exampleWithOwner( ctx.getUserFullName( ).asAccountFullName( ) ),
              Restrictions.eq( "vpc.displayName", vpcId ),
              Collections.singletonMap( "vpc", "vpc" ) );
          if ( routeTablesForVpc >= VpcConfiguration.getRouteTablesPerVpc( ) ) {
            throw new ClientComputeException( " RouteTableLimitExceeded", "Route table limit exceeded for " + vpc.getDisplayName( ) );
          }
          return routeTables.save( RouteTable.create( ctx.getUserFullName( ), vpc, Identifier.rtb.generate( ctx.getUser( ) ), vpc.getCidr(), false ) );
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

  public CreateDefaultSubnetResponseType createDefaultSubnet(
      final CreateDefaultSubnetType request
  ) throws EucalyptusCloudException {
    final CreateDefaultSubnetResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName();
    final Optional<String> availabilityZone = Iterables.tryFind(
        Clusters.list( ),
        Predicates.and(
            CollectionUtils.propertyPredicate( request.getAvailabilityZone( ), CloudMetadatas.toDisplayName( ) ),
            RestrictedTypes.filterPrivilegedWithoutOwner( ) ) ).transform( CloudMetadatas.toDisplayName( ) );
    if ( !availabilityZone.isPresent( ) ) {
      throw new ClientComputeException( "InvalidParameterValue", "Availability zone invalid: " + request.getAvailabilityZone( ) );
    }
    final Supplier<Subnet> allocator = new Supplier<Subnet>( ) {
      @SuppressWarnings( "OptionalGetWithoutIsPresent" )
      @Override
      public Subnet get( ) {
        try {
          final Vpc vpc =
              vpcs.lookupDefault( accountFullName, Functions.identity( ) );
          try {
            subnets.lookupDefault( accountFullName, availabilityZone.get( ), Functions.identity( ) );
            throw new ClientComputeException( "DefaultSubnetAlreadyExistsInAvailabilityZone",
                "Default subnet exists for zone " + availabilityZone.get( ) );
          } catch ( VpcMetadataNotFoundException ignore ) {
          }
          final String vpcCidr = vpc.getCidr( );
          final Set<String> cidrsInUse = Sets.newHashSet( subnets.listByExample(
              Subnet.exampleWithOwner( accountFullName ),
              CollectionUtils.propertyPredicate( vpc.getDisplayName(), Subnets.FilterStringFunctions.VPC_ID ),
              Subnets.FilterStringFunctions.CIDR ) );
          final NetworkAcl networkAcl = networkAcls.lookupDefault( vpc.getDisplayName(), Functions.identity() );
          final List<String> subnetCidrs = Lists.newArrayList( Iterables.transform(
              Cidr.parseUnsafe( ).apply( vpcCidr ).split( 16 ),
              Functions.toStringFunction( ) ) );
          subnetCidrs.removeAll( cidrsInUse );
          final Subnet subnet = subnets.save( Subnet.create(
              vpc.getOwner( ),
              vpc,
              networkAcl,
              Identifier.subnet.generate( ctx.getUser( ) ),
              subnetCidrs.remove( 0 ),
              availabilityZone.get( ) ) );
          subnet.setDefaultForAz( true );
          subnet.setMapPublicIpOnLaunch( true );
          return subnet;
        } catch ( VpcMetadataNotFoundException ex ) {
          throw Exceptions.toUndeclared( new ClientComputeException( "DefaultVpcDoesNotExist", "Default vpc not found" ) );
        } catch ( Exception ex ) {
          throw new RuntimeException( ex );
        }
      }
    };
    reply.setSubnet( allocate( allocator, Subnet.class, SubnetType.class ) );
    invalidate( reply.getSubnet( ).getSubnetId( ) );
    return reply;
  }

  public CreateSubnetResponseType createSubnet( final CreateSubnetType request ) throws EucalyptusCloudException {
    final CreateSubnetResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName();
    final String vpcId = Identifier.vpc.normalize( request.getVpcId( ) );
    final Optional<String> availabilityZone = Iterables.tryFind(
            Clusters.list( ),
            Predicates.and(
                request.getAvailabilityZone( ) == null ?
                    Predicates.<RestrictedType>alwaysTrue( ) :
                    CollectionUtils.propertyPredicate( request.getAvailabilityZone( ), CloudMetadatas.toDisplayName( ) ),
                RestrictedTypes.filterPrivilegedWithoutOwner( ) ) ).transform( CloudMetadatas.toDisplayName( ) );
    final Optional<Cidr> subnetCidr = Cidr.parseLax( ).apply( request.getCidrBlock( ) );
    if ( !subnetCidr.isPresent( ) ) {
      throw new ClientComputeException( "InvalidParameterValue", "Cidr invalid: " + request.getCidrBlock( ) );
    }
    if ( !subnetCidr.transform( Cidr.prefix( ) ).transform( Functions.forPredicate( Range.closed( 16, 28 ) ) ).or( false ) ) {
      throw new ClientComputeException( "InvalidSubnet.Range", "Cidr invalid: " + request.getCidrBlock( ) );
    }
    if ( !availabilityZone.isPresent( ) ) {
      throw new ClientComputeException( "InvalidParameterValue", "Availability zone invalid: " + request.getAvailabilityZone( ) );
    }
    final Supplier<Subnet> allocator = new Supplier<Subnet>( ) {
      @SuppressWarnings( "OptionalGetWithoutIsPresent" )
      @Override
      public Subnet get( ) {
        try {
          final Vpc vpc =
              vpcs.lookupByName( accountFullName, vpcId, Functions.identity( ) );
          final Iterable<Subnet> subnetsInVpc = subnets.listByExample(
              Subnet.exampleWithOwner( accountFullName ),
              CollectionUtils.propertyPredicate( vpc.getDisplayName(), Subnets.FilterStringFunctions.VPC_ID ),
              Functions.identity( ) );
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
          final NetworkAcl networkAcl = networkAcls.lookupDefault( vpc.getDisplayName(), Functions.identity() );
          return subnets.save( Subnet.create(
              ctx.getUserFullName( ),
              vpc,
              networkAcl,
              Identifier.subnet.generate( ctx.getUser( ) ),
              subnetCidr.get( ).toString( ),
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

  public CreateDefaultVpcResponseType createDefaultVpc(
      final CreateDefaultVpcType request
  ) throws EucalyptusCloudException {
    final CreateDefaultVpcResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final UserFullName userFullName = ctx.getUserFullName();
    final AccountFullName accountFullName = userFullName.asAccountFullName();
    final Supplier<Vpc> allocator = new Supplier<Vpc>( ) {
      @Override
      public Vpc get( ) {
        try {
          try {
            vpcs.lookupDefault( accountFullName, Functions.identity() );
            throw new ClientComputeException( "DefaultVpcAlreadyExists", "Default vpc already exists" );
          } catch ( final VpcMetadataNotFoundException ignore ) {
          }
          final String vpcCidr = VpcConfiguration.getDefaultVpcCidr( );
          final UserFullName vpcOwner =
              UserFullName.getInstance( Accounts.lookupPrincipalByAccountNumber( accountFullName.getAccountNumber( ) ) );
          DhcpOptionSet options;
          try {
            options = dhcpOptionSets.lookupByExample(
                DhcpOptionSet.exampleDefault( accountFullName ),
                accountFullName,
                "default",
                Predicates.alwaysTrue(),
                Functions.identity() );
          } catch ( VpcMetadataNotFoundException e ) {
            options = dhcpOptionSets.save( DhcpOptionSet.createDefault(
                vpcOwner,
                Identifier.dopt.generate( ctx.getUser( ) ),
                VmInstances.INSTANCE_SUBDOMAIN ) );
          }
          final Vpc vpc = vpcs.save(
              Vpc.create( vpcOwner, Identifier.vpc.generate( ctx.getUser( ) ), options, vpcCidr, true ) );
          final RouteTable routeTable = routeTables.save(
              RouteTable.create( vpcOwner, vpc, Identifier.rtb.generate( ctx.getUser( ) ), vpc.getCidr(), true ) );
          final NetworkAcl networkAcl = networkAcls.save(
              NetworkAcl.create( vpcOwner, vpc, Identifier.acl.generate( ctx.getUser( ) ), true ) );
          final NetworkGroup group = NetworkGroup.create(
              vpcOwner,
              vpc,
              ResourceIdentifiers.generateString( NetworkGroup.ID_PREFIX ),
              NetworkGroups.defaultNetworkName(),
              "default VPC security group" );
          final Collection<NetworkPeer> peers = Lists.newArrayList(
              NetworkPeer.create( group.getOwnerAccountNumber(), group.getName(), group.getGroupId(), null ) );
          group.addNetworkRules( Lists.newArrayList(
              NetworkRule.create( null/*protocol name*/, -1, null/*low port*/, null/*high port*/, peers, null/*cidrs*/ ),
              NetworkRule.createEgress( null/*protocol name*/, -1, null/*low port*/, null/*high port*/, null/*peers*/, Collections.singleton( NetworkCidr.create(  "0.0.0.0/0" ) ) )
          ) );
          securityGroups.save( group );
          InternetGateway internetGateway = internetGateways.save(
              InternetGateway.create( vpcOwner, Identifier.igw.generate( ctx.getUser( ) ) ) );
          internetGateway.setVpc( vpc );
          routeTable.getRoutes( ).add(
              Route.create( routeTable, Route.RouteOrigin.CreateRoute, "0.0.0.0/0", internetGateway ) );
          final Set<String> zonesWithoutSubnets =
              Sets.newTreeSet( Clusters.stream( ).map( CloudMetadatas.toDisplayName( ) ) );
          final List<String> subnetCidrs = Lists.newArrayList( Iterables.transform(
              Cidr.parseUnsafe( ).apply( vpcCidr ).split( 16 ),
              Functions.toStringFunction( ) ) );
          for ( final String zone : zonesWithoutSubnets ) {
            final Subnet subnet = subnets.save( Subnet.create(
                vpcOwner,
                vpc,
                networkAcl,
                Identifier.subnet.generate( ctx.getUser( ) ),
                subnetCidrs.remove( 0 ),
                zone ) );
            subnet.setDefaultForAz( true );
            subnet.setMapPublicIpOnLaunch( true );
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

  public CreateVpcResponseType createVpc( final CreateVpcType request ) throws EucalyptusCloudException {
    final CreateVpcResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final UserFullName userFullName = ctx.getUserFullName();
    final boolean createDefault = ctx.isAdministrator( ) && request.getCidrBlock( ).matches( "[0-9]{12}" );
    final Optional<Cidr> requestedCidr;
    if ( !createDefault ) {
      requestedCidr = Cidr.parseLax( ).apply( request.getCidrBlock( ) );
      if ( requestedCidr.transform( Vpcs.isReservedVpcCidr( VpcConfiguration.getReservedCidrs( ) ) ).or( true ) ||
          requestedCidr
              .transform( Cidr.prefix( ) )
              .transform( Functions.forPredicate( Predicates.not( Range.closed( 16, 28 ) ) ) ).or( true )
      ) {
        throw new ClientComputeException( "InvalidVpc.Range", "The CIDR '" + request.getCidrBlock( ) + "' is invalid." );
      }
    } else {
      requestedCidr = Optional.absent( );
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
            final UserPrincipal user = Accounts.lookupPrincipalByAccountNumber( request.getCidrBlock( ) );
            vpcCidr = VpcConfiguration.getDefaultVpcCidr( );
            vpcAccountFullName = AccountFullName.getInstance( user.getAccountNumber( ) );
            vpcOwnerFullName = UserFullName.getInstance( user );

            // check for existing vpc
            try {
              vpc = vpcs.lookupDefault( vpcAccountFullName, Functions.identity() );
              routeTable = routeTables.lookupMain( vpc.getDisplayName( ), Functions.identity( ) );
              networkAcl = networkAcls.lookupDefault( vpc.getDisplayName( ), Functions.identity( ) );
            } catch ( final VpcMetadataNotFoundException e ) {
              // so create it
            }
          } else {
            vpcCidr = requestedCidr.get( ).toString( );
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
                  Functions.identity() );
            } catch ( VpcMetadataNotFoundException e ) {
              options = dhcpOptionSets.save( DhcpOptionSet.createDefault(
                  vpcOwnerFullName,
                  Identifier.dopt.generate( ctx.getUser( ) ),
                  VmInstances.INSTANCE_SUBDOMAIN ) );
            }
            vpc =
                vpcs.save( Vpc.create( vpcOwnerFullName, Identifier.vpc.generate( ctx.getUser( ) ), options, vpcCidr, createDefault ) );
            routeTable =
                routeTables.save( RouteTable.create( vpcOwnerFullName, vpc, Identifier.rtb.generate( ctx.getUser( ) ), vpc.getCidr(), true ) );
            networkAcl =
                networkAcls.save( NetworkAcl.create( vpcOwnerFullName, vpc, Identifier.acl.generate( ctx.getUser( ) ), true ) );
            final NetworkGroup group = NetworkGroup.create(
                vpcOwnerFullName,
                vpc,
                ResourceIdentifiers.generateString( NetworkGroup.ID_PREFIX ),
                NetworkGroups.defaultNetworkName(),
                "default VPC security group" );
            final Collection<NetworkPeer> peers = Lists.newArrayList(
                NetworkPeer.create( group.getOwnerAccountNumber(), group.getName(), group.getGroupId(), null ) );
            group.addNetworkRules( Lists.newArrayList(
                NetworkRule.create( null/*protocol name*/, -1, null/*low port*/, null/*high port*/, peers, null/*cidrs*/ ),
                NetworkRule.createEgress( null/*protocol name*/, -1, null/*low port*/, null/*high port*/, null/*peers*/, Collections.singleton( NetworkCidr.create(  "0.0.0.0/0" ) ) )
            ) );
            securityGroups.save( group );
          }
          if ( createDefault && routeTable != null && networkAcl != null ) {
            // ensure there is an internet gateway for the vpc and a route in place
            InternetGateway internetGateway;
            try {
              internetGateway =
                  internetGateways.lookupByVpc( vpcAccountFullName, vpc.getDisplayName(), Functions.identity( ) );
            } catch ( final VpcMetadataNotFoundException e ) {
              internetGateway = internetGateways.save( InternetGateway.create( vpcOwnerFullName, Identifier.igw.generate( ctx.getUser( ) ) ) );
              internetGateway.setVpc( vpc );
            }

            final Optional<Route> defaultRoute =
                Iterables.tryFind( routeTable.getRoutes( ), CollectionUtils.propertyPredicate(
                    "0.0.0.0/0",
                    RouteTables.RouteFilterStringFunctions.DESTINATION_CIDR ) );

            if ( !defaultRoute.isPresent( ) ) {
              routeTable.getRoutes( ).add( Route.create( routeTable, Route.RouteOrigin.CreateRoute, "0.0.0.0/0", internetGateway ) );
              routeTable.updateTimeStamps( ); // ensure version of table increments also
            }

            // ensure there is a default subnet in each availability zone
            final Set<String> cidrsInUse = Sets.newHashSet( );
            final Set<String> zonesWithoutSubnets = Sets.newTreeSet( );
            for ( final String zone : Clusters.stream( ).map( CloudMetadatas.toDisplayName( ) ) ) {
              try {
                cidrsInUse.add( subnets.lookupDefault( vpcAccountFullName, zone, Subnets.FilterStringFunctions.CIDR ) );
              } catch ( final VpcMetadataNotFoundException e ) {
                zonesWithoutSubnets.add( zone );
              }
            }
            final List<String> subnetCidrs = Lists.newArrayList( Iterables.transform(
                Cidr.parseUnsafe( ).apply( vpcCidr ).split( 16 ),
                Functions.toStringFunction( ) ) );
            subnetCidrs.removeAll( cidrsInUse );
            for ( final String zone : zonesWithoutSubnets ) {
              final Subnet subnet = subnets.save( Subnet.create(
                  vpcOwnerFullName,
                  vpc,
                  networkAcl,
                  Identifier.subnet.generate( ctx.getUser( ) ),
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
              Functions.identity( ) );
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
              Functions.identity( ) );
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

  public DeleteNatGatewayResponseType deleteNatGateway( final DeleteNatGatewayType request ) throws EucalyptusCloudException {
    final DeleteNatGatewayResponseType reply = request.getReply( );
    final String natGatewayId = Identifier.nat.normalize( request.getNatGatewayId( ) );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.isAdministrator( ) ? null : ctx.getUserFullName( ).asAccountFullName( );
    try {
      natGateways.updateByExample(
          NatGateway.exampleWithName( accountFullName, natGatewayId ),
          accountFullName,
          natGatewayId,
          new Callback<NatGateway>( ) {
            @Override
            public void fire( final NatGateway natGateway ) {
              if ( RestrictedTypes.filterPrivileged( ).apply( natGateway ) ) {
                try {
                  if ( natGateway.getState( ) == NatGateway.State.available ||
                      natGateway.getState( ) == NatGateway.State.pending ) {
                    natGateway.setState( NatGateway.State.deleting );
                    natGateway.markDeletion( );
                  } else if ( natGateway.getState( ) == NatGateway.State.failed ) {
                    natGateway.setState( NatGateway.State.deleted );
                  } else { // failed, deleted or deleting
                    if ( ctx.isAdministrator( ) ) {
                      // allow administrator to force immediate deletion of NAT gateway
                      final Optional<NetworkInterface> networkInterface = NatGatewayHelper.cleanupResources( natGateway );
                      if ( networkInterface.isPresent( ) ) {
                        networkInterfaces.delete( networkInterface.get( ) );
                      }
                      natGateways.delete( natGateway );
                    }
                  }
                } catch ( Exception e ) {
                  throw Exceptions.toUndeclared( e );
                }
              } else {
                throw Exceptions.toUndeclared( new ClientUnauthorizedComputeException( "Not authorized to delete nat gateway" ) );
              }
            }
          }
      );
    } catch ( final Exception e ) {
      if ( Exceptions.isCausedBy( e, VpcMetadataNotFoundException.class ) ) {
        throw new ClientComputeException( "NatGatewayNotFound", "The Nat Gateway "+natGatewayId+" was not found" );
      } else {
        throw handleException( e );
      }
    }
    reply.setNatGatewayId( request.getNatGatewayId( ) );
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
              Functions.identity( ) );
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
      networkAcls.withRetries( ).updateByExample(
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
    } catch ( VpcMetadataNotFoundException e ) {
      throw new ClientComputeException(
          "InvalidNetworkAclEntry.NotFound",
          "Entry not found for number " + request.getRuleNumber( ) + " in network acl " + networkAclId );
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
              Functions.identity( ) );
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
    final String destinationCidr = request.getDestinationCidrBlock( );
    final Optional<Cidr> destinationCidrOption = Cidr.parseLax( ).apply( destinationCidr );
    if ( !destinationCidrOption.isPresent( ) ) {
      throw new ClientComputeException( "InvalidParameterValue", "Cidr invalid: " + destinationCidr );
    }
    final String routeCidr = destinationCidrOption.get( ).toString( );
    try {
      routeTables.withRetries( ).updateByExample(
          RouteTable.exampleWithName( accountFullName, routeTableId ),
          accountFullName,
          request.getRouteTableId( ),
          new Callback<RouteTable>() {
            @Override
            public void fire( final RouteTable routeTable ) {
              try {
                if ( RestrictedTypes.filterPrivileged( ).apply( routeTable ) ) {
                  final Optional<Route> routeOption = Iterables.tryFind(
                      routeTable.getRoutes( ),
                      CollectionUtils.propertyPredicate( routeCidr, RouteTables.RouteFilterStringFunctions.DESTINATION_CIDR ) );
                  if ( routeOption.isPresent( ) ) {
                    final Route route = routeOption.get( );
                    if ( route.getDestinationCidr( ).equals( routeTable.getVpc( ).getCidr( ) ) ) {
                      throw new ClientComputeException(
                          "InvalidParameterValue", "Cannot remove local route "+destinationCidr+" in route table " + routeTableId );
                    }
                    routeTable.getRoutes( ).remove( route );
                    routeTable.updateTimeStamps( ); // ensure version of table increments also
                  } else {
                    throw new ClientComputeException( "InvalidRoute.NotFound", "Route not found for cidr: " + destinationCidr );
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
    } catch ( VpcMetadataNotFoundException e ) {
      throw new ClientComputeException(
          "InvalidRoute.NotFound", "no route with destination-cidr-block "+routeCidr+" in route table " + routeTableId );
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
              Functions.identity( ) );
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
              Functions.identity( ) );
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
              Functions.identity( ) );
          if ( RestrictedTypes.filterPrivileged( ).apply( vpc ) ) {
            if ( Boolean.TRUE.equals( vpc.getDefaultVpc( ) ) &&  Contexts.lookup( ).isAdministrator( ) ) {
              final List<Subnet> defaultSubnets = subnets.listByExample(
                  Subnet.exampleDefault( AccountFullName.getInstance( vpc.getOwnerAccountNumber( ) ), null ),
                  Predicates.alwaysTrue( ),
                  Functions.identity( ) );
              for ( final Subnet subnet : defaultSubnets ) {
                subnets.delete( subnet );
              }
              try {
                final InternetGateway internetGateway =
                    internetGateways.lookupByVpc( null, vpc.getDisplayName( ), Functions.identity( ) );
                internetGateways.delete( internetGateway );
              } catch ( VpcMetadataNotFoundException e ) { /* so no need to delete */ }
            }
            try {
              networkAcls.delete( networkAcls.lookupDefault( vpc.getDisplayName(), Functions.identity() ) );
            } catch ( VpcMetadataNotFoundException e ) { /* so no need to delete */ }
            try {
              routeTables.delete( routeTables.lookupMain( vpc.getDisplayName(), Functions.identity() ) );
            } catch ( VpcMetadataNotFoundException e ) { /* so no need to delete */ }
            try {
              securityGroups.delete( securityGroups.lookupDefault( vpc.getDisplayName(), Functions.identity() ) );
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
                final Vpc vpc = vpcs.lookupByName( accountFullName, vpcId, Functions.identity( ) );
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

  public DetachNetworkInterfaceResponseType detachNetworkInterface(
      final DetachNetworkInterfaceType request
  ) throws EucalyptusCloudException {
    final DetachNetworkInterfaceResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    final String attachmentId = Identifier.eni_attach.normalize( request.getAttachmentId( ) );
    final String[] vpcId = new String[1];
    try {
      final NetworkInterface eni = networkInterfaces.updateByExample(
          NetworkInterface.exampleWithAttachment( accountFullName, attachmentId ),
          accountFullName,
          request.getAttachmentId( ),
          new Callback<NetworkInterface>() {
            @Override
            public void fire( final NetworkInterface networkInterface ) {
              if ( RestrictedTypes.filterPrivileged( ).apply( networkInterface ) ) try {
                if ( Integer.valueOf( 0 ).equals( networkInterface.getAttachment( ).getDeviceIndex( ) ) ) {
                  throw new ClientComputeException( "OperationNotPermitted",
                      "Primary network interface cannot be detached" );
                }
                if ( networkInterface.getType( ) == NetworkInterface.Type.NatGateway ) {
                  throw Exceptions.toUndeclared( new ClientComputeException(
                      "OperationNotPermitted", "NAT gateway network interface cannot be detached" ) );
                }
                if ( networkInterface.isAttached( ) &&
                    ( networkInterface.getAttachment( ).getStatus( ) == NetworkInterfaceAttachment.Status.attached ||
                      networkInterface.getAttachment( ).getStatus( ) == NetworkInterfaceAttachment.Status.attaching ) ) {
                  if ( VmInstance.VmState.PENDING.apply( networkInterface.getInstance( ) ) ) {
                    throw new ClientComputeException(
                        "IncorrectState", "The instance is not in a valid state for this operation" );
                  } else if ( VmInstance.VmStateSet.TORNDOWN.apply( networkInterface.getInstance( ) ) ) {
                    networkInterface.detach( );
                  } else { // mark detaching and process on vm state callback
                    vpcId[0] = networkInterface.getVpc( ).getDisplayName( );
                    networkInterface.getAttachment( ).transitionStatus( NetworkInterfaceAttachment.Status.detaching );
                    networkInterface.getInstance( ).updateTimeStamps( ); // mark dirty
                  }
                }
              } catch ( Exception e ) {
                throw Exceptions.toUndeclared( e );
              } else {
                throw Exceptions.toUndeclared(
                    new ClientUnauthorizedComputeException( "Not authorized to detach network interface" ) );
              }
            }
          } );
      if ( vpcId[0] != null ) {
        NetworkInterfaceHelper.stop( eni );
        invalidate( vpcId[0] );
      }
    } catch ( VpcMetadataNotFoundException e ) {
      throw new ClientComputeException( "InvalidAttachmentID.NotFound",
          "Network interface attachment ("+request.getAttachmentId()+") not found " );
    } catch ( Exception e ) {
      throw handleException( e );
    }
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
                if ( networkInterface.getType( ) == NetworkInterface.Type.NatGateway ) {
                  throw Exceptions.toUndeclared( new ClientComputeException(
                      "OperationNotPermitted", "NAT gateway network interface attribute modification not permitted" ) );
                }
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
                    if ( !Collections.singleton( networkInterface.getVpc( ).getDisplayName( ) ).equals(
                        Sets.newHashSet( Iterables.transform( networkInterface.getNetworkGroups( ), NetworkGroup.vpcId( ) ) ) ) ) {
                      throw Exceptions.toUndeclared( new ClientComputeException( "InvalidParameterValue", "Invalid security groups (inconsistent VPC)" ) );
                    }
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
                final NetworkAcl networkAcl = networkAcls.lookupByName( accountFullName, networkAclId, Functions.identity( ) );
                if ( !subnet.getVpc( ).getDisplayName( ).equals( networkAcl.getVpc( ).getDisplayName( ) ) ) {
                  throw new ClientComputeException( "InvalidParameterValue",
                      "Network ACL "+networkAclId+" and subnet "+subnet.getDisplayName( )+" belong to different networks" );
                }
                subnet.setNetworkAcl( networkAcl );
                subnet.setNetworkAclAssociationId( Identifier.aclassoc.generate( ctx.getUser( ) ) );
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
    final Optional<Cidr> cidrOptional = Cidr.parseLax( ).apply( cidr );
    if ( !cidrOptional.isPresent( ) ) {
      throw new ClientComputeException( "InvalidParameterValue", "Cidr invalid: " + cidr );
    }
    final String aclCidr = cidrOptional.get( ).toString( );
    final Optional<Integer> protocolOptional = protocolNumber( request.getProtocol() );
    if ( !protocolOptional.isPresent( ) ) {
      throw new ClientComputeException( "InvalidParameterValue", "Protocol invalid: " + request.getProtocol( ) );
    }
    if ( !Range.closed( 1, 32766 ).contains( request.getRuleNumber( ) ) ) {
      throw new ClientComputeException( "InvalidParameterValue", "Rule number invalid: " + request.getRuleNumber( ) );
    }
    try {
      networkAcls.updateByExample(
          NetworkAcl.exampleWithName( accountFullName, networkAclId ),
          accountFullName,
          request.getNetworkAclId(),
          new Callback<NetworkAcl>( ) {
            @SuppressWarnings( "OptionalGetWithoutIsPresent" )
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
                        FUtils.valueOfFunction( NetworkAclEntry.RuleAction.class ).apply( request.getRuleAction() ),
                        request.getEgress(),
                        aclCidr,
                        request.getIcmpTypeCode().getCode(),
                        request.getIcmpTypeCode().getType() );
                    break;
                  case 6:
                  case 17:
                    entry = NetworkAclEntry.createTcpUdpEntry(
                        networkAcl,
                        request.getRuleNumber(),
                        protocolOptional.get(),
                        FUtils.valueOfFunction( NetworkAclEntry.RuleAction.class ).apply( request.getRuleAction() ),
                        request.getEgress(),
                        aclCidr,
                        request.getPortRange().getFrom(),
                        request.getPortRange().getTo() );
                    break;
                  default:
                    entry = NetworkAclEntry.createEntry(
                        networkAcl,
                        request.getRuleNumber( ),
                        protocolOptional.get(),
                        FUtils.valueOfFunction( NetworkAclEntry.RuleAction.class ).apply( request.getRuleAction( ) ),
                        request.getEgress( ),
                        aclCidr );
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
    final String gatewayId = request.getGatewayId( ) == null ?
        null : Identifier.igw.normalize( request.getGatewayId( ) );
    final String natGatewayId = request.getNatGatewayId( ) == null ?
        null : Identifier.nat.normalize( request.getNatGatewayId( ) );
    final String instanceId = request.getInstanceId( ) == null ?
        null : Identifier.i.normalize( request.getInstanceId( ) );
    final String networkInterfaceId = request.getNetworkInterfaceId( ) == null ?
        null : Identifier.eni.normalize( request.getNetworkInterfaceId( ) );
    final String routeTableId = Identifier.rtb.normalize( request.getRouteTableId( ) );
    final String destinationCidr = request.getDestinationCidrBlock( );
    final Optional<Cidr> destinationCidrOption = Cidr.parseLax( ).apply( destinationCidr );
    if ( !destinationCidrOption.isPresent( ) ) {
      throw new ClientComputeException( "InvalidParameterValue", "Cidr invalid: " + destinationCidr );
    }
    final String routeCidr = destinationCidrOption.get( ).toString( );
    if ( gatewayId == null && natGatewayId == null && instanceId == null && networkInterfaceId == null ) {
      throw new ClientComputeException( "InvalidParameterCombination", "Route target required" );
    }
    try {
      routeTables.withRetries( ).updateByExample(
          RouteTable.exampleWithName( accountFullName, routeTableId ),
          accountFullName,
          request.getRouteTableId( ),
          new Callback<RouteTable>( ) {
            @Override
            public void fire( final RouteTable routeTable ) {
              if ( RestrictedTypes.filterPrivileged( ).apply( routeTable ) ) try {
                final NetworkInterface networkInterface = networkInterfaceId == null ? null :
                    networkInterfaces.lookupByName( accountFullName, networkInterfaceId, Functions.identity( ) );
                final VmInstance instance = instanceId == null ? null :
                    VmInstances.lookup( instanceId );
                final InternetGateway internetGateway = gatewayId == null ? null :
                    internetGateways.lookupByName( accountFullName, gatewayId, Functions.identity() );
                final NatGateway natGateway = natGatewayId == null ? null :
                    natGateways.lookupByName( accountFullName, natGatewayId, Functions.identity() );

                final List<Route> routes = routeTable.getRoutes( );
                final Optional<Route> oldRoute =
                    Iterables.tryFind( routes, CollectionUtils.propertyPredicate(
                        routeCidr,
                        RouteTables.RouteFilterStringFunctions.DESTINATION_CIDR ) );

                final Route route = createRouteForTarget(
                    routeTable, routeCidr, networkInterface, instance, natGateway, internetGateway, gatewayId );

                if ( !oldRoute.isPresent( ) ) {
                  throw new ClientComputeException(
                      "InvalidRoute.NotFound",
                      "Route not found for cidr: " + destinationCidr );
                }

                routes.set( routes.indexOf( oldRoute.get( ) ), route );
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
                final RouteTable newRouteTable = routeTables.lookupByName( accountFullName, routeTableId, Functions.identity( ) );
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

  private enum LongIdStyle { Always, Configurable, Never }

  private enum Identifier {
    acl( "networkAcl" ),
    aclassoc( "networkAclAssociation" ),
    dopt( "DHCPOption" ),
    eipalloc( "allocation" ),
    ela_attach( "networkInterfaceAttachment" ),
    eni( "networkInterface" ),
    eni_attach( "networkInterfaceAttachment" ),
    i( "instance" ),
    igw( "internetGateway" ),
    nat( "natGateway", LongIdStyle.Always ),
    rtb( "routeTable" ),
    rtbassoc( "routeTableAssociation" ),
    sg( "securityGroup" ),
    subnet( "subnet" ),
    vpc( "vpc" ),
    ;

    private final String code;
    private final LongIdStyle longIdStyle;
    private final String defaultParameter;
    private final String defaultListParameter;

    Identifier( final String defaultParameter ) {
      this( defaultParameter, LongIdStyle.Configurable );
    }

    Identifier( final String defaultParameter, final LongIdStyle longIdStyle ) {
      this.code = "InvalidParameterValue";
      this.longIdStyle = longIdStyle;
      this.defaultParameter = defaultParameter;
      this.defaultListParameter = defaultParameter + "s";
    }

    private String prefix( ) {
      return name( ).replace( '_', '-' );
    }

    public String generate( final UserPrincipal identity ) {
      switch ( longIdStyle ) {
        case Never:
          return ResourceIdentifiers.generateShortString( prefix( ) );
        case Always:
          return ResourceIdentifiers.generateLongString( prefix( ) );
        case Configurable:
          return ResourceIdentifiers.generateString( prefix( ) );
      }
      throw new IllegalStateException( "Unexpected long identity value " + longIdStyle );
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
        return ResourceIdentifiers.normalize( prefix( ), identifiers );
      } catch ( final InvalidResourceIdentifier e ) {
        throw new ClientComputeException(
            code,
            "Value ("+e.getIdentifier()+") for parameter "+parameter+" is invalid. Expected: '"+prefix()+"-...'." );
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

  @SuppressWarnings( "WeakerAccess" )
  protected <E extends AbstractPersistent> Supplier<E> transactional( final Supplier<E> supplier ) {
    return Entities.asTransaction( supplier );
  }

  @SuppressWarnings( "WeakerAccess" )
  protected <E extends AbstractPersistent,P> Function<P,E> transactional( final Function<P,E> function ) {
    return Entities.asTransaction( function );
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
            Predicates.alwaysTrue( ) :
            CollectionUtils.propertyPredicate(
              ruleNumber,
              NetworkAcls.NetworkAclEntryFilterIntegerFunctions.RULE_NUMBER ),
        CollectionUtils.propertyPredicate(
            egress,
            NetworkAcls.NetworkAclEntryFilterBooleanFunctions.EGRESS )
    );
  }

  @Nonnull
  private static Route createRouteForTarget(
      @Nonnull  final RouteTable routeTable,
      @Nonnull  final String destinationCidr,
      @Nullable final NetworkInterface networkInterface,
      @Nullable final VmInstance instance,
      @Nullable final NatGateway natGateway,
      @Nullable final InternetGateway internetGateway,
      @Nullable final String gatewayId
  ) throws ClientComputeException {
    NetworkInterface targetNetworkInterface = networkInterface;
    if ( instance != null ) {
      final List<NetworkInterface> enis = instance.getNetworkInterfaces( );
      if ( enis.size() != 1 ) {
        throw new ClientComputeException(
            "InvalidInstanceID",
            "Network interface not found for " + instance.getDisplayName( ) );
      }
      targetNetworkInterface = enis.get( 0 );
    }

    final Route route;
    if ( targetNetworkInterface != null ) {
      if ( !RestrictedTypes.filterPrivileged( ).apply( targetNetworkInterface ) ||
          !targetNetworkInterface.getVpc( ).getDisplayName( ).equals( routeTable.getVpc( ).getDisplayName( ) ) ) {
        throw new ClientComputeException(
            "InvalidParameterValue",
            "Network interface invalid: " + targetNetworkInterface.getDisplayName( ) );
      }
      route = Route.create( routeTable, Route.RouteOrigin.CreateRoute, destinationCidr, targetNetworkInterface );
    } else if ( natGateway != null ) {
      if ( !RestrictedTypes.filterPrivileged( ).apply( natGateway ) ||
          !natGateway.getVpcId( ).equals( routeTable.getVpc( ).getDisplayName( ) ) ) {
        throw new ClientComputeException(
            "InvalidParameterValue",
            "NAT gateway invalid: " + natGateway.getDisplayName( ) );
      }
      route = Route.create( routeTable, Route.RouteOrigin.CreateRoute, destinationCidr, natGateway );
    } else {
      if ( internetGateway== null || internetGateway.getVpc( )==null ||
          !internetGateway.getVpc( ).getDisplayName( ).equals( routeTable.getVpc( ).getDisplayName( ) ) ) {
        throw new ClientComputeException(
            "InvalidParameterValue",
            "Internet gateway invalid: " + gatewayId );
      }
      route = Route.create( routeTable, Route.RouteOrigin.CreateRoute, destinationCidr, internetGateway );
    }

    return route;
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
