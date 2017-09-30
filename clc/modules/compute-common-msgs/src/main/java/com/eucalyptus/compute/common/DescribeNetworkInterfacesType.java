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

public class DescribeNetworkInterfacesType extends VpcMessage {

  @HttpEmbedded
  private NetworkInterfaceIdSetType networkInterfaceIdSet;
  @HttpParameterMapping( parameter = "Filter" )
  @HttpEmbedded( multiple = true )
  private ArrayList<Filter> filterSet = new ArrayList<Filter>( );

  public Collection<String> networkInterfaceIds( ) {
    List<String> networkInterfaceIds = Lists.newArrayList( );
    if ( networkInterfaceIdSet != null && networkInterfaceIdSet.getItem( ) != null ) {
      for ( NetworkInterfaceIdSetItemType item : networkInterfaceIdSet.getItem( ) ) {
        if ( item != null ) {
          networkInterfaceIds.add( item.getNetworkInterfaceId( ) );
        }

      }

    }

    return networkInterfaceIds;
  }

  public NetworkInterfaceIdSetType getNetworkInterfaceIdSet( ) {
    return networkInterfaceIdSet;
  }

  public void setNetworkInterfaceIdSet( NetworkInterfaceIdSetType networkInterfaceIdSet ) {
    this.networkInterfaceIdSet = networkInterfaceIdSet;
  }

  public ArrayList<Filter> getFilterSet( ) {
    return filterSet;
  }

  public void setFilterSet( ArrayList<Filter> filterSet ) {
    this.filterSet = filterSet;
  }
}
