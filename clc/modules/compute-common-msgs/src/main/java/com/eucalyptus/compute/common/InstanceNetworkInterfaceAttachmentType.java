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
package com.eucalyptus.compute.common;

import java.util.Date;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class InstanceNetworkInterfaceAttachmentType extends EucalyptusData {

  private String attachmentId;
  private Integer deviceIndex;
  private String status;
  private Date attachTime;
  private Boolean deleteOnTermination;

  public InstanceNetworkInterfaceAttachmentType( ) {
  }

  public InstanceNetworkInterfaceAttachmentType( final String attachmentId, final Integer deviceIndex, final String status, final Date attachTime, final Boolean deleteOnTermination ) {
    this.attachmentId = attachmentId;
    this.deviceIndex = deviceIndex;
    this.status = status;
    this.attachTime = attachTime;
    this.deleteOnTermination = deleteOnTermination;
  }

  public String getAttachmentId( ) {
    return attachmentId;
  }

  public void setAttachmentId( String attachmentId ) {
    this.attachmentId = attachmentId;
  }

  public Integer getDeviceIndex( ) {
    return deviceIndex;
  }

  public void setDeviceIndex( Integer deviceIndex ) {
    this.deviceIndex = deviceIndex;
  }

  public String getStatus( ) {
    return status;
  }

  public void setStatus( String status ) {
    this.status = status;
  }

  public Date getAttachTime( ) {
    return attachTime;
  }

  public void setAttachTime( Date attachTime ) {
    this.attachTime = attachTime;
  }

  public Boolean getDeleteOnTermination( ) {
    return deleteOnTermination;
  }

  public void setDeleteOnTermination( Boolean deleteOnTermination ) {
    this.deleteOnTermination = deleteOnTermination;
  }
}
