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

import java.util.Collection;
import com.eucalyptus.util.CompatFunction;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class RouteTableType extends EucalyptusData implements VpcTagged {

  private String routeTableId;
  private String vpcId;
  private RouteSetType routeSet;
  private RouteTableAssociationSetType associationSet;
  private PropagatingVgwSetType propagatingVgwSet;
  private ResourceTagSetType tagSet;

  public RouteTableType( ) {
  }

  public RouteTableType( final String routeTableId, final String vpcId, final Collection<RouteType> routes, final Collection<RouteTableAssociationType> associations ) {
    this.routeTableId = routeTableId;
    this.vpcId = vpcId;
    this.routeSet = new RouteSetType( routes );
    this.associationSet = new RouteTableAssociationSetType( associations );
    this.propagatingVgwSet = new PropagatingVgwSetType( );
  }

  public static CompatFunction<RouteTableType, String> id( ) {
    return new CompatFunction<RouteTableType, String>( ) {
      @Override
      public String apply( final RouteTableType routeTableType ) {
        return routeTableType.getRouteTableId( );
      }
    };
  }

  public String getRouteTableId( ) {
    return routeTableId;
  }

  public void setRouteTableId( String routeTableId ) {
    this.routeTableId = routeTableId;
  }

  public String getVpcId( ) {
    return vpcId;
  }

  public void setVpcId( String vpcId ) {
    this.vpcId = vpcId;
  }

  public RouteSetType getRouteSet( ) {
    return routeSet;
  }

  public void setRouteSet( RouteSetType routeSet ) {
    this.routeSet = routeSet;
  }

  public RouteTableAssociationSetType getAssociationSet( ) {
    return associationSet;
  }

  public void setAssociationSet( RouteTableAssociationSetType associationSet ) {
    this.associationSet = associationSet;
  }

  public PropagatingVgwSetType getPropagatingVgwSet( ) {
    return propagatingVgwSet;
  }

  public void setPropagatingVgwSet( PropagatingVgwSetType propagatingVgwSet ) {
    this.propagatingVgwSet = propagatingVgwSet;
  }

  public ResourceTagSetType getTagSet( ) {
    return tagSet;
  }

  public void setTagSet( ResourceTagSetType tagSet ) {
    this.tagSet = tagSet;
  }
}
