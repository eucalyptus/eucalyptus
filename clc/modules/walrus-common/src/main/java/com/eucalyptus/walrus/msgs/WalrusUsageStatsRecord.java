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
