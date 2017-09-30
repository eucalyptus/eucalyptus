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
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;

public class DescribeSnapshotsType extends BlockSnapshotMessage {

  @HttpParameterMapping( parameter = "SnapshotId" )
  private ArrayList<String> snapshotSet = new ArrayList<String>( );
  @HttpParameterMapping( parameter = "Owner" )
  private ArrayList<String> ownersSet = new ArrayList<String>( );
  @HttpParameterMapping( parameter = "RestorableBy" )
  private ArrayList<String> restorableBySet = new ArrayList<String>( );
  @HttpParameterMapping( parameter = "Filter" )
  @HttpEmbedded( multiple = true )
  private ArrayList<Filter> filterSet = new ArrayList<Filter>( );
  private Integer maxResults;
  private String nextToken;

  public ArrayList<String> getSnapshotSet( ) {
    return snapshotSet;
  }

  public void setSnapshotSet( ArrayList<String> snapshotSet ) {
    this.snapshotSet = snapshotSet;
  }

  public ArrayList<String> getOwnersSet( ) {
    return ownersSet;
  }

  public void setOwnersSet( ArrayList<String> ownersSet ) {
    this.ownersSet = ownersSet;
  }

  public ArrayList<String> getRestorableBySet( ) {
    return restorableBySet;
  }

  public void setRestorableBySet( ArrayList<String> restorableBySet ) {
    this.restorableBySet = restorableBySet;
  }

  public ArrayList<Filter> getFilterSet( ) {
    return filterSet;
  }

  public void setFilterSet( ArrayList<Filter> filterSet ) {
    this.filterSet = filterSet;
  }

  public Integer getMaxResults( ) {
    return maxResults;
  }

  public void setMaxResults( Integer maxResults ) {
    this.maxResults = maxResults;
  }

  public String getNextToken( ) {
    return nextToken;
  }

  public void setNextToken( String nextToken ) {
    this.nextToken = nextToken;
  }
}
