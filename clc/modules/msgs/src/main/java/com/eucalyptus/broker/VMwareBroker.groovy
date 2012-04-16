package com.eucalyptus.broker

import java.util.ArrayList;
import java.util.Set;

import org.apache.log4j.Logger;
import com.eucalyptus.broker.vmware.VMwareBroker;
import com.eucalyptus.component.ComponentId.ComponentMessage;
import com.eucalyptus.util.EucalyptusCloudException;
import edu.ucsb.eucalyptus.msgs.BaseMessage
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.cloud.VirtualBootRecord;

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
 * Author: Dmitrii Zagorodnov <dmitrii@eucalyptus.com>
 */

// parent to all requests/replies used by VMwareBroker/NC,
// which inherit correlationId and userId from BaseMessage
@ComponentMessage(VMwareBroker.class)
public class BrokerRequestType extends BaseMessage {
	String nodeName;

  	@Override
	public String toString() {
		toString("");
	}

  	@Override
    public String toString(String msg) {
  		String paddedMsg = (msg.size()>0 && !msg.endsWith(" "))?(msg+" "):(msg);
    	return this.nodeName + ": BrokerRequest {" + paddedMsg + "userId=" + getUserId() + "}";
    }
	
	@Override public <TYPE extends BaseMessage> TYPE getReply( ) {
		TYPE r = super.getReply();
		((BrokerResponseType)r).nodeName = this.nodeName;
		return r;
	}
}

public class BrokerResponseType extends BaseMessage {
	String nodeName;
	
	public static final String STATUS_MSG_NOT_SUPPORTED = "method not supported";
	public static final String STATUS_MSG_SUCCESS = "method succeeded";
	public static final String STATUS_MSG_FAILURE = "method failed";

	private static Logger LOG = Logger.getLogger( BrokerResponseType.class );

  	def BrokerResponseType() {}

  	/*
  	@Override
  	public void setStatusMessage (String msg) {
  		// TODO: don't suppress the message once CC can accept statusMessage
  		// this.statusMessage = msg;
  	}
  	 */
  	
  	// unused (handler must cast the response)
  	public BrokerResponseType withSuccess () {
  		withSuccess (STATUS_MSG_SUCCESS); // default OK message
  	}
  	
  	public BrokerResponseType withSuccess (String msg) {
  		String logMsg = genSuccessMsg(msg);
  		if (logMsg!=null) {
  			LOG.info(this.nodeName + ": " + logMsg);
  		}
  		this.setStatusMessage (msg);
  		this.set_return (true);
  		return this;
  	}

  	// unused
  	public BrokerResponseType withFailure () {
  		withFailure (STATUS_MSG_NOT_SUPPORTED); // default error message
  	}
  	
  	public BrokerResponseType withFailure (String msg) throws EucalyptusCloudException {
  		LOG.error(this.nodeName + ": failure in " + this.getClass().getSimpleName() + ":" + msg);
  		this.setStatusMessage (msg);
  		this.set_return (false);
  		//throw EucalyptusCloudException(msg);
  		return this;
  	}

	protected String genSuccessMsg(String msg) {
		return null; // subclasses should override if they want to log anything on success
	}
}

// fields used by {Run|Describe}Instance

public class VirtualMachineType extends EucalyptusData {
	String name;
	Integer memory;
	Integer cores;
	Integer disk; // in Gibibytes (2^30)
	ArrayList<VirtualBootRecord> virtualBootRecord = new ArrayList<VirtualBootRecord>();
	
	VirtualMachineType () {}
	
	VirtualMachineType (String name, int cores, int disk, int memory) {
		this.name = name;
		this.memory = memory;
		this.cores = cores;
		this.disk = disk;
	}
}

public class NetConfigType extends EucalyptusData {
	String privateMacAddress;
	String privateIp;
	String publicIp;
	Integer vlan;
	Integer networkIndex;
	
	NetConfigType () {}
	
