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
