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
package com.eucalyptus.walrus.msgs;

import edu.ucsb.eucalyptus.msgs.StatEventRecord;

public class WalrusUsageStatsRecord extends StatEventRecord {

  private Long bytesIn;
  private Long bytesOut;
  private Integer numberOfBuckets;
  private Long totalSpaceUsed;

  public WalrusUsageStatsRecord( ) { }

  public WalrusUsageStatsRecord( Long bytesIn, Long bytesOut, Integer numberOfBuckets, Long totalSpaceUsed ) {
    super( "WalrusBackend", System.getProperty( "euca.version" ) );
    this.bytesIn = bytesIn;
    this.bytesOut = bytesOut;
    this.numberOfBuckets = numberOfBuckets;
    this.totalSpaceUsed = totalSpaceUsed;
  }

  public static WalrusUsageStatsRecord create( Long bytesIn, Long bytesOut, Integer numberOfBuckets, Long totalSpaceUsed ) {
    return new WalrusUsageStatsRecord( bytesIn, bytesOut, numberOfBuckets, totalSpaceUsed );
  }

  public String toString( ) {
    return String.format( "Service: %s Version: %s Bytes In: %s Bytes Out: %s Buckets: %d Space Used: %s", service, version, bytesIn, bytesOut, numberOfBuckets, totalSpaceUsed );
  }

  public Long getBytesIn( ) {
    return bytesIn;
  }

  public void setBytesIn( Long bytesIn ) {
    this.bytesIn = bytesIn;
  }

  public Long getBytesOut( ) {
    return bytesOut;
  }

  public void setBytesOut( Long bytesOut ) {
    this.bytesOut = bytesOut;
  }

  public Integer getNumberOfBuckets( ) {
    return numberOfBuckets;
  }

  public void setNumberOfBuckets( Integer numberOfBuckets ) {
    this.numberOfBuckets = numberOfBuckets;
  }

  public Long getTotalSpaceUsed( ) {
    return totalSpaceUsed;
  }

  public void setTotalSpaceUsed( Long totalSpaceUsed ) {
    this.totalSpaceUsed = totalSpaceUsed;
  }
}
