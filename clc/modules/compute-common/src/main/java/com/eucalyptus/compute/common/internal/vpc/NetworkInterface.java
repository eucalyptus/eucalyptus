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

import static org.hamcrest.Matchers.notNullValue;
import static com.eucalyptus.compute.common.CloudMetadata.NetworkInterfaceMetadata;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import org.apache.log4j.Logger;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.internal.network.NetworkGroup;
import com.eucalyptus.compute.common.internal.network.PrivateAddressReferrer;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.upgrade.Upgrades;
import com.eucalyptus.util.Parameters;
import com.google.common.base.CaseFormat;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_network_interfaces", indexes = {
    @Index( name = "metadata_network_interfaces_user_id_idx", columnList = "metadata_user_id" ),
    @Index( name = "metadata_network_interfaces_account_id_idx", columnList = "metadata_account_id" ),
    @Index( name = "metadata_network_interfaces_display_name_idx", columnList = "metadata_display_name" ),
    @Index( name = "metadata_network_interfaces_instance_idx", columnList = "metadata_instance_id" ),
}  )
public class NetworkInterface extends UserMetadata<NetworkInterface.State> implements NetworkInterfaceMetadata, PrivateAddressReferrer {

  private static final long serialVersionUID = 1L;

  public enum State {
    available,
    in_use("in-use"),
    attaching, // incorrect state, do not use
    detaching, // incorrect state, do not use
    ;

    private final String value;

    private State( ) {
      this.value = name( );
    }

