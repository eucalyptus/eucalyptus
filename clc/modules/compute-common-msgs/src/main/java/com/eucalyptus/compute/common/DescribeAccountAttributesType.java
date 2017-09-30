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
import java.util.List;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import com.google.common.collect.Lists;

public class DescribeAccountAttributesType extends VpcMessage {

  @HttpEmbedded
  private AccountAttributeNameSetType accountAttributeNameSet;
  @HttpParameterMapping( parameter = "Filter" )
  @HttpEmbedded( multiple = true )
  private ArrayList<Filter> filterSet = new ArrayList<Filter>( );

  public Iterable<String> attributeNames( ) {
    List<String> attributeNames = Lists.newArrayList( );
    if ( accountAttributeNameSet != null && accountAttributeNameSet.getItem( ) != null ) {
      for ( AccountAttributeNameSetItemType item : accountAttributeNameSet.getItem( ) ) {
        attributeNames.add( item.getAttributeName( ) );
      }

    }

    return attributeNames;
  }

  public AccountAttributeNameSetType getAccountAttributeNameSet( ) {
    return accountAttributeNameSet;
  }

  public void setAccountAttributeNameSet( AccountAttributeNameSetType accountAttributeNameSet ) {
    this.accountAttributeNameSet = accountAttributeNameSet;
  }

  public ArrayList<Filter> getFilterSet( ) {
    return filterSet;
  }

  public void setFilterSet( ArrayList<Filter> filterSet ) {
    this.filterSet = filterSet;
  }
}
