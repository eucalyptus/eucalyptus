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

import static com.eucalyptus.compute.common.CloudMetadata.RouteTableMetadata;
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
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_route_tables", indexes = {
    @Index( name = "metadata_route_tables_account_id_idx", columnList = "metadata_account_id" ),
    @Index( name = "metadata_route_tables_display_name_idx", columnList = "metadata_display_name" ),
} )
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
  private Vpc vpc;

  /**
   * True if main route table for VPC
   */
  @Column( name = "metadata_main" )
  private Boolean main;

  @OneToMany( cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "routeTable" )
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
