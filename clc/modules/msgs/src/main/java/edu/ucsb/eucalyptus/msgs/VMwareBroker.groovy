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

// parent to all requests/replies used by VMwareBroker/NC,
// which inherit correlationId and userId from EucalyptusMessage

public class VMwareBrokerRequestType extends EucalyptusMessage {
	String nodeName;

  	@Override
	public String toString() {
		toString("");
	}

  	@Override
    public String toString(String msg) {
    	return "VMwareBrokerRequest {" + msg + " userId=" + getUserId() + " correlationId=" + getCorrelationId() + " }";
    }

  	// extract the useful name from the class, for nicer logging
    private String simplifyClassName ()
    {
    	String name = getClass().getSimpleName();
    	if (name.startsWith("EucalyptusNCNc") && name.endsWith("Type")) {
    		name = name.substring(14, name.length()-5);
    	}
    	return name;
    }
}

public class VMwareBrokerResponseType extends EucalyptusMessage {
  	def VMwareBrokerResponseType() {}

  	@Override
  	public void setStatusMessage (String msg) {
  		// TODO: don't suppress the message once CC can accept statusMessage 
  	}
}

// fields used by {Run|Describe}Instance

public class VirtualMachineType extends EucalyptusData {
	Integer memory;
	Integer cores;
	Integer disk;
	
	VirtualMachineType () {}
	
	VirtualMachineType (int cores, int disk, int memory) {
		this.memory = memory;
		this.cores = cores;
		this.disk = disk;
	}
}

public class NetConfigType extends EucalyptusData {
	String macAddress;
	String ignoredMacAddress;
	int vlan;
	String ipAddress;
	String ignoredPublicIp;
	
	NetConfigType () {}
	
	NetConfigType (String macAddress, String ignoredMacAddress, int vlan, String ipAddress, String ignoredPublicIp) {
		this.macAddress = macAddress;
		this.ignoredMacAddress = ignoredMacAddress;
		this.vlan = vlan;
		this.ipAddress = ipAddress;
		this.ignoredPublicIp = ignoredPublicIp;
	}
}

public class VolumeType extends EucalyptusData {
	String volumeId;
	String remoteDevice;
	String device;
	String status;
	
	VolumeType () {}
	
	VolumeType (String volumeId, String remoteDevice, String device, String status) {
		this.volumeId = volumeId;
		this.remoteDevice = remoteDevice;
		this.device = device;
		this.status = status;
	}
}

public class InstanceType extends EucalyptusData {
	String reservationId;
	String instanceId;
	String imageId;
	String imageURL; // extra
	String kernelId;
	String kernelURL; // extra
	String ramdiskId;
	String ramdiskURL; // extra
	String userId;
	String keyName;
	VirtualMachineType instanceType;
	NetConfigType netParams;
	String stateName;
	String launchTime;
	String userData;
	String launchIndex;
	ArrayList<String> groupNames = new ArrayList<String>();
	ArrayList<VolumeType> volumes = new ArrayList<VolumeType>();
	String serviceTag;
}

// DescribeResource

public class EucalyptusNCNcDescribeResourceType extends VMwareBrokerRequestType {		
    String resourceType;
    def EucalyptusNCNcDescribeResourceType() {}
   
  	@Override
    public String toString() {
    	return super.toString(simplifyClassName() + " resourceType=" + getResourceType());
    	//return "}}}}} " + simplifyClassName() + " resourceType=" + getResourceType();

    }
}

public class EucalyptusNCNcDescribeResourceResponseType extends VMwareBrokerResponseType {
    String nodeStatus;
    int memorySizeMax;
    int memorySizeAvailable;
    int diskSizeMax;
    int diskSizeAvailable;
    int numberOfCoresMax;
    int numberOfCoresAvailable;
    String publicSubnets;
    def EucalyptusNCNcDescribeResourceResponseType() {}
}

// GetConsoleOutput

public class EucalyptusNCNcGetConsoleOutputType extends VMwareBrokerRequestType {
    String instanceId;
    def EucalyptusNCNcGetConsoleOutputType() {}
}

public class EucalyptusNCNcGetConsoleOutputResponseType extends VMwareBrokerResponseType {
    String consoleOutput;
    def EucalyptusNCNcGetConsoleOutputResponseType() {}
}

// DescribeInstances

