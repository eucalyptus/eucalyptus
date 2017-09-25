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
package com.eucalyptus.imaging.common.msgs;

public class GetInstanceImportTaskResponseType extends ImagingMessage {

  private String importTaskId;
  private String importTaskType;
  private VolumeTask volumeTask;
  private InstanceStoreTask instanceStoreTask;

  public void GetInstanceImportTaskResponse( ) {
  }

  public String getImportTaskId( ) {
    return importTaskId;
  }

  public void setImportTaskId( String importTaskId ) {
    this.importTaskId = importTaskId;
  }

  public String getImportTaskType( ) {
    return importTaskType;
  }

  public void setImportTaskType( String importTaskType ) {
    this.importTaskType = importTaskType;
  }

  public VolumeTask getVolumeTask( ) {
    return volumeTask;
  }

  public void setVolumeTask( VolumeTask volumeTask ) {
    this.volumeTask = volumeTask;
  }

  public InstanceStoreTask getInstanceStoreTask( ) {
    return instanceStoreTask;
  }

  public void setInstanceStoreTask( InstanceStoreTask instanceStoreTask ) {
    this.instanceStoreTask = instanceStoreTask;
  }
}
