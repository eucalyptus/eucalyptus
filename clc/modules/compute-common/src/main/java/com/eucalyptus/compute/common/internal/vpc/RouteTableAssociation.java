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
