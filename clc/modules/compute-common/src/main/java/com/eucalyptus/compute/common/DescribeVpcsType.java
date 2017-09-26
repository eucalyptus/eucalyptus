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

public class DescribeVpcsType extends VpcMessage {

  @HttpEmbedded
  private VpcIdSetType vpcSet;
  @HttpParameterMapping( parameter = "Filter" )
  @HttpEmbedded( multiple = true )
  private ArrayList<Filter> filterSet = new ArrayList<Filter>( );

  public Collection<String> vpcIds( ) {
    List<String> vpcIds = Lists.newArrayList( );
    if ( vpcSet != null && vpcSet.getItem( ) != null ) {
      for ( VpcIdSetItemType item : vpcSet.getItem( ) ) {
        if ( item != null ) {
          ( (ArrayList<String>) vpcIds ).add( item.getVpcId( ) );
        }

      }

    }

    return vpcIds;
  }

  public VpcIdSetType getVpcSet( ) {
    return vpcSet;
  }

  public void setVpcSet( VpcIdSetType vpcSet ) {
    this.vpcSet = vpcSet;
  }

  public ArrayList<Filter> getFilterSet( ) {
    return filterSet;
  }

  public void setFilterSet( ArrayList<Filter> filterSet ) {
    this.filterSet = filterSet;
  }
}
