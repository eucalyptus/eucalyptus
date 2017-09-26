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

import java.util.ArrayList;
import java.util.Date;

public class ReportInstanceStatusType extends VmControlMessage {

  private ArrayList<String> instanceId;
  private String status;
  private Date startTime;
  private Date endTime;
  private ArrayList<String> reasonCode;
  private String description;

  public ArrayList<String> getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( ArrayList<String> instanceId ) {
    this.instanceId = instanceId;
  }

  public String getStatus( ) {
    return status;
  }

  public void setStatus( String status ) {
    this.status = status;
  }

  public Date getStartTime( ) {
    return startTime;
  }

  public void setStartTime( Date startTime ) {
    this.startTime = startTime;
  }

  public Date getEndTime( ) {
    return endTime;
  }

  public void setEndTime( Date endTime ) {
    this.endTime = endTime;
  }

  public ArrayList<String> getReasonCode( ) {
    return reasonCode;
  }

  public void setReasonCode( ArrayList<String> reasonCode ) {
    this.reasonCode = reasonCode;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }
}
