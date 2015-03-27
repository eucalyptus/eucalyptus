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
package com.eucalyptus.compute.common.internal.vpc;

import static com.eucalyptus.compute.common.CloudMetadata.VpcMetadata;
import java.util.Collection;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.OwnerFullName;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_vpcs" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class Vpc extends UserMetadata<Vpc.State> implements VpcMetadata {

  private static final long serialVersionUID = 1L;

  public enum State {
    pending,
    available,
  }

  protected Vpc( ) {
  }

  protected Vpc( final OwnerFullName owner, final String displayName ) {
    super( owner, displayName );
  }

  public static Vpc create( final OwnerFullName owner,
                            final String name,
                            final DhcpOptionSet dhcpOptionSet,
                            final String cidr,
                            final boolean defaultVpc ) {
    final Vpc vpc = new Vpc( owner, name );
    vpc.setDhcpOptionSet( dhcpOptionSet );
    vpc.setCidr( cidr );
    vpc.setDefaultVpc( defaultVpc );
    vpc.setDnsEnabled( true );
    vpc.setDnsHostnames( defaultVpc );
    vpc.setState( State.available );
    return vpc;
  }

  public static Vpc exampleWithOwner( final OwnerFullName owner ) {
    return new Vpc( owner, null );
  }

  public static Vpc exampleWithName( final OwnerFullName owner, final String name ) {
    return new Vpc( owner, name );
  }

  public static Vpc exampleDefault( final String accountNumber ) {
    final Vpc vpc = exampleWithOwner( null );
    vpc.setOwnerAccountNumber( accountNumber );
    vpc.setDefaultVpc( true );
    return vpc;
  }

  public static Vpc exampleDefault( final OwnerFullName owner ) {
    final Vpc vpc = exampleWithOwner( owner );
    vpc.setDefaultVpc( true );
    return vpc;
  }

  @Column( name = "metadata_cidr", nullable = false )
  private String cidr;

  @Column( name = "metadata_default", nullable = false )
  private Boolean defaultVpc;

  @Column( name = "metadata_dns_enabled", nullable = false )
  private Boolean dnsEnabled;

  @Column( name = "metadata_dns_hostnames", nullable = false )
  private Boolean dnsHostnames;

  @ManyToOne
  @JoinColumn( name = "metadata_dhcp_option_set_id" )
  private DhcpOptionSet dhcpOptionSet;

  @OneToMany( fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "vpc" )
  private Collection<VpcTag> tags;

  @Override
  public String getPartition() {
    return "eucalyptus";
  }

  @Override
  public FullName getFullName() {
    return FullName.create.vendor( "euca" )
        .region( ComponentIds.lookup( Eucalyptus.class ).name( ) )
        .namespace( getOwnerAccountNumber() )
        .relativeId( "vpc", getDisplayName() );
  }

  public String getCidr( ) {
    return cidr;
  }

  public void setCidr( final String cidr ) {
    this.cidr = cidr;
  }

  public Boolean getDefaultVpc() {
    return defaultVpc;
  }

  public void setDefaultVpc( final Boolean defaultVpc ) {
    this.defaultVpc = defaultVpc;
  }

  public Boolean getDnsEnabled() {
    return dnsEnabled;
  }

  public void setDnsEnabled( final Boolean dnsEnabled ) {
    this.dnsEnabled = dnsEnabled;
  }

  public Boolean getDnsHostnames() {
    return dnsHostnames;
  }

  public void setDnsHostnames( final Boolean dnsHostnames ) {
    this.dnsHostnames = dnsHostnames;
  }

  public DhcpOptionSet getDhcpOptionSet( ) {
    return dhcpOptionSet;
  }

  public void setDhcpOptionSet( final DhcpOptionSet dhcpOptionSet ) {
    this.dhcpOptionSet = dhcpOptionSet;
  }
}