	NetConfigType (String macAddress, String privateIp, String publicIp, int vlan, int networkIndex) {
		this.privateMacAddress = macAddress;
		this.privateIp = privateIp;
		this.publicIp = publicIp;
		this.vlan = vlan;
		this.networkIndex = networkIndex;
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
	String uuid;
	String reservationId;
	String instanceId;
	String imageId;
	String imageURL; // extra
	String kernelId;
	String kernelURL; // extra
	String ramdiskId;
	String ramdiskURL; // extra
	String userId;
	String ownerId;
	String accountId;
	String keyName;
	VirtualMachineType instanceType;
	NetConfigType netParams;
	String stateName;
	String bundleTaskStateName;
	String createImageStateName;
	String launchTime;
	String expiryTime;
	Long blkbytes;
	Long netbytes;
	String userData;
	String launchIndex;
	String platform;
	ArrayList<String> groupNames = new ArrayList<String>();
	ArrayList<VolumeType> volumes = new ArrayList<VolumeType>();
	String serviceTag;

	public InstanceType() {}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((instanceId == null) ? 0 : instanceId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this.is(obj))
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InstanceType other = (InstanceType) obj;
		if (instanceId == null) {
			if (other.instanceId != null)
				return false;
		} else if (!instanceId.equals(other.instanceId))
			return false;
		return true;
	}
}

// class used by DescribeBundleTasks

public class BundleTaskType extends EucalyptusData {
	String instanceId;
	String state;
	String manifest;
	
	BundleTaskType () {}
	
	BundleTaskType (String instanceId, String state, String manifest) {
		this.instanceId = instanceId;
		this.state = state;
		this.manifest = manifest;
	}
}

// DescribeResource

public class EucalyptusNCNcDescribeResourceType extends BrokerRequestType {		
    String resourceType;
    def EucalyptusNCNcDescribeResourceType() {}
   
  	@Override
    public String toString() {
    	return super.toString ("DescribeResource: resourceType=" + this.resourceType);
    }
}

public class EucalyptusNCNcDescribeResourceResponseType extends BrokerResponseType {
    String nodeStatus;
    String iqn;
    int memorySizeMax;
    int memorySizeAvailable;
    int diskSizeMax;
    int diskSizeAvailable;
    int numberOfCoresMax;
    int numberOfCoresAvailable;
    String publicSubnets;
    def EucalyptusNCNcDescribeResourceResponseType() {}

    @Override
    String genSuccessMsg(String msg) {
    	return "BrokerReply {DescribeResource:" \
    	+ " status: " + nodeStatus \
    	+ " iqn: "    + iqn \
    	+ " cores: "  + numberOfCoresAvailable + "/" + numberOfCoresMax \
    	+ " mem: "    + memorySizeAvailable    + "/" + memorySizeMax \
    	+ " disk: "   + diskSizeAvailable      + "/" + diskSizeMax \
    	+ "}";
    }
}

// GetConsoleOutput

public class EucalyptusNCNcGetConsoleOutputType extends BrokerRequestType {
    String instanceId;
    def EucalyptusNCNcGetConsoleOutputType() {}

  	@Override
    public String toString() {
    	return super.toString ("GetConsoleOutput: instanceId=" + this.instanceId);
    }
}

public class EucalyptusNCNcGetConsoleOutputResponseType extends BrokerResponseType {
    String consoleOutput;
    def EucalyptusNCNcGetConsoleOutputResponseType() {}
}

// DescribeInstances

public class EucalyptusNCNcDescribeInstancesType extends BrokerRequestType {
    ArrayList<String> instanceIds = new ArrayList<String>();
    def EucalyptusNCNcDescribeInstancesType() {}

  	@Override
    public String toString() {
    	return super.toString ("DescribeInstances: instances=" + this.instanceIds);
    }
}

