/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

@GroovyAddClassUUID
package com.eucalyptus.compute.common

import edu.ucsb.eucalyptus.msgs.BaseMessage
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID;

public class VmExportMessage extends ComputeMessage {
  @Override
  public <TYPE extends BaseMessage> TYPE getReply( ) {
    VmExportResponseMessage reply = (VmExportResponseMessage) super.getReply( );
    reply.requestId = this.getCorrelationId( );
    return (TYPE) reply;
  }
}
public class VmExportResponseMessage extends VmExportMessage {
  protected String requestId;
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
