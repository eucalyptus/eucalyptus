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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import com.eucalyptus.entities.AbstractStatefulPersistent;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_routes" )
public class Route extends AbstractStatefulPersistent<Route.State> {
  private static final long serialVersionUID = 1L;

  public enum State {
    active,
    blackhole,
  }

  public enum RouteOrigin {
    CreateRouteTable,
    CreateRoute
  }

  @ManyToOne( optional = false )
  @JoinColumn( name = "metadata_route_table_id", nullable = false, updatable = false )
  private RouteTable routeTable;

  @Column( name = "metadata_origin", nullable = false, updatable = false )
  @Enumerated( EnumType.STRING )
  private RouteOrigin origin;

  @Column( name = "metadata_destination_cidr", nullable = false, updatable = false )
  private String destinationCidr;

  @ManyToOne
  @JoinColumn( name = "metadata_internet_gateway_id", nullable = true, updatable = false )
  private InternetGateway internetGateway;

  protected Route( ) {
  }

  protected Route( final RouteTable routeTable,
                   final RouteOrigin origin,
                   final String destinationCidr,
                   final InternetGateway internetGateway ) {
    this.routeTable = routeTable;
    this.origin = origin;
    this.destinationCidr = destinationCidr;
    this.internetGateway = internetGateway;
    this.setState( State.active );
  }

  public static Route create( final RouteTable routeTable,
                              final RouteOrigin origin,
                              final String destinationCidr,
                              final InternetGateway internetGateway ) {
    return new Route( routeTable, origin, destinationCidr, internetGateway );
  }

  public static Route create( final RouteTable routeTable,
                              final String destinationCidr ) {
    return new Route( routeTable, RouteOrigin.CreateRouteTable, destinationCidr, null );
  }

  public RouteTable getRouteTable( ) {
    return routeTable;
  }

  public void setRouteTable( final RouteTable routeTable ) {
    this.routeTable = routeTable;
  }

  public RouteOrigin getOrigin() {
    return origin;
  }

  public void setOrigin( final RouteOrigin origin ) {
    this.origin = origin;
  }

  public String getDestinationCidr( ) {
    return destinationCidr;
  }

  public void setDestinationCidr( final String destinationCidr ) {
    this.destinationCidr = destinationCidr;
  }

  public InternetGateway getInternetGateway( ) {
    return internetGateway;
  }

  public void setInternetGateway( final InternetGateway internetGateway ) {
    this.internetGateway = internetGateway;
  }
}
