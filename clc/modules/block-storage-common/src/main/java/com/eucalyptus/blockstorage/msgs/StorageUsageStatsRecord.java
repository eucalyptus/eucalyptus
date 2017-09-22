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
package com.eucalyptus.blockstorage.msgs;

import edu.ucsb.eucalyptus.msgs.StatEventRecord;

public class StorageUsageStatsRecord extends StatEventRecord {

  private Long totalSpaceUsed;
  private Integer numberOfVolumes;

  public StorageUsageStatsRecord( ) { }

  public StorageUsageStatsRecord( final Integer numberOfVolumes, final Long totalSpaceUsed ) {
    super( "StorageController", System.getProperty( "euca.version" ) );
    this.totalSpaceUsed = totalSpaceUsed;
    this.numberOfVolumes = numberOfVolumes;
  }

  public static StorageUsageStatsRecord create( Integer numberOfVolumes, Long totalSpaceUsed ) {
    return new StorageUsageStatsRecord( numberOfVolumes, totalSpaceUsed );
  }

  public String toString( ) {
    return String.format( "Service: %s Version: %s Volumes: %d Space Used: %s", service, version, numberOfVolumes, totalSpaceUsed );
  }

  public Long getTotalSpaceUsed( ) {
    return totalSpaceUsed;
  }

  public void setTotalSpaceUsed( Long totalSpaceUsed ) {
    this.totalSpaceUsed = totalSpaceUsed;
  }

  public Integer getNumberOfVolumes( ) {
    return numberOfVolumes;
  }

  public void setNumberOfVolumes( Integer numberOfVolumes ) {
    this.numberOfVolumes = numberOfVolumes;
  }
}