public class EucalyptusNCNcDescribeInstancesResponseType extends BrokerResponseType {
	ArrayList<InstanceType> instances = new ArrayList<InstanceType>();
    def EucalyptusNCNcDescribeInstancesResponseType() {}

    @Override
    String genSuccessMsg(String msg) {
    	String finalMsg = "BrokerReply {DescribeInstances:";
    	int totalInstances = 0;
    	for (InstanceType i : instances) {
    		if (totalInstances>0) {
    			finalMsg += ", ";
    		}
    		finalMsg += i.getInstanceId() + "/" + i.getStateName();
    		if (totalInstances>8) {
    			finalMsg += "...";
    			break;
    		}
    		totalInstances++;
    	}
    	finalMsg += "}";
    	return finalMsg;
    }
}

// RunInstance

public class EucalyptusNCNcRunInstanceType extends BrokerRequestType {
	String imageId;
	String kernelId;
	String ramdiskId;
	String imageURL;
	String kernelURL;
	String ramdiskURL;
	String ownerId;
	String accountId;
	String reservationId;
	String instanceId;
	String uuid;
	VirtualMachineType instanceType;
	String keyName;
	NetConfigType netParams;
	String userData;
	String launchIndex;
	String platform;
	String expiryTime;
	ArrayList<String> groupNames = new ArrayList<String>();
    def EucalyptusNCNcRunInstanceType() {}

  	@Override
    public String toString() {
    	return super.toString ("RunInstance: instanceId=" + this.instanceId + " imageId=" + this.imageId);
    }
}
	
public class EucalyptusNCNcRunInstanceResponseType extends BrokerResponseType {
    InstanceType instance;
    def EucalyptusNCNcRunInstanceResponseType() {}
}

// TerminateInstance

public class EucalyptusNCNcTerminateInstanceType extends BrokerRequestType {
    String instanceId;
    Boolean force;
    def EucalyptusNCNcTerminateInstanceType() {}

  	@Override
    public String toString() {
    	return super.toString ("TerminateInstance: instanceId=" + this.instanceId);
    }
}

public class EucalyptusNCNcTerminateInstanceResponseType extends BrokerResponseType {
    String instanceId;
	String shutdownState;
	String previousState;
    def EucalyptusNCNcTerminateInstanceResponseType() {}
}

// StartNetwork

public class EucalyptusNCNcStartNetworkType extends BrokerRequestType {
	ArrayList<String> remoteHosts = new ArrayList<String>();
    Integer remoteHostPort;
    Integer vlan;
    String uuid;
    def EucalyptusNCNcStartNetworkType() {}

  	@Override
    public String toString() {
    	return super.toString ("StartNetwork: remoteHosts=" + this.remoteHosts + " port=" + this.remoteHostPort + " vlan= "+ this.vlan);
    }
}

public class EucalyptusNCNcStartNetworkResponseType extends BrokerResponseType {
    String networkStatus;
    def EucalyptusNCNcStartNetworkResponseType() {}
}

// RebootInstance

public class EucalyptusNCNcRebootInstanceType extends BrokerRequestType {
    String instanceId;
    def EucalyptusNCNcRebootInstanceType() {}

  	@Override
    public String toString() {
    	return super.toString ("RebootInstance: instanceId=" + this.instanceId);
    }
}

public class EucalyptusNCNcRebootInstanceResponseType extends BrokerResponseType {
    Boolean	status;
    def EucalyptusNCNcRebootInstanceResponseType() {}
}

// AttachVolume

public class EucalyptusNCNcAttachVolumeType extends BrokerRequestType {
    String instanceId;
    String volumeId;
    String remoteDev;
    String localDev;
    def EucalyptusNCNcAttachVolumeType() {}

  	@Override
    public String toString() {
    	return super.toString ("AttachVolume: instanceId=" + this.instanceId + " volumeId=" + this.volumeId);
    }
}

public class EucalyptusNCNcAttachVolumeResponseType extends BrokerResponseType {
    def EucalyptusNCNcAttachVolumeResponseType() {}
}

