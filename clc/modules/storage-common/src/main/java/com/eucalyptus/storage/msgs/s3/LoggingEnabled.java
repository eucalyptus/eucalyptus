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
package com.eucalyptus.storage.msgs.s3;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class LoggingEnabled extends EucalyptusData {

  private String targetBucket;
  private String targetPrefix;
  private TargetGrants targetGrants;

  public LoggingEnabled( ) {
  }

  public LoggingEnabled( TargetGrants grants ) {
    targetGrants = grants;
  }

  public LoggingEnabled( String bucket, String prefix, TargetGrants grants ) {
    targetBucket = bucket;
    targetPrefix = prefix;
    targetGrants = grants;
  }

  public String getTargetBucket( ) {
    return targetBucket;
  }

  public void setTargetBucket( String targetBucket ) {
    this.targetBucket = targetBucket;
  }

  public String getTargetPrefix( ) {
    return targetPrefix;
  }

  public void setTargetPrefix( String targetPrefix ) {
    this.targetPrefix = targetPrefix;
  }

  public TargetGrants getTargetGrants( ) {
    return targetGrants;
  }

  public void setTargetGrants( TargetGrants targetGrants ) {
    this.targetGrants = targetGrants;
  }
}
