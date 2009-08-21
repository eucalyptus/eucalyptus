package edu.ucsb.eucalyptus.msgs
/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */
public class StorageResponseType extends EucalyptusMessage {
	def StorageResponseType() {}
}

public class StorageRequestType extends EucalyptusMessage {
	def StorageRequestType() {}
}


public class StorageErrorMessageType extends EucalyptusMessage {
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

public class InitializeStorageManagerType extends StorageRequestType {
}

public class InitializeStorageManagerResponseType extends StorageResponseType {
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

public class UpdateStorageConfigurationType extends StorageRequestType {
	String name;
	String storageRootDirectory;
	Integer maxTotalVolumeSize;
	Integer maxVolumeSize;
	String storageInterface;
	Boolean zeroFillVolumes;

	def UpdateStorageConfigurationType() {}

	def UpdateStorageConfigurationType(StorageStateType storageState) {
		this.name = storageState.getName();
		this.storageRootDirectory = storageState.getVolumesPath();
		this.maxTotalVolumeSize = storageState.getMaxVolumeSizeInGB();
		this.maxVolumeSize = storageState.getTotalVolumesSizeInGB();
		this.storageInterface = storageState.getStorageInterface();
		this.zeroFillVolumes = storageState.getZeroFillVolumes();
	}
}

public class UpdateStorageConfigurationResponseType extends StorageResponseType {
}

public class CreateStorageVolumeType extends StorageRequestType {
	String volumeId;
	String size;
	String snapshotId;

	def CreateStorageVolumeType() {
	}

	def CreateStorageVolumeType(final String volumeId, final String size, final String snapshotId) {
		this.volumeId = volumeId;
		this.size = size;
		this.snapshotId = snapshotId;
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

	def DescribeStorageVolumesType(final volumeSet) {
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

	def DescribeStorageSnapshotsType(final snapshotSet) {
		this.snapshotSet = snapshotSet;
	}

}
public class DescribeStorageSnapshotsResponseType extends StorageResponseType {
	ArrayList<StorageSnapshot> snapshotSet = new ArrayList<StorageSnapshot>();
}

public class StorageControllerHeartbeatMessage extends HeartbeatMessage {

	def StorageControllerHeartbeatMessage() {}

	def StorageControllerHeartbeatMessage(final String heartbeatId) {
		super(heartbeatId);
	}
}

public class StorageUsageStatsRecord extends StatEventRecord {
	Long totalSpaceUsed;
	Integer numberOfVolumes;
	
	def StorageUsageStatsRecord() {}

	def StorageUsageStatsRecord(final Integer numberOfVolumes, 
			final Long totalSpaceUsed) {			
		super("StorageController", "Unknown");
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
