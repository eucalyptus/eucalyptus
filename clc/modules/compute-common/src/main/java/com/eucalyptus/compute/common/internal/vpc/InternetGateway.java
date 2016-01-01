/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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

import static com.eucalyptus.compute.common.CloudMetadata.InternetGatewayMetadata;
import java.util.Collection;
import javax.persistence.CascadeType;
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

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_internet_gateways", indexes = {
    @Index( name = "metadata_internet_gateways_account_id_idx", columnList = "metadata_account_id" ),
    @Index( name = "metadata_internet_gateways_display_name_idx", columnList = "metadata_display_name" ),
} )
public class InternetGateway extends AbstractOwnedPersistent implements InternetGatewayMetadata {

  private static final long serialVersionUID = 1L;

  protected InternetGateway( ) {
  }

  protected InternetGateway( final OwnerFullName owner, final String displayName ) {
    super( owner, displayName );
  }

  public static InternetGateway create( final OwnerFullName owner,
                                        final String name ) {
    return new InternetGateway( owner, name );
  }

  public static InternetGateway exampleWithOwner( final OwnerFullName owner ) {
    return new InternetGateway( owner, null );
  }

  public static InternetGateway exampleWithName( final OwnerFullName owner, final String name ) {
    return new InternetGateway( owner, name );
  }

  @ManyToOne
  @JoinColumn( name = "metadata_vpc_id" )
  private Vpc vpc;

  @OneToMany( fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "internetGateway" )
  private Collection<InternetGatewayTag> tags;

  public Vpc getVpc() {
    return vpc;
  }

  public void setVpc( final Vpc vpc ) {
    this.vpc = vpc;
  }
}
