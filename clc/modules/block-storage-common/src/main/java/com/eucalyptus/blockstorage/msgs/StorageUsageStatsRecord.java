/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
