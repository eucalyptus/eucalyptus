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

import static com.eucalyptus.compute.common.CloudMetadata.RouteTableMetadata;
import java.util.Collection;
import java.util.List;
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
import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_route_tables" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class RouteTable extends AbstractOwnedPersistent implements RouteTableMetadata {

  private static final long serialVersionUID = 1L;

  protected RouteTable( ) {
  }

  protected RouteTable( final OwnerFullName owner, final String displayName ) {
    super( owner, displayName );
  }

  public static RouteTable create( final OwnerFullName owner,
                                   final Vpc vpc,
                                   final String name,
                                   final String destinationCidr,
                                   final boolean main ) {
    final RouteTable routeTable = new RouteTable( owner, name );
    routeTable.setVpc( vpc );
    routeTable.setMain( main );
    routeTable.setRoutes( Lists.newArrayList(
      Route.create( routeTable, destinationCidr )
    ) );
    routeTable.setRouteTableAssociations( Lists.newArrayList( Optional.fromNullable(
      main ?  RouteTableAssociation.create( routeTable ) : null
    ).asSet( ) ) );
    return routeTable;
  }

  public static RouteTable exampleWithOwner( final OwnerFullName owner ) {
    return new RouteTable( owner, null );
  }

  public static RouteTable exampleWithName( final OwnerFullName owner, final String name ) {
    return new RouteTable( owner, name );
  }

  public static RouteTable exampleMain( ) {
    final RouteTable routeTable = new RouteTable( );
    routeTable.setMain( true );
    return routeTable;
  }

  @ManyToOne( optional = false )
  @JoinColumn( name = "metadata_vpc_id" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Vpc vpc;

  /**
   * True if main route table for VPC
   */
  @Column( name = "metadata_main" )
  private Boolean main;

  @OneToMany( cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "routeTable" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private List<Route> routes = Lists.newArrayList();

  @OneToMany( fetch = FetchType.LAZY, cascade = CascadeType.ALL , orphanRemoval = true, mappedBy = "routeTable" )
  private List<RouteTableAssociation> routeTableAssociations;

  @OneToMany( fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "routeTable" )
  private Collection<RouteTableTag> tags;

  public Vpc getVpc() {
    return vpc;
  }

  public void setVpc( final Vpc vpc ) {
    this.vpc = vpc;
  }

  public Boolean getMain() {
    return main;
  }

  public void setMain( final Boolean main ) {
    this.main = main;
  }

  public List<Route> getRoutes( ) {
    return routes;
  }

  public void setRoutes( final List<Route> routes ) {
    this.routes = routes;
  }

  public List<RouteTableAssociation> getRouteTableAssociations( ) {
    return routeTableAssociations;
  }

  public void setRouteTableAssociations( final List<RouteTableAssociation> routeTableAssociations ) {
    this.routeTableAssociations = routeTableAssociations;
  }

  public RouteTableAssociation associateMain( ) {
    final RouteTableAssociation association = RouteTableAssociation.create( this );
    setMain( true );
    getRouteTableAssociations( ).add( association );
    return association;
  }

  public RouteTableAssociation associate( final Subnet subnet ) {
    final RouteTableAssociation association = RouteTableAssociation.create( this, subnet );
    getRouteTableAssociations( ).add( association );
    updateTimeStamps( );
    return association;
  }

  public RouteTableAssociation disassociate( final String associationId ) {
    for ( final RouteTableAssociation association : getRouteTableAssociations( ) ) {
      if ( associationId.equals( association.getAssociationId( ) ) ) {
        if ( association.getMain( ) ) {
          setMain( false );
        }
        getRouteTableAssociations( ).remove( association );
        updateTimeStamps( );
        return association;
      }
    }
    return null;
  }
}
