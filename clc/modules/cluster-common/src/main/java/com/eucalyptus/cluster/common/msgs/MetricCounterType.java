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
package com.eucalyptus.cluster.common.msgs;

import java.util.ArrayList;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class MetricCounterType extends EucalyptusData {

  private String type;
  private Long collectionIntervalMs;
  private ArrayList<MetricDimensionsType> dimensions = new ArrayList<MetricDimensionsType>( );

  public String getType( ) {
    return type;
  }

  public void setType( String type ) {
    this.type = type;
  }

  public Long getCollectionIntervalMs( ) {
    return collectionIntervalMs;
  }

  public void setCollectionIntervalMs( Long collectionIntervalMs ) {
    this.collectionIntervalMs = collectionIntervalMs;
  }

  public ArrayList<MetricDimensionsType> getDimensions( ) {
    return dimensions;
  }

  public void setDimensions( ArrayList<MetricDimensionsType> dimensions ) {
    this.dimensions = dimensions;
  }
}
