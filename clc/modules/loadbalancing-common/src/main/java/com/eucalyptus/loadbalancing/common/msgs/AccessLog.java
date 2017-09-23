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
package com.eucalyptus.loadbalancing.common.msgs;

import javax.annotation.Nonnull;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class AccessLog extends EucalyptusData {

  private static final long serialVersionUID = 1L;
  @Nonnull
  private Boolean enabled;
  private String s3BucketName;
  private Integer emitInterval;
  private String s3BucketPrefix;

  public Boolean getEnabled( ) {
    return enabled;
  }

  public void setEnabled( Boolean enabled ) {
    this.enabled = enabled;
  }

  public String getS3BucketName( ) {
    return s3BucketName;
  }

  public void setS3BucketName( String s3BucketName ) {
    this.s3BucketName = s3BucketName;
  }

  public Integer getEmitInterval( ) {
    return emitInterval;
  }

  public void setEmitInterval( Integer emitInterval ) {
    this.emitInterval = emitInterval;
  }

  public String getS3BucketPrefix( ) {
    return s3BucketPrefix;
  }

  public void setS3BucketPrefix( String s3BucketPrefix ) {
    this.s3BucketPrefix = s3BucketPrefix;
  }
}