    private State( String value ) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }
  }

  public enum Type {
    NatGateway,
    Interface,
    ;

    private final String value;

    private Type( ) {
      this.value = CaseFormat.UPPER_CAMEL.to( CaseFormat.LOWER_CAMEL, name( ) );
    }

    @Override
    public String toString() {
      return value;
    }
  }

  protected NetworkInterface( ) {
  }

  protected NetworkInterface( final OwnerFullName owner, final String displayName ) {
    super( owner, displayName );
  }

  /**
   * Create a regular network interface.
   */
  public static NetworkInterface create( final OwnerFullName owner,
                                         final Vpc vpc,
                                         final Subnet subnet,
                                         final Set<NetworkGroup> networkGroups,
                                         final String displayName,
                                         final String macAddress,
                                         final String privateIp,
                                         final String privateDnsName,
                                         final String description ) {
    final NetworkInterface networkInterface = create(
        owner,
        vpc,
        subnet,
        Type.Interface,
        displayName,
        macAddress,
        privateIp,
        privateDnsName,
        description );
    networkInterface.setNetworkGroups( networkGroups );
    return networkInterface;
  }


  /**
   * Create a network interface for use with a NAT gateway
   */
  public static NetworkInterface create( final OwnerFullName owner,
                                         final String requesterId,
                                         final Vpc vpc,
                                         final Subnet subnet,
                                         final String displayName,
                                         final String macAddress,
                                         final String privateIp,
                                         final String privateDnsName,
                                         final String description ) {
    Parameters.checkParam( "requesterId", requesterId, notNullValue( ) );
    final NetworkInterface networkInterface = create(
        owner,
        vpc,
        subnet,
        Type.NatGateway,
        displayName,
        macAddress,
        privateIp,
        privateDnsName,
        description );
    networkInterface.setSourceDestCheck( false );
    networkInterface.setRequesterId( requesterId );
    networkInterface.setRequesterManaged( true );
    return networkInterface;
  }

  private static NetworkInterface create( final OwnerFullName owner,
                                          final Vpc vpc,
                                          final Subnet subnet,
                                          final Type type,
                                          final String displayName,
                                          final String macAddress,
                                          final String privateIp,
                                          final String privateDnsName,
                                          final String description ) {
    Parameters.checkParam( "owner", owner, notNullValue( ) );
    Parameters.checkParam( "vpc", vpc, notNullValue( ) );
    Parameters.checkParam( "subnet", subnet, notNullValue( ) );
    Parameters.checkParam( "type", type, notNullValue( ) );
    Parameters.checkParam( "displayName", displayName, notNullValue( ) );
    final NetworkInterface networkInterface = new NetworkInterface( owner, displayName );
    networkInterface.setVpc( vpc );
    networkInterface.setSubnet( subnet );
    networkInterface.setType( type );
    networkInterface.setAvailabilityZone( subnet.getAvailabilityZone( ) );
    networkInterface.setDescription( description );
    networkInterface.setState( State.available );
    networkInterface.setSourceDestCheck( true );
    networkInterface.setRequesterManaged( false );
    networkInterface.setMacAddress( macAddress );
    networkInterface.setPrivateIpAddress( privateIp );
    networkInterface.setPrivateDnsName( privateDnsName );
    return networkInterface;
  }

  public static NetworkInterface exampleWithOwner( final OwnerFullName owner ) {
    return new NetworkInterface( owner, null );
  }

  public static NetworkInterface exampleWithName( final OwnerFullName owner, final String name ) {
    return new NetworkInterface( owner, name );
  }

  public static NetworkInterface exampleWithAttachment( final OwnerFullName owner, final String attachmentId ) {
    final NetworkInterface example = new NetworkInterface( owner, null );
    example.attachment = new NetworkInterfaceAttachment( );
    example.attachment.setAttachmentId( attachmentId );
    return example;
  }

  @ManyToOne( optional = false )
  @JoinColumn( name = "metadata_vpc_id", nullable = false, updatable = false )
  private Vpc vpc;

  @ManyToOne( optional = false )
  @JoinColumn( name = "metadata_subnet_id", nullable = false, updatable = false )
  private Subnet subnet;

  // Due to an issue with mappedBy on VmNetworkConfig we define the instance
  // reference here rather than on the embedded attachment where it belongs
  @ManyToOne( fetch = FetchType.LAZY )
  @JoinColumn( name = "metadata_instance_id" )
  private VmInstance instance;

  @NotFound( action = NotFoundAction.IGNORE )
  @JoinTable( name = "metadata_network_interfaces_groups",
      joinColumns =        @JoinColumn( name = "networkinterface_id" ),
      inverseJoinColumns = @JoinColumn( name = "networkgroup_id" ) )
  @ManyToMany
  private Set<NetworkGroup> networkGroups = Sets.newHashSet();

  @Column( name = "metadata_zone", nullable = false, updatable = false )
  private String availabilityZone;

  @Column( name = "metadata_description", nullable = false )
  private String description;

  @Column( name = "metadata_requester_id", updatable = false )
  private String requesterId;

  @Column( name = "metadata_requester_managed", nullable = false )
  private Boolean requesterManaged;

  @Column( name = "metadata_mac_address", nullable = false )
  private String macAddress;

  @Column( name = "metadata_private_ip", nullable = false )
  private String privateIpAddress;

  @Column( name = "metadata_private_name" )
  private String privateDnsName;

  @Column( name = "metadata_source_dest_check", nullable = false )
  private Boolean sourceDestCheck;

  @Column( name = "metadata_type" )
  @Enumerated( EnumType.STRING )
  private Type type;

  @Embedded
  private NetworkInterfaceAttachment attachment;

  @Embedded
  private NetworkInterfaceAssociation association;

  @OneToMany( fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "networkInterface" )
  private Collection<NetworkInterfaceTag> tags;

  @Override
  public String getPartition( ) {
    return availabilityZone;
  }

  @Override
  public FullName getFullName() {
    return FullName.create.vendor( "euca" )
        .region( ComponentIds.lookup( Eucalyptus.class ).name( ) )
        .namespace( getOwnerAccountNumber() )
        .relativeId( "network-interface", getDisplayName() );
  }

  public Vpc getVpc() {
    return vpc;
  }

  public void setVpc( final Vpc vpc ) {
    this.vpc = vpc;
  }

  public Subnet getSubnet() {
    return subnet;
  }

  public void setSubnet( final Subnet subnet ) {
    this.subnet = subnet;
  }

  public Set<NetworkGroup> getNetworkGroups( ) {
    return networkGroups;
  }

  public void setNetworkGroups( final Set<NetworkGroup> networkGroups ) {
    this.networkGroups = networkGroups;
  }

  public VmInstance getInstance() {
    return instance;
  }

  public String getAvailabilityZone() {
    return availabilityZone;
  }

  public void setAvailabilityZone( final String availabilityZone ) {
    this.availabilityZone = availabilityZone;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription( final String description ) {
    this.description = description;
  }

  public String getRequesterId() {
    return requesterId;
  }

  public void setRequesterId( final String requesterId ) {
    this.requesterId = requesterId;
  }

  public Boolean getRequesterManaged() {
    return requesterManaged;
  }

  public void setRequesterManaged( final Boolean requesterManaged ) {
    this.requesterManaged = requesterManaged;
  }

  public String getMacAddress() {
    return macAddress;
  }

  public void setMacAddress( final String macAddress ) {
    this.macAddress = macAddress;
  }

  public String getPrivateIpAddress() {
    return privateIpAddress;
  }

  public void setPrivateIpAddress( final String privateIpAddress ) {
    this.privateIpAddress = privateIpAddress;
  }

  public String getPrivateDnsName() {
    return privateDnsName;
  }

  public void setPrivateDnsName( final String privateDnsName ) {
    this.privateDnsName = privateDnsName;
  }

  public Boolean getSourceDestCheck() {
    return sourceDestCheck;
  }

  public void setSourceDestCheck( final Boolean sourceDestCheck ) {
    this.sourceDestCheck = sourceDestCheck;
  }

  public Type getType( ) {
    return type;
  }

  public void setType( final Type type ) {
    this.type = type;
  }

  public boolean isAttached( ) {
    return attachment != null;
  }

  public NetworkInterfaceAttachment getAttachment( ) {
    return attachment;
  }

  public void attach( final NetworkInterfaceAttachment attachment ) {
    if ( isAttached( ) ) throw new IllegalStateException( "Already attached" );
    this.setState( State.in_use );
    this.attachment = attachment;
    this.instance = attachment.getInstance( );
  }

  public void detach( ) {
    if ( attachment != null ) this.setState( State.available );
    this.attachment = null;
    this.instance = null;
  }

  public boolean isAssociated( ) {
    return association != null;
  }

  public NetworkInterfaceAssociation getAssociation( ) {
    return association;
  }

  public void associate( final NetworkInterfaceAssociation association ) {
    if ( isAssociated( ) ) throw new IllegalStateException( "Already associated" );
    this.association = association;
    // Seems hibernate does not notice the new association unless we make
    // another change also, so touch last updated time manually.
    this.updateTimeStamps( );
  }

  public void disassociate( ) {
    this.association = null;
  }

  @PostLoad
  protected void postLoad( ) {
    if ( isAttached( ) ) {
      getAttachment( ).setInstance( instance );
    }
  }

  @Upgrades.EntityUpgrade( entities = NetworkInterface.class,  since = Upgrades.Version.v4_2_2, value = Eucalyptus.class)
  public enum NetworkInterfaceEntityUpgrade422 implements Predicate<Class> {
    INSTANCE;

    private static Logger logger = Logger.getLogger( NetworkInterfaceEntityUpgrade422.class );

    @SuppressWarnings( "unchecked" )
    @Override
    public boolean apply( final Class entityClass ) {
      try ( final TransactionResource tx = Entities.transactionFor( NetworkInterface.class ) ) {
        final List<NetworkInterface> entities = (List<NetworkInterface>)
            Entities.createCriteria( NetworkInterface.class )
                .add( Restrictions.eq( "attachment.deviceIndex", 0 ) )
                .add( Restrictions.isNotNull( "attachment.instanceId" ) )
                .add( Restrictions.isEmpty( "networkGroups" ) )
                .list( );
        for ( final NetworkInterface entity : entities ) {
          final Set<NetworkGroup> groups = entity.getAttachment( ).getInstance( ).getNetworkGroups( );
          if ( !groups.isEmpty( ) ) {
            logger.info(
                "Updating security groups for network interface " + entity.getDisplayName( ) +
                " with groups for " + entity.getAttachment( ).getInstanceId( ) );
            entity.setNetworkGroups( Sets.newHashSet( groups ) );
          }
        }
        tx.commit( );
      }
      return true;
    }
  }

  @Upgrades.EntityUpgrade( entities = NetworkInterface.class,  since = Upgrades.Version.v4_3_0, value = Eucalyptus.class)
  public enum NetworkInterfaceEntityUpgrade430 implements Predicate<Class> {
    INSTANCE;

    private static Logger logger = Logger.getLogger( NetworkInterfaceEntityUpgrade430.class );

    @SuppressWarnings( "unchecked" )
    @Override
    public boolean apply( final Class entityClass ) {
      try ( final TransactionResource tx = Entities.transactionFor( NetworkInterface.class ) ) {
        final List<NetworkInterface> entities = (List<NetworkInterface>)
            Entities.createCriteria( NetworkInterface.class )
                .add( Restrictions.isNull( "type" ) )
                .list( );
        for ( final NetworkInterface entity : entities ) {
          if ( entity.getType( ) == null ) {
            logger.info( "Updating type for network interface " + entity.getDisplayName( ) );
            entity.setType( Type.Interface );
            if ( entity.getState( ) == State.attaching || entity.getState( ) == State.detaching ) {
              entity.setState( State.in_use );
            }
          }
        }
        tx.commit( );
      }
      return true;
    }
  }
}
