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

@GroovyAddClassUUID
package com.eucalyptus.compute.common

import com.eucalyptus.binding.HttpParameterMapping
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID;

public class VmExportMessage extends ComputeMessage {
}
public class VmExportResponseMessage extends VmExportMessage {
}
/*********************************************************************************/
public class CreateInstanceExportTaskResponseType extends VmExportResponseMessage {
  ExportTaskResponse exportTask;
  public CreateInstanceExportTaskResponseType() {
  }
}
public class CreateInstanceExportTaskType extends VmExportMessage {
  String description;
  String instanceId;
  String targetEnvironment;
  ExportToS3Task exportToS3;
  public CreateInstanceExportTask() {
  }
}
/*********************************************************************************/
public class CancelExportTaskType extends VmExportMessage {
  String exportTaskId;
  public CancelExportTaskType() {
  }
}
public class CancelExportTaskResponseType extends VmExportResponseMessage {
  Boolean _return;
  public CancelExportTaskResponseType() {
  }
}
/*********************************************************************************/
public class DescribeExportTasksType extends VmExportMessage {
  @HttpParameterMapping( parameter = "ExportTaskId" )
  ArrayList<String> exportTaskIdSet = new ArrayList<String>();
  public DescribeExportTasksType() {
  }
}
public class DescribeExportTasksResponseType extends VmExportResponseMessage {
  ArrayList<ExportTaskResponse> exportTaskSet = new ArrayList<ExportTaskResponse>();
  public DescribeExportTasksResponseType() {
  }
}
/*********************************************************************************/
public class ExportTaskResponse extends EucalyptusData {
  String exportTaskId;
  String description;
  String state;
  String statusMessage;
  InstanceExportTaskResponse instanceExport;
  ExportToS3TaskResponse exportToS3;
  public ExportTaskResponseType() {
  }
}
public class ExportToS3TaskResponse extends EucalyptusData {
  String diskImageFormat;
  String containerFormat;
  String s3Bucket;
  String s3Key;
  public ExportToS3TaskResponseType() {
  }
}
public class InstanceExportTaskResponse extends EucalyptusData {
  String instanceId;
  String targetEnvironment;
  public InstanceExportTaskResponseType() {
  }
}
public class ExportToS3Task extends EucalyptusData {
  String diskImageFormat;
  String containerFormat;
  String s3Bucket;
  String s3Prefix;
  public ExportToS3TaskType() {
  }
}
