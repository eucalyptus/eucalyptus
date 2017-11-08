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

import static com.eucalyptus.compute.common.CloudMetadata.NetworkAclMetadata;
import static com.eucalyptus.compute.common.internal.vpc.NetworkAclEntry.RuleAction;
import java.util.Collection;
import java.util.List;
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
import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.google.common.collect.Lists;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_network_acls", indexes = {
    @Index( name = "metadata_network_acls_account_id_idx", columnList = "metadata_account_id" ),
    @Index( name = "metadata_network_acls_display_name_idx", columnList = "metadata_display_name" ),
} )
public class NetworkAcl extends AbstractOwnedPersistent implements NetworkAclMetadata {

  private static final long serialVersionUID = 1L;

  protected NetworkAcl( ) {
  }

  protected NetworkAcl( final OwnerFullName owner, final String displayName ) {
    super( owner, displayName );
  }

  public static NetworkAcl create( final OwnerFullName owner,
                                   final Vpc vpc,
                                   final String name,
                                   final boolean defaultForVpc ) {
    final NetworkAcl networkAcl = new NetworkAcl( owner, name );
    networkAcl.setVpc( vpc );
    networkAcl.setDefaultForVpc( defaultForVpc );
    networkAcl.setEntries( defaultForVpc ? Lists.newArrayList(
        NetworkAclEntry.createEntry( networkAcl, 100, -1, RuleAction.allow, true, "0.0.0.0/0" ),
        NetworkAclEntry.createEntry( networkAcl, 32767, -1, RuleAction.deny, true, "0.0.0.0/0" ),
        NetworkAclEntry.createEntry( networkAcl, 100, -1, RuleAction.allow, false, "0.0.0.0/0" ),
        NetworkAclEntry.createEntry( networkAcl, 32767, -1, RuleAction.deny, false, "0.0.0.0/0" )
    ) : Lists.newArrayList(
        NetworkAclEntry.createEntry( networkAcl, 32767, -1, RuleAction.deny, true, "0.0.0.0/0" ),
        NetworkAclEntry.createEntry( networkAcl, 32767, -1, RuleAction.deny, false, "0.0.0.0/0" )
    ) );
    networkAcl.subnets = Lists.newArrayList( );
    return networkAcl;
  }

  public static NetworkAcl exampleWithOwner( final OwnerFullName owner ) {
    return new NetworkAcl( owner, null );
  }

  public static NetworkAcl exampleWithName( final OwnerFullName owner, final String name ) {
    return new NetworkAcl( owner, name );
  }

  public static NetworkAcl exampleDefault() {
    final NetworkAcl networkAcl = new NetworkAcl( );
    networkAcl.setDefaultForVpc( true );
    return networkAcl;
  }

  @ManyToOne( optional = false )
  @JoinColumn( name = "metadata_vpc_id" )
  private Vpc vpc;

  @Column( name = "metadata_default" )
  private Boolean defaultForVpc;

  @OneToMany( cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "networkAcl" )
  private List<NetworkAclEntry> entries = Lists.newArrayList( );

  @OneToMany( cascade = CascadeType.REFRESH , orphanRemoval = true, mappedBy = "networkAcl" )
  private Collection<Subnet> subnets;

  @OneToMany( fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "networkAcl" )
  private Collection<NetworkAclTag> tags;

  public Vpc getVpc() {
    return vpc;
  }

  public void setVpc( final Vpc vpc ) {
    this.vpc = vpc;
  }

  public Boolean getDefaultForVpc() {
    return defaultForVpc;
  }

  public void setDefaultForVpc( final Boolean defaultForVpc ) {
    this.defaultForVpc = defaultForVpc;
  }

  public List<NetworkAclEntry> getEntries( ) {
    return entries;
  }

  public void setEntries( final List<NetworkAclEntry> entries ) {
    this.entries = entries;
  }

  public List<Subnet> getSubnets( ) {
    return Lists.newArrayList( subnets );
  }
}
