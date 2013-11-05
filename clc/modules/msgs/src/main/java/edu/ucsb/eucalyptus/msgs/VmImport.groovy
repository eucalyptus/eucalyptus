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
 * Added 2010-11-15
 */
@GroovyAddClassUUID
package edu.ucsb.eucalyptus.msgs;

import groovy.transform.TypeChecked;
import java.math.BigInteger;
import java.util.ArrayList;
import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.binding.HttpEmbedded;

public class VmImportMessage extends EucalyptusMessage {
}
/*********************************************************************************/
public class ImportInstanceType extends VmImportMessage {
  String description;
  ImportInstanceLaunchSpecification launchSpecification;
  @HttpEmbedded(multiple = true)
  @HttpParameterMapping (parameter = "DiskImage")
  ArrayList<DiskImage> diskImageSet = new ArrayList<DiskImage>();
  Boolean keepPartialImports;
  String platform;
  public ImportInstance() {}
  @Override
  public <TYPE extends BaseMessage> TYPE getReply( ) {
    TYPE reply = super.getReply( );
    reply.requestId = this.getCorrelationId( );
    return reply;
  }  
}
public class ImportInstanceResponseType extends VmImportMessage {
  String requestId;
  ConversionTask conversionTask;
  public ImportInstanceResponse() {}
}
public class ImportInstanceLaunchSpecification extends EucalyptusData {
  String architecture;
  @HttpEmbedded(multiple = true)
  @HttpParameterMapping (parameter = "SecurityGroup")
  ArrayList<ImportInstanceGroup> groupSet = new ArrayList<ImportInstanceGroup>();
  UserData userData;
  String instanceType;
  InstancePlacement placement;
  MonitoringInstance monitoring;
  String subnetId;
  String instanceInitiatedShutdownBehavior;
  String privateIpAddress;
  public ImportInstanceLaunchSpecification() {}
}
public class ImportInstanceGroup extends EucalyptusData {
  String groupId;
  String groupName;
  public ImportInstanceGroupItem() {}
}
public class DiskImage extends EucalyptusData {
  DiskImageDetail image;
  String description;
  DiskImageVolume volume;
  public DiskImage() {}
}
public class UserData extends EucalyptusData {
  String data;
  String version;
  String encoding;
  public UserData() {}
}
public class InstancePlacement extends EucalyptusData {
  String availabilityZone;
  String groupName;
  public InstancePlacement() {}
}
public class MonitoringInstance extends EucalyptusData {
  Boolean enabled;
  public MonitoringInstance() {}
}
/*********************************************************************************/
public class DescribeConversionTasksType extends VmImportMessage {
  @HttpParameterMapping (parameter = "ConversionTaskId")
  ArrayList<String> conversionTaskIdSet = new ArrayList<String>();
  public DescribeConversionTasks() {}
  @Override
  public <TYPE extends BaseMessage> TYPE getReply( ) {
    TYPE reply = super.getReply( );
    reply.requestId = this.getCorrelationId( );
    return reply;
  }  
}
public class DescribeConversionTasksResponseType extends VmImportMessage {
  String requestId;
  ArrayList<ConversionTask> conversionTasks = new ArrayList<ConversionTask>();
  public DescribeConversionTasksResponse() {}
}
/*********************************************************************************/
public class ImportVolumeType extends VmImportMessage {
  String availabilityZone;
  DiskImageDetail image;
  String description;
  DiskImageVolume volume;
  public ImportVolume() {}
  @Override
  public <TYPE extends BaseMessage> TYPE getReply( ) {
    TYPE reply = super.getReply( );
    reply.requestId = this.getCorrelationId( );
    return reply;
  }  
}
public class ImportVolumeResponseType extends VmImportMessage {
  String requestId;
  ConversionTask conversionTask;
  public ImportVolumeResponse() {}
}
public class DiskImageDetail extends EucalyptusData {
  String format;
  Long bytes;
  String importManifestUrl;
  public DiskImageDetail() {}
}
public class DiskImageVolume extends EucalyptusData {
  BigInteger size;
  public DiskImageVolume() {}
}
/*********************************************************************************/
public class CancelConversionTaskType extends VmImportMessage {
  String conversionTaskId;
  public CancelConversionTask() {}
  @Override
  public <TYPE extends BaseMessage> TYPE getReply( ) {
    TYPE reply = super.getReply( );
    reply.requestId = this.getCorrelationId( );
    return reply;
  }  
}
public class CancelConversionTaskResponseType extends VmImportMessage {
  String requestId;
  Boolean _return;
  public CancelConversionTaskResponse() {}
}
/*********************************************************************************/
public class ConversionTask extends EucalyptusData {
  String conversionTaskId;
  String expirationTime;
  ImportVolumeTaskDetails importVolume;
  ImportInstanceTaskDetails importInstance;
  String state;
  String statusMessage;
  @HttpEmbedded(multiple = true)
  @HttpParameterMapping (parameter = "ResourceTag")
  ArrayList<ImportResourceTag> resourceTagSet = new ArrayList<ImportResourceTag>();
  public ConversionTask() {}
}
public class ImportResourceTag extends EucalyptusData {
  String key;
  String value;
  public ImportResourceTag() {}
  public ImportResourceTag( String key, String value ) {
    this.key = key;
    this.value = value;
  }
}
public class ImportVolumeTaskDetails extends EucalyptusData {
  Long bytesConverted;
  String availabilityZone;
  String description;
  DiskImageDescription image;
  DiskImageVolumeDescription volume;
  public ImportVolumeTaskDetails() {}
}
public class ImportInstanceTaskDetails extends EucalyptusData {
  @HttpEmbedded(multiple = true)
  @HttpParameterMapping (parameter = "Volume")
  ArrayList<ImportInstanceVolumeDetail> volumes = new ArrayList<ImportInstanceVolumeDetail>();
  String instanceId;
  String platform;
  String description;
  public ImportInstanceTaskDetails() {}
  public ImportInstanceTaskDetails( String instanceId, String platform, String description, ArrayList<ImportInstanceVolumeDetail> volumes ) {
    this.volumes = volumes;
    this.instanceId = instanceId;
    this.platform = platform;
    this.description = description;
  }
}
public class ImportInstanceVolumeDetail extends EucalyptusData {
  Long bytesConverted;
  String availabilityZone;
  DiskImageDescription image;
  String description;
  DiskImageVolumeDescription volume;
  String status;
  String statusMessage;
  public ImportInstanceVolumeDetailItem() {}
  public ImportInstanceVolumeDetail( String status, String statusMessage, Long bytesConverted, String availabilityZone, String description, DiskImageDescription image, DiskImageVolumeDescription volume ) {
    this.bytesConverted = bytesConverted;
    this.availabilityZone = availabilityZone;
    this.description = description;
    this.status = status;
    this.statusMessage = statusMessage;
    this.image = image;
    this.volume = volume;
  }
}
public class DiskImageDescription extends EucalyptusData {
  String format;
  Long size;
  String importManifestUrl;
  String checksum;
  public DiskImageDescription() {}
  public DiskImageDescription( String format, Long size, String importManifestUrl, String checksum ) {
    this.format = format;
    this.size = size;
    this.importManifestUrl = importManifestUrl;
    this.checksum = checksum;
  }
}
public class DiskImageVolumeDescription extends EucalyptusData {
  BigInteger size;
  String id;
  public DiskImageVolumeDescription() {}
  public DiskImageVolumeDescription( BigInteger size, String id ) {
    this.size = size;
    this.id = id;
  }
}