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
package com.eucalyptus.compute.common.internal.vpc;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.hibernate.annotations.Type;
import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.auth.type.RestrictedType;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.CloudMetadata.NatGatewayMetadata;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.util.RestrictedTypes;
import com.google.common.base.Function;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_nat_gateways", indexes = {
    @Index( name = "metadata_nat_gateways_user_id_idx", columnList = "metadata_user_id" ),
    @Index( name = "metadata_nat_gateways_account_id_idx", columnList = "metadata_account_id" ),
    @Index( name = "metadata_nat_gateways_display_name_idx", columnList = "metadata_display_name" ),
}  )
public class NatGateway extends UserMetadata<NatGateway.State> implements NatGatewayMetadata {

  private static final long serialVersionUID = 1L;

  public enum State {
    pending,
    failed,
    available,
    deleting,
    deleted,
    ;
  }

  @Temporal( TemporalType.TIMESTAMP)
  @Column( name = "deletion_timestamp" )
  private Date deletionTimestamp;

  @Column( name = "metadata_client_token", updatable = false )
  private String clientToken;

  @Column( name = "metadata_client_token_unique", unique = true, updatable = false )
  private String uniqueClientToken;

  @ManyToOne
  @JoinColumn( name = "metadata_vpc_id" )
  private Vpc vpc;

  @Column( name = "metadata_vpc_name", nullable = false, updatable = false )
  private String vpcId;

  @ManyToOne
  @JoinColumn( name = "metadata_subnet_id" )
  private Subnet subnet;

  @Column( name = "metadata_subnet_name", nullable = false, updatable = false )
  private String subnetId;

  @OneToOne( fetch = FetchType.LAZY )
  @JoinColumn( name = "metadata_network_interface_id" )
  private NetworkInterface networkInterface;

  @Column( name = "metadata_network_interface_name" )
  private String networkInterfaceId;

  @Column( name = "metadata_failure_code" )
  private String failureCode;

  @Column( name = "metadata_failure_message" )
  @Type(type="text")
  private String failureMessage;

  @Column( name = "metadata_allocation_id", nullable = false, updatable = false )
  private String allocationId;

  @Column( name = "metadata_association_id" )
  private String associationId;

  @Column( name = "metadata_mac_address" )
  private String macAddress;

  @Column( name = "metadata_private_ip" )
  private String privateIpAddress;

  @Column( name = "metadata_public_ip" )
  private String publicIpAddress;

  protected NatGateway( ) {
  }

  public static NatGateway create( final OwnerFullName owner,
                                   final Vpc vpc,
                                   final Subnet subnet,
                                   final String displayName,
                                   final String clientToken,
                                   final String allocationId ) {
    final NatGateway natGateway = new NatGateway( );
    natGateway.setOwner( owner );
    natGateway.setDisplayName( displayName );
    if ( clientToken != null ) {
      natGateway.setClientToken( clientToken );
      natGateway.setUniqueClientToken( uniqueClientToken( owner, clientToken ) );
    }
    natGateway.setVpc( vpc );
    natGateway.setSubnet( subnet );
    natGateway.setAllocationId( allocationId );
    natGateway.setState( State.pending );
    natGateway.updateTimeStamps( );
    return natGateway;
  }

  public static NatGateway exampleWithOwner( final OwnerFullName owner ) {
    final NatGateway example = new NatGateway( );
    example.setOwner( owner );
    return example;
  }

  public static NatGateway exampleWithName( final OwnerFullName owner, final String name ) {
    final NatGateway example = exampleWithOwner( owner );
    example.setDisplayName( name );
    return example;
  }

  public static NatGateway exampleWithClientToken( final OwnerFullName owner, final String clientToken ) {
    final NatGateway example = new NatGateway( );
    example.setUniqueClientToken( uniqueClientToken( owner, clientToken ) );
    return example;
  }

  public static NatGateway exampleWithState( final State state ) {
    final NatGateway example = new NatGateway( );
    example.setState( state );
    example.setLastState( null );
    example.setStateChangeStack( null );
    return example;
  }

  public void attach( final NetworkInterface networkInterface ) {
    if ( getNetworkInterface( ) != null ) {
      throw new IllegalStateException( "Already attached" );
    }
    setNetworkInterface( networkInterface );
    setNetworkInterfaceId( RestrictedTypes.toDisplayName( ).apply( networkInterface ) );
    if ( networkInterface.isAssociated( ) ) {
      associate(
          networkInterface.getAssociation( ).getPublicIp( ),
          networkInterface.getAssociation( ).getAssociationId( )
      );
    }
    setMacAddress( networkInterface.getMacAddress( ) );
    setPrivateIpAddress( networkInterface.getPrivateIpAddress( ) );
  }

