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

public class CreateInstanceExportTaskType extends VmExportMessage {

  private String description;
  private String instanceId;
  private String targetEnvironment;
  private ExportToS3Task exportToS3;

  public void CreateInstanceExportTask( ) {
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( String instanceId ) {
    this.instanceId = instanceId;
  }

  public String getTargetEnvironment( ) {
    return targetEnvironment;
  }

  public void setTargetEnvironment( String targetEnvironment ) {
    this.targetEnvironment = targetEnvironment;
  }

  public ExportToS3Task getExportToS3( ) {
    return exportToS3;
  }

  public void setExportToS3( ExportToS3Task exportToS3 ) {
    this.exportToS3 = exportToS3;
  }
}