public class EucalyptusNCNcDescribeInstancesType extends VMwareBrokerRequestType {
    ArrayList<String> instanceIds = new ArrayList<String>();
    def EucalyptusNCNcDescribeInstancesType() {}

  	@Override
    public String toString() {
    	return super.toString(simplifyClassName() + " instances={" + instanceIds + "}");
    	//return "}}}}} " + simplifyClassName() + " resourceType=" + getResourceType();

    }
}

public class EucalyptusNCNcDescribeInstancesResponseType extends VMwareBrokerResponseType {
	ArrayList<InstanceType> instances = new ArrayList<InstanceType>();
    def EucalyptusNCNcDescribeInstancesResponseType() {}
}

// RunInstance

public class EucalyptusNCNcRunInstanceType extends VMwareBrokerRequestType {
	String imageId;
	String kernelId;
	String ramdiskId;
	String imageURL;
	String kernelURL;
	String ramdiskURL;
	String instanceId;
	VirtualMachineType instanceType;
	String keyName;
	String publicMacAddress;
	String privateMacAddress;
	String reservationId;
	Integer vlan;
	String userData;
	String launchIndex;
	ArrayList<String> groupNames = new ArrayList<String>();
    def EucalyptusNCNcRunInstanceType() {}
}
	
public class EucalyptusNCNcRunInstanceResponseType extends VMwareBrokerResponseType {
    InstanceType instance;
    def EucalyptusNCNcRunInstanceResponseType() {}
}

// TerminateInstance

public class EucalyptusNCNcTerminateInstanceType extends VMwareBrokerRequestType {
    String instanceId;
    def EucalyptusNCNcTerminateInstanceType() {}
}

public class EucalyptusNCNcTerminateInstanceResponseType extends VMwareBrokerResponseType {
    String instanceId;
	String shutdownState;
	String previousState;
    def EucalyptusNCNcTerminateInstanceResponseType() {}
}

// StartNetwork

public class EucalyptusNCNcStartNetworkType extends VMwareBrokerRequestType {
	ArrayList<String> remoteHosts = new ArrayList<String>();
    Integer remoteHostPort;
    Integer vlan;
    def EucalyptusNCNcStartNetworkType() {}
}

public class EucalyptusNCNcStartNetworkResponseType extends VMwareBrokerResponseType {
    String networkStatus;
    def EucalyptusNCNcStartNetworkResponseType() {}
}

// RebootInstance

public class EucalyptusNCNcRebootInstanceType extends VMwareBrokerRequestType {
    String instanceId;
    def EucalyptusNCNcRebootInstanceType() {}
}

public class EucalyptusNCNcRebootInstanceResponseType extends VMwareBrokerResponseType {
    Boolean	status;
    def EucalyptusNCNcRebootInstanceResponseType() {}
}

// AttachVolume

public class EucalyptusNCNcAttachVolumeType extends VMwareBrokerRequestType {
    String instanceId;
    String volumeId;
    String remoteDev;
    String localDev;
    def EucalyptusNCNcAttachVolumeType() {}
}

public class EucalyptusNCNcAttachVolumeResponseType extends VMwareBrokerResponseType {
    def EucalyptusNCNcAttachVolumeResponseType() {}
}

// DetachVolume

public class EucalyptusNCNcDetachVolumeType extends VMwareBrokerRequestType {
    String instanceId;
    String volumeId;
    String remoteDev;
    String localDev;
    Boolean force;
    def EucalyptusNCNcDetachVolumeType() {}
}

public class EucalyptusNCNcDetachVolumeResponseType extends VMwareBrokerResponseType {
    def EucalyptusNCNcDetachVolumeResponseType() {}
}

// PowerDown

public class EucalyptusNCNcPowerDownType extends VMwareBrokerRequestType {
    def EucalyptusNCNcPowerDownType() {}

  	@Override
    public String toString() {
    	return super.toString(getClass().getSimpleName());
    }
}

public class EucalyptusNCNcPowerDownResponseType extends VMwareBrokerResponseType {
    def EucalyptusNCNcPowerDownResponseType() {}
}

// Template 
/*
public class EucalyptusNCNcXXXType extends VMwareBrokerRequestType {
    String instanceId;
    def EucalyptusNCNcXXXType() {}
}

public class EucalyptusNCNcXXXResponseType extends VMwareBrokerResponseType {
    String consoleOutput;
    def EucalyptusNCNcXXXResponseType() {}
}
*/

