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

public class MetricDimensionsType extends EucalyptusData {

  private String dimensionName;
  private Long sequenceNum;
  private ArrayList<MetricDimensionsValuesType> values = new ArrayList<MetricDimensionsValuesType>( );

  public String getDimensionName( ) {
    return dimensionName;
  }

  public void setDimensionName( String dimensionName ) {
    this.dimensionName = dimensionName;
  }

  public Long getSequenceNum( ) {
    return sequenceNum;
  }

  public void setSequenceNum( Long sequenceNum ) {
    this.sequenceNum = sequenceNum;
  }

  public ArrayList<MetricDimensionsValuesType> getValues( ) {
    return values;
  }

  public void setValues( ArrayList<MetricDimensionsValuesType> values ) {
    this.values = values;
  }
}
