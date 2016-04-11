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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import com.eucalyptus.compute.common.internal.identifier.ResourceIdentifiers;
import com.eucalyptus.entities.AbstractPersistent;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_route_table_associations", indexes = {
    @Index( name = "metadata_route_table_associations_subnet_idx", columnList = "metadata_subnet" ),
} )
public class RouteTableAssociation extends AbstractPersistent {

  private static final long serialVersionUID = 1L;

  @Column( name = "metadata_association_id", nullable = false, updatable = false, unique = true )
  private String associationId;

  @Column( name = "metadata_main", nullable = false, updatable = false )
  private Boolean main;

  @ManyToOne( optional = false, fetch = FetchType.LAZY )
  @JoinColumn( name = "metadata_route_table", nullable = false, updatable = false )
  private RouteTable routeTable;

  @Column( name = "metadata_route_table_id", nullable = false, updatable = false )
  private String routeTableId;

  @OneToOne
  @JoinColumn( name = "metadata_subnet" )
  private Subnet subnet;

  @Column( name = "metadata_subnet_id", updatable = false, unique = true )
  private String subnetId;

  protected RouteTableAssociation( ) {

  }

  public static RouteTableAssociation create( final RouteTable routeTable ) {
    final RouteTableAssociation association = new RouteTableAssociation( );
    association.setAssociationId( ResourceIdentifiers.generateString( "rtbassoc" ) );
    association.setRouteTable( routeTable );
    association.setRouteTableId( routeTable.getDisplayName( ) );
    association.setMain( true );
    return association;
  }

  public static RouteTableAssociation create( final RouteTable routeTable,
                                              final Subnet subnet ) {
    final RouteTableAssociation association = create( routeTable );
    association.setSubnet( subnet );
    association.setSubnetId( subnet.getDisplayName( ) );
    association.setMain( false );
    return association;
  }

  public String getAssociationId() {
    return associationId;
  }

  public void setAssociationId( final String associationId ) {
    this.associationId = associationId;
  }

  public Boolean getMain() {
    return main;
  }

  public void setMain( final Boolean main ) {
    this.main = main;
  }

  public RouteTable getRouteTable() {
    return routeTable;
  }

  public void setRouteTable( final RouteTable routeTable ) {
    this.routeTable = routeTable;
  }

  public String getRouteTableId() {
    return routeTableId;
  }

  public void setRouteTableId( final String routeTableId ) {
    this.routeTableId = routeTableId;
  }

  public Subnet getSubnet() {
    return subnet;
  }

  public void setSubnet( final Subnet subnet ) {
    this.subnet = subnet;
  }

  public String getSubnetId() {
    return subnetId;
  }

  public void setSubnetId( final String subnetId ) {
    this.subnetId = subnetId;
  }
}
