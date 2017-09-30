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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import com.google.common.collect.Lists;

public class DescribeRouteTablesType extends VpcMessage {

  @HttpEmbedded
  private RouteTableIdSetType routeTableIdSet;
  @HttpParameterMapping( parameter = "Filter" )
  @HttpEmbedded( multiple = true )
  private ArrayList<Filter> filterSet = new ArrayList<Filter>( );

  public Collection<String> routeTableIds( ) {
    List<String> routeTableIds = Lists.newArrayList( );
    if ( routeTableIdSet != null && routeTableIdSet.getItem( ) != null ) {
      for ( RouteTableIdSetItemType item : routeTableIdSet.getItem( ) ) {
        if ( item != null ) {
          routeTableIds.add( item.getRouteTableId( ) );
        }

      }

    }

    return routeTableIds;
  }

  public RouteTableIdSetType getRouteTableIdSet( ) {
    return routeTableIdSet;
  }

  public void setRouteTableIdSet( RouteTableIdSetType routeTableIdSet ) {
    this.routeTableIdSet = routeTableIdSet;
  }

  public ArrayList<Filter> getFilterSet( ) {
    return filterSet;
  }

  public void setFilterSet( ArrayList<Filter> filterSet ) {
    this.filterSet = filterSet;
  }
}
