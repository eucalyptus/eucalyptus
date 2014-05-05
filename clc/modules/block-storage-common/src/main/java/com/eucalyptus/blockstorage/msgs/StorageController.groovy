/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

@GroovyAddClassUUID
package com.eucalyptus.blockstorage.msgs;

import com.eucalyptus.component.annotation.ComponentMessage;
import com.eucalyptus.blockstorage.Storage;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.ComponentMessageResponseType;
import edu.ucsb.eucalyptus.msgs.ComponentMessageType;
import edu.ucsb.eucalyptus.msgs.ComponentProperty;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import edu.ucsb.eucalyptus.msgs.StatEventRecord

import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID;

@ComponentMessage(Storage.class)
public class StorageRequestType extends BaseMessage {
	def StorageRequestType() {}
}

@ComponentMessage(Storage.class)
public class StorageResponseType extends BaseMessage {
	def StorageResponseType() {}
}

@ComponentMessage(Storage.class)
public class StorageErrorMessageType extends BaseMessage {
  String code
  String message
  Integer httpCode
  String requestId
  
	def StorageErrorMessageType() {
	}
	
	def StorageErrorMessageType(String code, String message, Integer httpCode, String requestId) {
		this.code = code;
		this.message = message;
		this.requestId = requestId;
		this.httpCode = httpCode;
	}
	
	public String toString() {
		return "StrorageErrorMessage:" + message;
	}
}

public class ExportVolumeType extends StorageRequestType {
	String volumeId;
	String token;
	String ip;
	String iqn;
	
	def ExportVolumeType() {}
}

public class ExportVolumeResponseType extends StorageResponseType {
	String volumeId;
	String connectionString;
	def ExportVolumeResponseType() {}
	
}

public class UnexportVolumeType extends StorageRequestType {
	String volumeId;
	String token;
	String ip;
	String iqn;
	def UnexportVolumeType() {}
}

public class UnexportVolumeResponseType extends StorageResponseType {
}


public class GetStorageVolumeType extends StorageRequestType {
	String volumeId;
}

public class GetStorageVolumeResponseType extends StorageResponseType {
	String volumeId;
	String size;
	String status;
	String createTime;
	String snapshotId;
	//These fields are implementation specific. Major and minor device numbers for AoE
	String actualDeviceName;
}

public class GetVolumeTokenType extends StorageRequestType {
	String volumeId;

	def GetVolumeTokenType() {}
	
	def GetVolumeTokenType(String vol) {
		this.volumeId = vol;
	}
}

public class GetVolumeTokenResponseType extends StorageResponseType {
	String volumeId;
	String token;
}

public class UpdateStorageConfigurationType extends StorageRequestType {
	String name;
	ArrayList<ComponentProperty> storageParams;
	
	def UpdateStorageConfigurationType() {}
}

public class UpdateStorageConfigurationResponseType extends StorageResponseType {
}

public class GetStorageConfigurationType extends StorageRequestType {
	String name;
	def GetStorageConfigurationType() {}
	
	def GetStorageConfigurationType(String name) {
		this.name = name;		
	}
}

public class GetStorageConfigurationResponseType extends StorageResponseType {
	String name;
	ArrayList<ComponentProperty> storageParams;
	def GetStorageConfigurationResponseType() {}
	
	def GetStorageConfigurationResponseType(String name, ArrayList<ComponentProperty> storageParams) {
		this.name = name;
		this.storageParams = storageParams;
	}
}

public class CreateStorageVolumeType extends StorageRequestType {
	String volumeId;
	String size;
	String snapshotId;
	String parentVolumeId;
	
	def CreateStorageVolumeType() {
	}
	
	def CreateStorageVolumeType(final String volumeId, final Integer size, final String snapshotId, final String parentVolumeId) {
		this.volumeId = volumeId;
		this.size = size;
		this.snapshotId = snapshotId;
		this.parentVolumeId = parentVolumeId;
	}
}

public class CreateStorageVolumeResponseType extends StorageResponseType {
	String size;
	String volumeId;
	String snapshotId;
	String status;
	String createTime;
}

public class CreateStorageSnapshotType extends StorageRequestType {
	String volumeId;
	String snapshotId;
	
	def CreateStorageSnapshotType(final String volumeId, final String snapshotId) {
		this.volumeId = volumeId;
		this.snapshotId = snapshotId;
	}
	
	def CreateStorageSnapshotType() {
	}
}
public class CreateStorageSnapshotResponseType extends StorageResponseType {
	String snapshotId;
	String volumeId;
	String status;
	String startTime;
	String progress;
}

public class DeleteStorageVolumeType extends StorageRequestType {
	String volumeId;
	
	def DeleteStorageVolumeType() {
	}
	
