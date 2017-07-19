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
package com.eucalyptus.reporting.event;

import com.amazonaws.auth.policy.actions.S3Actions;
import javax.annotation.Nonnull;

public class S3ApiAccumulatedUsageEvent extends S3EventSupport<S3Actions> {

  private static final long serialVersionUID = 1L;

  private final long accumulatedValue;
  public enum ValueType {
    Counts("S3ApiCounts"),
    Bytes("S3ApiBytes");
    private final String valueType;
    private ValueType(String valueType) {
      this.valueType = valueType;
    }
    @Override
    public String toString() {
      return valueType;
    }
  }
  private final ValueType valueType;

  private final long startTime;
  private final long endTime;
  
  private S3ApiAccumulatedUsageEvent( 
      @Nonnull final String accountNumber,
      @Nonnull final S3Actions action,
      @Nonnull final String bucketName,
               final long accumulatedValue,
               final ValueType valueType,
               final long startTime,
               final long endTime
  ) {
    super( action, bucketName, null, null, accountNumber, null );
    this.accumulatedValue = accumulatedValue;
    this.valueType = valueType;
    this.startTime = startTime;
    this.endTime = endTime;
  }
  
  public static S3ApiAccumulatedUsageEvent with(
      @Nonnull final String accountNumber,
      @Nonnull final S3Actions action,
      @Nonnull final String bucketName,
               final long accumulatedValue,
               final ValueType valueType,
               final long startTime,
               final long endTime
  ) {
    return new S3ApiAccumulatedUsageEvent( accountNumber, action, bucketName,
        accumulatedValue, valueType, startTime, endTime );
  }

  public long getAccumulatedValue() {
    return accumulatedValue;
  }
  
  public ValueType getValueType() {
    return valueType;
  }

  public long getStartTime() {
    return startTime;
  }

  public long getEndTime() {
    return endTime;
  }

  @Override
  public String toString() {
    return "S3ApiAccumulatedUsageEvent "
        + "[accountNumber=" + getAccountNumber()
        + ", action=" + getAction()
        + ", bucketName=" + getBucketName()
        + ", accumulatedValue=" + getAccumulatedValue()
        + ", valueType=" + getValueType()
        + ", startTime=" + getStartTime()
        + ", endTime=" + getEndTime()
        + "]";
  }
}
