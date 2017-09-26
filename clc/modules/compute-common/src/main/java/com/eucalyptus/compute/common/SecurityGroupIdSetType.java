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
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class SecurityGroupIdSetType extends EucalyptusData {

  @HttpParameterMapping( parameter = "SecurityGroupId" )
  @HttpEmbedded( multiple = true )
  private ArrayList<SecurityGroupIdSetItemType> item = new ArrayList<SecurityGroupIdSetItemType>( );

  public SecurityGroupIdSetType( ) {
  }

  public SecurityGroupIdSetType( final Collection<SecurityGroupIdSetItemType> values ) {
    this.item = Lists.newArrayList( values );
  }

  public ArrayList<String> groupIds( ) {
    ArrayList<String> groupIds = Lists.newArrayList( );
    if ( item != null ) {
      for ( SecurityGroupIdSetItemType itemType : item ) {
        groupIds.add( itemType.getGroupId( ) );
      }

    }

    return groupIds;
  }

  public ArrayList<SecurityGroupIdSetItemType> getItem( ) {
    return item;
  }

  public void setItem( ArrayList<SecurityGroupIdSetItemType> item ) {
    this.item = item;
  }
}
