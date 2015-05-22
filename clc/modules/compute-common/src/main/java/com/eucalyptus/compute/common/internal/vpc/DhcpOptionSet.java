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

import static com.eucalyptus.compute.common.CloudMetadata.DhcpOptionSetMetadata;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.Strings;
import com.google.common.collect.Sets;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_dhcp_option_sets" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class DhcpOptionSet extends AbstractOwnedPersistent implements DhcpOptionSetMetadata {

  private static final long serialVersionUID = 1L;

  @Column( name = "metadata_default", nullable = false, updatable = false )
  private Boolean defaultOptions;

  @OneToMany( cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "dhcpOptionSet" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Set<DhcpOption> dhcpOptions = new HashSet<>( );

  @OneToMany( fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "dhcpOptionSet" )
  private Collection<DhcpOptionSetTag> tags;

  protected DhcpOptionSet( ) {
  }

  protected DhcpOptionSet( final OwnerFullName owner, final String displayName ) {
    super( owner, displayName );
  }

  public static DhcpOptionSet create( final OwnerFullName owner,
                                      final String name ) {
    final DhcpOptionSet dhcpOptionSet = new DhcpOptionSet( owner, name );
    dhcpOptionSet.setDefaultOptions( false );
    return dhcpOptionSet;
  }

  public static DhcpOptionSet createDefault( final OwnerFullName owner,
                                             final String name,
                                             final String instanceSubdomain ) {
    final DhcpOptionSet dhcpOptionSet = new DhcpOptionSet( owner, name );
    dhcpOptionSet.setDefaultOptions( true );
    dhcpOptionSet.setDhcpOptions( Sets.newHashSet(
        DhcpOption.create( dhcpOptionSet, "domain-name", Strings.trimPrefix( ".", instanceSubdomain + ".internal" ) ),
        DhcpOption.create( dhcpOptionSet, "domain-name-servers", "AmazonProvidedDNS" )
    ) );
    return dhcpOptionSet;
  }

  public static DhcpOptionSet exampleWithOwner( final OwnerFullName owner ) {
    return new DhcpOptionSet( owner, null );
  }

  public static DhcpOptionSet exampleWithName( final OwnerFullName owner, final String name ) {
    return new DhcpOptionSet( owner, name );
  }

  public static DhcpOptionSet exampleDefault( final OwnerFullName owner  ) {
    final DhcpOptionSet dhcpOptionSet = exampleWithOwner( owner );
    dhcpOptionSet.setDefaultOptions( true );
    return dhcpOptionSet;
  }

  public Boolean getDefaultOptions( ) {
    return defaultOptions;
  }

  public void setDefaultOptions( final Boolean defaultOptions ) {
    this.defaultOptions = defaultOptions;
  }

  public Set<DhcpOption> getDhcpOptions() {
    return dhcpOptions;
  }

  public void setDhcpOptions( final Set<DhcpOption> dhcpOptions ) {
    this.dhcpOptions = dhcpOptions;
  }
}