	def DeleteStorageVolumeType(final String volumeId) {
		this.volumeId = volumeId;
	}
	
}

public class DeleteStorageVolumeResponseType extends StorageResponseType {
}

public class DeleteStorageSnapshotType extends StorageRequestType {
	String snapshotId;
	
	def DeleteStorageSnapshotType() {
	}
	
	def DeleteStorageSnapshotType(final String snapshotId) {
		this.snapshotId = snapshotId;
	}
}

public class DeleteStorageSnapshotResponseType extends StorageResponseType {
}

public class StorageVolume extends EucalyptusData {
	
	String volumeId;
	String size;
	String snapshotId;
	String status;
	String createTime;
	String actualDeviceName;
	def StorageVolume() {}
	
	def StorageVolume(String volumeId) {
		this.volumeId = volumeId;
	}
}

public class DescribeStorageVolumesType extends StorageRequestType {
	ArrayList<String> volumeSet = new ArrayList<String>();
	
	def DescribeStorageVolumesType() {
	}
	
	def DescribeStorageVolumesType(ArrayList<String> volumeSet) {
		this.volumeSet = volumeSet;
	}
	
}
public class DescribeStorageVolumesResponseType extends StorageResponseType {
	ArrayList<StorageVolume> volumeSet = new ArrayList<StorageVolume>();
}

public class StorageSnapshot extends EucalyptusData {
	String snapshotId;
	String volumeId;
	String status;
	String startTime;
	String progress;
}

public class DescribeStorageSnapshotsType extends StorageRequestType {
	ArrayList<String> snapshotSet = new ArrayList<String>();
	
	def DescribeStorageSnapshotsType() {
	}
	
	def DescribeStorageSnapshotsType(ArrayList<String> snapshotSet) {
		this.snapshotSet = snapshotSet;
	}
	
}
public class DescribeStorageSnapshotsResponseType extends StorageResponseType {
	ArrayList<StorageSnapshot> snapshotSet = new ArrayList<StorageSnapshot>();
}

public class StorageComponentMessageType extends ComponentMessageType {

  @Override
  public String getComponent( ) {
    return "storage";
  }       
  
}

public class StorageComponentMessageResponseType extends ComponentMessageResponseType {
}

public class ConvertVolumesType extends StorageComponentMessageType {
	String originalProvider;
	
	def ConvertVolumesType() {		
	}
}

public class ConvertVolumesResponseType extends StorageComponentMessageResponseType {
	def ConvertVolumesResponseType() {		
	}
}

public class AttachStorageVolumeType extends StorageRequestType {
	String volumeId;
	String nodeIqn;
	
	def AttachStorageVolumeType() {}
	
	def AttachStorageVolumeType(String nodeIqn, String volumeId) {
		this.nodeIqn = nodeIqn;
		this.volumeId = volumeId;
	}
}

public class AttachStorageVolumeResponseType extends StorageResponseType {
	String remoteDeviceString;
	
	def AttachStorageVolumeResponseType() {}
	
	def AttachStorageVolumeResponseType(String remoteDeviceString) {
		this.remoteDeviceString = remoteDeviceString;
	}	
}

public class DetachStorageVolumeType extends StorageRequestType {
	String nodeIqn;
	String volumeId;
	
	def DetachStorageVolumeType() {}
	
  def DetachStorageVolumeType(String volumeId) {
    this.volumeId = volumeId;
  }
	def DetachStorageVolumeType(String nodeIqn, String volumeId) {
		this.nodeIqn = nodeIqn;
		this.volumeId = volumeId;
	}
}

public class DetachStorageVolumeResponseType extends StorageResponseType {	
	
	def DetachStorageVolumeResponseType() {}
}

public class StorageUsageStatsRecord extends StatEventRecord {
	Long totalSpaceUsed;
	Integer numberOfVolumes;
	
	def StorageUsageStatsRecord() {}
	
	def StorageUsageStatsRecord(final Integer numberOfVolumes, 
	final Long totalSpaceUsed) {			
		super("StorageController", System.getProperty("euca.version"));
		this.totalSpaceUsed = totalSpaceUsed;
		this.numberOfVolumes = numberOfVolumes;
	}
	
	public String toString() {
		return String.format("Service: %s Version: %s Volumes: %d Space Used: %s", 
		service, 
		version, 
		numberOfVolumes, 
		totalSpaceUsed);
	}
	
	public static StorageUsageStatsRecord create(Integer numberOfVolumes, Long totalSpaceUsed) {
		return new StorageUsageStatsRecord(numberOfVolumes, totalSpaceUsed);
	}
}

public class CloneVolumeType extends StorageRequestType {
	String volumeId;
	
	def CloneVolumeType() {}
}

public class CloneVolumeResponseType extends StorageResponseType {
	def CloneVolumeResponseType() {}
}
