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

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

/*********************************************************************************/
public class ExportTaskResponse extends EucalyptusData {

  private String exportTaskId;
  private String description;
  private String state;
  private String statusMessage;
  private InstanceExportTaskResponse instanceExport;
  private ExportToS3TaskResponse exportToS3;

  public void ExportTaskResponseType( ) {
  }

  public String getExportTaskId( ) {
    return exportTaskId;
  }

  public void setExportTaskId( String exportTaskId ) {
    this.exportTaskId = exportTaskId;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public String getState( ) {
    return state;
  }

  public void setState( String state ) {
    this.state = state;
  }

  public String getStatusMessage( ) {
    return statusMessage;
  }

  public void setStatusMessage( String statusMessage ) {
    this.statusMessage = statusMessage;
  }

  public InstanceExportTaskResponse getInstanceExport( ) {
    return instanceExport;
  }

  public void setInstanceExport( InstanceExportTaskResponse instanceExport ) {
    this.instanceExport = instanceExport;
  }

  public ExportToS3TaskResponse getExportToS3( ) {
    return exportToS3;
  }

  public void setExportToS3( ExportToS3TaskResponse exportToS3 ) {
    this.exportToS3 = exportToS3;
  }
}