  public void associate( final String publicIp, final String associationId ) {
    if ( getPublicIpAddress( ) != null ) {
      throw new IllegalStateException( "Already associated" );
    }
    setPublicIpAddress( publicIp );
    setAssociationId( associationId );
  }

  @Override
  public String getPartition( ) {
    return "eucalyptus";
  }

  @Override
  public FullName getFullName( ) {
    return FullName.create.vendor( "euca" )
        .region( ComponentIds.lookup( Eucalyptus.class ).name( ) )
        .namespace( getOwnerAccountNumber() )
        .relativeId( "nat-gateway", getDisplayName( ) );
  }

  public Vpc getVpc( ) {
    return vpc;
  }

  public void setVpc( final Vpc vpc ) {
    this.vpc = vpc;
  }

  public String getVpcId( ) {
    return vpcId;
  }

  public void setVpcId( final String vpcId ) {
    this.vpcId = vpcId;
  }

  public Subnet getSubnet( ) {
    return subnet;
  }

  public void setSubnet( final Subnet subnet ) {
    this.subnet = subnet;
  }

  public String getSubnetId( ) {
    return subnetId;
  }

  public void setSubnetId( final String subnetId ) {
    this.subnetId = subnetId;
  }

  public NetworkInterface getNetworkInterface( ) {
    return networkInterface;
  }

  public void setNetworkInterface( final NetworkInterface networkInterface ) {
    this.networkInterface = networkInterface;
  }

  public String getNetworkInterfaceId( ) {
    return networkInterfaceId;
  }

  public void setNetworkInterfaceId( final String networkInterfaceId ) {
    this.networkInterfaceId = networkInterfaceId;
  }

  public Date getDeletionTimestamp( ) {
    return deletionTimestamp;
  }

  public void setDeletionTimestamp( final Date deletionTimestamp ) {
    this.deletionTimestamp = deletionTimestamp;
  }

  public String getFailureCode( ) {
    return failureCode;
  }

  public void setFailureCode( final String failureCode ) {
    this.failureCode = failureCode;
  }

  public String getFailureMessage( ) {
    return failureMessage;
  }

  public void setFailureMessage( final String failureMessage ) {
    this.failureMessage = failureMessage;
  }

  public String getAllocationId( ) {
    return allocationId;
  }

  public void setAllocationId( final String allocationId ) {
    this.allocationId = allocationId;
  }

  public String getAssociationId( ) {
    return associationId;
  }

  public void setAssociationId( final String associationId ) {
    this.associationId = associationId;
  }

  public String getClientToken( ) {
    return clientToken;
  }

  public void setClientToken( final String clientToken ) {
    this.clientToken = clientToken;
  }

  public String getUniqueClientToken( ) {
    return uniqueClientToken;
  }

  public void setUniqueClientToken( final String uniqueClientToken ) {
    this.uniqueClientToken = uniqueClientToken;
  }

  public String getMacAddress( ) {
    return macAddress;
  }

  public void setMacAddress( final String macAddress ) {
    this.macAddress = macAddress;
  }

  public String getPrivateIpAddress( ) {
    return privateIpAddress;
  }

  public void setPrivateIpAddress( final String privateIpAddress ) {
    this.privateIpAddress = privateIpAddress;
  }

  public String getPublicIpAddress( ) {
    return publicIpAddress;
  }

  public void setPublicIpAddress( final String publicIpAddress ) {
    this.publicIpAddress = publicIpAddress;
  }

  public void markDeletion( ) {
    if ( getDeletionTimestamp( ) == null ) {
      setDeletionTimestamp( new Date( ) );
    }
  }

  @Override
  protected String createUniqueName( ) {
    return getDisplayName( );
  }

  @PrePersist
  @PreUpdate
  protected void updateReferences( ) {
    // not all reference updates will actually be persisted (as per field annotations)
    final Function<RestrictedType, String> nameTransform =  RestrictedTypes.toDisplayName( );
    setVpcId( nameTransform.apply( getVpc( ) ) );
    setSubnetId( nameTransform.apply( getSubnet( ) ) );
    final NetworkInterface networkInterface = getNetworkInterface( );
    if ( networkInterface != null ) {
      setNetworkInterfaceId( nameTransform.apply( getNetworkInterface( ) ) );
    }
  }

  private static String uniqueClientToken( final OwnerFullName owner, final String clientToken ) {
    return String.format( "%s:%s", owner.getAccountNumber( ), clientToken );
  }
}
