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

public class DescribeSensorsType extends CloudClusterMessage {

  private Integer historySize;
  private Integer collectionIntervalTimeMs;
  private ArrayList<String> sensorIds = new ArrayList<String>( );
  private ArrayList<String> instanceIds = new ArrayList<String>( );

  public DescribeSensorsType( ) {
  }

  public DescribeSensorsType( Integer historySize, Integer collectionIntervalTimeMs, ArrayList<String> instanceIds ) {
    this.historySize = historySize;
    this.collectionIntervalTimeMs = collectionIntervalTimeMs;
    this.instanceIds = instanceIds;
  }

  public Integer getHistorySize( ) {
    return historySize;
  }

  public void setHistorySize( Integer historySize ) {
    this.historySize = historySize;
  }

  public Integer getCollectionIntervalTimeMs( ) {
    return collectionIntervalTimeMs;
  }

  public void setCollectionIntervalTimeMs( Integer collectionIntervalTimeMs ) {
    this.collectionIntervalTimeMs = collectionIntervalTimeMs;
  }

  public ArrayList<String> getSensorIds( ) {
    return sensorIds;
  }

  public void setSensorIds( ArrayList<String> sensorIds ) {
    this.sensorIds = sensorIds;
  }

  public ArrayList<String> getInstanceIds( ) {
    return instanceIds;
  }

  public void setInstanceIds( ArrayList<String> instanceIds ) {
    this.instanceIds = instanceIds;
  }
}
