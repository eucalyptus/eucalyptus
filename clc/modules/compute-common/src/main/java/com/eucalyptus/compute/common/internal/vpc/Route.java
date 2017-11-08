/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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

import static com.eucalyptus.upgrade.Upgrades.Version.v4_3_0;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.apache.log4j.Logger;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.AbstractStatefulPersistent;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.upgrade.Upgrades;
import groovy.sql.Sql;

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

  @Column( name = "metadata_internet_gateway", updatable = false )
  private String internetGatewayId;

  @Column( name = "metadata_nat_gateway", updatable = false )
  private String natGatewayId;

  @Column( name = "metadata_network_interface", updatable = false )
  private String networkInterfaceId;

  @Column( name = "metadata_instance" )
  private String instanceId;

  @Column( name = "metadata_instance_ownerid" )
  private String instanceAccountNumber;

  protected Route( ) {
  }

  protected Route( final RouteTable routeTable,
                   final RouteOrigin origin,
                   final String destinationCidr ) {
    this.routeTable = routeTable;
    this.origin = origin;
    this.destinationCidr = destinationCidr;
    this.setState( State.active );
  }

  protected Route( final RouteTable routeTable,
                   final RouteOrigin origin,
                   final String destinationCidr,
                   final InternetGateway internetGateway ) {
    this( routeTable, origin, destinationCidr );
    setInternetGatewayId( internetGateway.getDisplayName( ) );
  }

  protected Route( final RouteTable routeTable,
                   final RouteOrigin origin,
                   final String destinationCidr,
                   final NatGateway natGateway ) {
    this( routeTable, origin, destinationCidr );
    setNatGatewayId( natGateway.getDisplayName( ) );
  }

  protected Route( final RouteTable routeTable,
                   final RouteOrigin origin,
                   final String destinationCidr,
                   final NetworkInterface networkInterface ) {
    this( routeTable, origin, destinationCidr );
    setNetworkInterfaceId( networkInterface.getDisplayName( ) );
    if ( networkInterface.getInstance( ) != null ) {
      setInstanceId( networkInterface.getInstance( ).getDisplayName( ) );
      setInstanceAccountNumber( networkInterface.getInstance( ).getOwnerAccountNumber( ) );
    } else {
      setState( State.blackhole );
    }
  }

  public static Route create( final RouteTable routeTable,
                              final RouteOrigin origin,
                              final String destinationCidr,
                              final InternetGateway internetGateway ) {
    return new Route( routeTable, origin, destinationCidr, internetGateway );
  }

  public static Route create( final RouteTable routeTable,
                              final RouteOrigin origin,
                              final String destinationCidr,
                              final NatGateway natGateway ) {
    return new Route( routeTable, origin, destinationCidr, natGateway );
  }

  public static Route create( final RouteTable routeTable,
                              final RouteOrigin origin,
                              final String destinationCidr,
                              final NetworkInterface networkInterface ) {
    return new Route( routeTable, origin, destinationCidr, networkInterface );
  }

  /**
   * Create a local route
   */
  public static Route create( final RouteTable routeTable,
                              final String destinationCidr ) {
    return new Route( routeTable, RouteOrigin.CreateRouteTable, destinationCidr );
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

  public String getInternetGatewayId( ) {
    return internetGatewayId;
  }

  public void setInternetGatewayId( final String internetGatewayId ) {
    this.internetGatewayId = internetGatewayId;
  }

  public String getNatGatewayId( ) {
    return natGatewayId;
  }

  public void setNatGatewayId( final String natGatewayId ) {
    this.natGatewayId = natGatewayId;
  }

  public String getNetworkInterfaceId( ) {
    return networkInterfaceId;
  }

  public void setNetworkInterfaceId( final String networkInterfaceId ) {
    this.networkInterfaceId = networkInterfaceId;
  }

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( final String instanceId ) {
    this.instanceId = instanceId;
  }

  public String getInstanceAccountNumber( ) {
    return instanceAccountNumber;
  }

  public void setInstanceAccountNumber( final String instanceAccountNumber ) {
    this.instanceAccountNumber = instanceAccountNumber;
  }

  @Upgrades.PreUpgrade( since = v4_3_0, value = Eucalyptus.class )
  public static class RoutePreUpgrade430 implements Callable<Boolean> {
    private static final Logger logger = Logger.getLogger( RoutePreUpgrade430.class );

    private boolean columnExists( Sql sql, String name ) throws SQLException {
      return !sql.rows(
          "select column_name from information_schema.columns where table_schema=? and table_name=? and column_name=?",
          new Object[]{ PersistenceContexts.toSchemaName( ).apply( "eucalyptus_cloud" ), "metadata_routes", name }
      ).isEmpty( );
    }

    @Override
    public Boolean call( ) throws Exception {
      Sql sql = null;
      try {
        sql = Upgrades.DatabaseFilters.NEWVERSION.getConnection( "eucalyptus_cloud" );
        if ( columnExists( sql, "metadata_internet_gateway_id" ) ) {
          logger.info( "Upgrading routes internet gateway references" );
          sql.executeUpdate( "update metadata_routes set metadata_internet_gateway = (select " +
              "metadata_internet_gateways.metadata_display_name from metadata_internet_gateways, metadata_route_tables " +
              "where metadata_internet_gateways.metadata_vpc_id=metadata_route_tables.metadata_vpc_id and " +
              "metadata_route_tables.id=metadata_routes.metadata_route_table_id) where metadata_internet_gateway_id is " +
              "not null" );
          sql.executeUpdate( "alter table metadata_routes drop column metadata_internet_gateway_id" );
        }
        return Boolean.TRUE;
      } finally {
        if ( sql != null ) sql.close( );
      }
    }
  }
}
