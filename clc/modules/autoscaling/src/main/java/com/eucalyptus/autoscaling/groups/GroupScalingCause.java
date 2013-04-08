/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.autoscaling.groups;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 */
@Embeddable
public class GroupScalingCause {

  @Temporal( TemporalType.TIMESTAMP)
  @Column(name = "metadata_timestamp", nullable = false, updatable = false)
  private Date timestamp;

  @Column(name = "metadata_detail", nullable = false, updatable = false)
  private String detail;

  public GroupScalingCause() {
  }

  public GroupScalingCause( final String detail ) {
    this.timestamp = new Date();
    this.detail = detail;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp( final Date timestamp ) {
    this.timestamp = timestamp;
  }

  public String getDetail() {
    return detail;
  }

  public void setDetail( final String detail ) {
    this.detail = detail;
  }
}
