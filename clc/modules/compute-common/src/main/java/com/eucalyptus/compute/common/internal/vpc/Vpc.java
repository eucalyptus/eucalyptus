/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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

import static com.eucalyptus.compute.common.CloudMetadata.VpcMetadata;
import java.util.Collection;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.auth.principal.OwnerFullName;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_vpcs", indexes = {
    @Index( name = "metadata_vpcs_user_id_idx", columnList = "metadata_user_id" ),
    @Index( name = "metadata_vpcs_account_id_idx", columnList = "metadata_account_id" ),
    @Index( name = "metadata_vpcs_display_name_idx", columnList = "metadata_display_name" ),
} )
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
