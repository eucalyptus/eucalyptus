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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

/**
 * Added 2012-05-01
 */
@GroovyAddClassUUID
package edu.ucsb.eucalyptus.msgs;

import java.math.BigInteger;
import java.util.ArrayList;
import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.binding.HttpEmbedded;

public class VmExportMessage extends EucalyptusMessage {}

/*********************************************************************************/
public class CreateInstanceExportTaskResponseType extends VmExportMessage {
  String requestId;
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
  @Override
  public <TYPE extends BaseMessage> TYPE getReply( ) {
    TYPE reply = super.getReply( );
    reply.requestId = this.getCorrelationId( );
    return reply;
  }  
}
/*********************************************************************************/
public class CancelExportTaskType extends VmExportMessage {
  String exportTaskId;
  public CancelExportTaskType() {
  }
  @Override
  public <TYPE extends BaseMessage> TYPE getReply( ) {
    TYPE reply = super.getReply( );
    reply.requestId = this.getCorrelationId( );
    return reply;
  }  
}
public class CancelExportTaskResponseType extends VmExportMessage {
  String requestId;
  Boolean _return;
  public CancelExportTaskResponseType() {
  }
}
/*********************************************************************************/
public class DescribeExportTasksType extends VmExportMessage {
  ArrayList<String> exportTaskIdSet = new ArrayList<String>();
  public DescribeExportTasksType() {
  }
  @Override
  public <TYPE extends BaseMessage> TYPE getReply( ) {
    TYPE reply = super.getReply( );
    reply.requestId = this.getCorrelationId( );
    return reply;
  }  
}
public class DescribeExportTasksResponseType extends VmExportMessage {
  String requestId;
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
