/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class RouteTableAssociationType extends EucalyptusData {

  private String routeTableAssociationId;
  private String routeTableId;
  private String subnetId;
  private Boolean main;

  public RouteTableAssociationType( ) {
  }

  public RouteTableAssociationType( final String routeTableAssociationId, final String routeTableId, final String subnetId, final Boolean main ) {
    this.routeTableAssociationId = routeTableAssociationId;
    this.routeTableId = routeTableId;
    this.subnetId = subnetId;
    this.main = main;
  }

  public String getRouteTableAssociationId( ) {
    return routeTableAssociationId;
  }

  public void setRouteTableAssociationId( String routeTableAssociationId ) {
    this.routeTableAssociationId = routeTableAssociationId;
  }

  public String getRouteTableId( ) {
    return routeTableId;
  }

  public void setRouteTableId( String routeTableId ) {
    this.routeTableId = routeTableId;
  }

  public String getSubnetId( ) {
    return subnetId;
  }

  public void setSubnetId( String subnetId ) {
    this.subnetId = subnetId;
  }

  public Boolean getMain( ) {
    return main;
  }

  public void setMain( Boolean main ) {
    this.main = main;
  }
}