// DetachVolume

public class EucalyptusNCNcDetachVolumeType extends BrokerRequestType {
    String instanceId;
    String volumeId;
    String remoteDev;
    String localDev;
    Boolean force;
    def EucalyptusNCNcDetachVolumeType() {}

  	@Override
    public String toString() {
    	return super.toString ("DetachVolume: instanceId=" + this.instanceId + " volumeId=" + this.volumeId);
    }
}

public class EucalyptusNCNcDetachVolumeResponseType extends BrokerResponseType {
    def EucalyptusNCNcDetachVolumeResponseType() {}
}

// PowerDown

public class EucalyptusNCNcPowerDownType extends BrokerRequestType {
    def EucalyptusNCNcPowerDownType() {}

  	@Override
    public String toString() {
    	return super.toString ("PowerDown: ");
    }
}

public class EucalyptusNCNcPowerDownResponseType extends BrokerResponseType {
    def EucalyptusNCNcPowerDownResponseType() {}
}

// BundleInstance 

public class EucalyptusNCNcBundleInstanceType extends BrokerRequestType {
    String instanceId;
    String bucketName;
    String filePrefix;
    String walrusURL;
    String userPublicKey;
    String cloudPublicKey;
    String S3Policy;
    String S3PolicySig;
    
    def EucalyptusNCNcBundleInstanceType() {}

  	@Override
    public String toString() {
    	return super.toString ("BundleInstance: instanceId=" + this.instanceId \
    			+ " bucketName=" + this.bucketName \
    			+ " filePrefix=" + this.filePrefix \
    			+ " walrusURL=" + this.walrusURL);
    }
}

public class EucalyptusNCNcBundleInstanceResponseType extends BrokerResponseType {
    def EucalyptusNCNcBundleInstanceResponseType() {}
}

// CancelBundle

public class EucalyptusNCNcCancelBundleType extends BrokerRequestType {
    String instanceId;
    
    def EucalyptusNCNcCancelBundleType() {}

  	@Override
    public String toString() {
    	return super.toString ("CancelBundle: instanceId=" + this.instanceId);
    }
}

public class EucalyptusNCNcCancelBundleResponseType extends BrokerResponseType {
    def EucalyptusNCNcCancelBundleResponseType() {}
}

// DescribeBundleTasks

public class EucalyptusNCNcDescribeBundleTasksType extends BrokerRequestType {
	ArrayList<String> instanceIds = new ArrayList<String>();
    def EucalyptusNCNcDescribeBundleTasksType() {}
}

public class EucalyptusNCNcDescribeBundleTasksResponseType extends BrokerResponseType {
	ArrayList<BundleTaskType> bundleTasks = new ArrayList<BundleTaskType>();
    def EucalyptusNCNcDescribeBundleTasksResponseType() {}
}

// AssignAddress 

public class EucalyptusNCNcAssignAddressType extends BrokerRequestType {
    String instanceId;
    String publicIp;
    def EucalyptusNCNcAssignAddressType() {}
}

public class EucalyptusNCNcAssignAddressResponseType extends BrokerResponseType {
    def EucalyptusNCNcAssignAddressResponseType() {}
}

// CreateImage 

public class EucalyptusNCNcCreateImageType extends BrokerRequestType {
    String instanceId;
    String volumeId;
    String remoteDev;
    def EucalyptusNCNcCreateImageType() {}
}

public class EucalyptusNCNcCreateImageResponseType extends BrokerResponseType {
    def EucalyptusNCNcCreateImageResponseType() {}
}

// Template 
/*
public class EucalyptusNCNcXXXType extends BrokerRequestType {
    String instanceId;
    def EucalyptusNCNcXXXType() {}
}

public class EucalyptusNCNcXXXResponseType extends BrokerResponseType {
    String consoleOutput;
    def EucalyptusNCNcXXXResponseType() {}
}
*/

