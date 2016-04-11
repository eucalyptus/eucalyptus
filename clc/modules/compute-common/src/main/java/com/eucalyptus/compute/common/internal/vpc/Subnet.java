/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
package com.eucalyptus.compute.common.internal.vpc;

import static com.eucalyptus.compute.common.CloudMetadata.SubnetMetadata;
import java.util.Collection;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.internal.identifier.ResourceIdentifiers;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.util.Cidr;
import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.auth.principal.OwnerFullName;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_subnets", indexes = {
    @Index( name = "metadata_subnets_user_id_idx", columnList = "metadata_user_id" ),
    @Index( name = "metadata_subnets_account_id_idx", columnList = "metadata_account_id" ),
    @Index( name = "metadata_subnets_display_name_idx", columnList = "metadata_display_name" ),
}  )
public class Subnet extends UserMetadata<Subnet.State> implements SubnetMetadata {

  private static final long serialVersionUID = 1L;

  public enum State {
    pending,
    available,
  }

  protected Subnet( ) {
  }

  protected Subnet( final OwnerFullName owner, final String displayName ) {
    super( owner, displayName );
  }

  public static Subnet create( final OwnerFullName owner,
                               final Vpc vpc,
                               final NetworkAcl networkAcl,
                               final String name,
                               final String cidr,
                               final String availabilityZone ) {
    final Subnet subnet = new Subnet( owner, name );
    subnet.setVpc( vpc );
    subnet.setNetworkAcl( networkAcl );
    subnet.setNetworkAclAssociationId( ResourceIdentifiers.generateString( "aclassoc" ) );
    subnet.setCidr( cidr );
    subnet.setAvailabilityZone( availabilityZone );
    subnet.setAvailableIpAddressCount( usableAddressesForSubnet( cidr ) );
    subnet.setDefaultForAz( false );
    subnet.setMapPublicIpOnLaunch( false );
    subnet.setState( State.available );
    return subnet;
  }

  public static int usableAddressesForSubnet( final String cidr ) {
    return Math.max( ((int) Math.pow( 2, 32 - Cidr.parse( cidr ).getPrefix( ) )) - 5, 0 ); // 5 reserved
  }

  public static Subnet exampleWithOwner( final OwnerFullName owner ) {
    return new Subnet( owner, null );
  }

  public static Subnet exampleWithName( final OwnerFullName owner, final String name ) {
    return new Subnet( owner, name );
  }

  public static Subnet exampleDefault( final OwnerFullName owner,
                                       final String availabilityZone ) {
    final Subnet subnet = exampleWithOwner( owner );
    subnet.setAvailabilityZone( availabilityZone );
    subnet.setDefaultForAz( true );
    return subnet;
  }

  public static Subnet exampleWithNetworkAclAssociation( final OwnerFullName owner, final String associationId ) {
    final Subnet subnet = new Subnet( owner, null );
    subnet.setNetworkAclAssociationId( associationId );
    return subnet;
  }

  @ManyToOne( optional = false )
  @JoinColumn( name = "metadata_vpc_id" )
  private Vpc vpc;

  @ManyToOne( optional = false, fetch = FetchType.LAZY )
  @JoinColumn( name = "metadata_network_acl_id" )
  private NetworkAcl networkAcl;

  @Column( name = "metadata_nacl_association_id", nullable = false, unique = true )
  private String networkAclAssociationId;

  @Column( name = "metadata_cidr", nullable = false )
  private String cidr;

  @Column( name = "metadata_availability_zone", nullable = false )
  private String availabilityZone;

  @Column( name = "metadata_available_ips", nullable = false )
  private Integer availableIpAddressCount;

  @Column( name = "metadata_default_for_az", nullable = false )
  private Boolean defaultForAz;

  @Column( name = "metadata_map_public_ip", nullable = false )
  private Boolean mapPublicIpOnLaunch;

  @OneToOne( cascade = CascadeType.REMOVE, mappedBy = "subnet" )
  private RouteTableAssociation routeTableAssociation;

  @OneToMany( fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "subnet" )
  private Collection<SubnetTag> tags;

  @Override
  public String getPartition() {
    return "eucalyptus";
  }

  @Override
  public FullName getFullName() {
    return FullName.create.vendor( "euca" )
        .region( ComponentIds.lookup( Eucalyptus.class ).name( ) )
        .namespace( getOwnerAccountNumber() )
        .relativeId( "subnet", getDisplayName() );
  }

  public Vpc getVpc() {
    return vpc;
  }

  public void setVpc( final Vpc vpc ) {
    this.vpc = vpc;
  }

  public NetworkAcl getNetworkAcl( ) {
    return networkAcl;
  }

  public void setNetworkAcl( final NetworkAcl networkAcl ) {
    this.networkAcl = networkAcl;
  }

  public String getNetworkAclAssociationId( ) {
    return networkAclAssociationId;
  }

  public void setNetworkAclAssociationId( final String networkAclAssociationId ) {
    this.networkAclAssociationId = networkAclAssociationId;
  }

  public String getCidr( ) {
    return cidr;
  }

  public void setCidr( final String cidr ) {
    this.cidr = cidr;
  }

  public String getAvailabilityZone( ) {
    return availabilityZone;
  }

  public void setAvailabilityZone( final String availabilityZone ) {
    this.availabilityZone = availabilityZone;
  }

  public Integer getAvailableIpAddressCount() {
    return availableIpAddressCount;
  }

  public void setAvailableIpAddressCount( final Integer availableIpAddressCount ) {
    this.availableIpAddressCount = availableIpAddressCount;
  }

  public Boolean getDefaultForAz() {
    return defaultForAz;
  }

  public void setDefaultForAz( final Boolean defaultForAz ) {
    this.defaultForAz = defaultForAz;
  }

  public Boolean getMapPublicIpOnLaunch() {
    return mapPublicIpOnLaunch;
  }

  public void setMapPublicIpOnLaunch( final Boolean mapPublicIpOnLaunch ) {
    this.mapPublicIpOnLaunch = mapPublicIpOnLaunch;
  }
}
