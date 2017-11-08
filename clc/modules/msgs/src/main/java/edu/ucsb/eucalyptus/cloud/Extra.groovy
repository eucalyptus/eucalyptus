/*************************************************************************
 * Copyright 2008 Regents of the University of California
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

@GroovyAddClassUUID
package edu.ucsb.eucalyptus.cloud

import com.eucalyptus.component.Component
import com.eucalyptus.component.Faults

import edu.ucsb.eucalyptus.msgs.*
import com.google.common.base.Joiner

public class Pair {
  
  public static List<Pair> getPaired(List one, List two) {
    List<Pair> newList = new ArrayList<Pair>();
    for ( int idx = 0; idx < one.size(); idx++ )
      newList.add(new Pair(one[ idx ], two[ idx ]));
    return newList;
  }
  
  def String left, right;
  
  def Pair(final left, final right) {
    this.left = left;
    this.right = right;
  }
}

public class VmDescribeType extends CloudClusterMessage {
  
  ArrayList<String> instancesSet = new ArrayList<String>();
}
public class VmDescribeResponseType extends CloudClusterMessage {
  
  String originCluster;
  ArrayList<VmInfo> vms = new ArrayList<VmInfo>();
  public String toString() {
    return "${this.getClass().getSimpleName()} " + Joiner.on("\n${this.getClass().getSimpleName()} " as String).join(vms.iterator());
  }
}

public class VmRunResponseType extends CloudClusterMessage {
  
  ArrayList<VmInfo> vms = new ArrayList<VmInfo>();
}

public class VmInfo extends EucalyptusData {
  String uuid;
  String imageId;
  String kernelId;
  String ramdiskId;
  String instanceId;
  VmTypeInfo instanceType = new VmTypeInfo();
  String keyValue;
  Date launchTime;
  String stateName;
  NetworkConfigType netParams = new NetworkConfigType();
  String ownerId;
  String accountId;
  String reservationId;
  String serviceTag;
  String userData;
  String launchIndex;
  Long networkBytes = 0l;
  Long blockBytes = 0l;
  ArrayList<String> groupNames = new ArrayList<String>();
  ArrayList<AttachedVolume> volumes = new ArrayList<AttachedVolume>();
  String placement;
  String platform;
  String bundleTaskStateName;
  Double bundleTaskProgress;
  String createImageStateName;
  String guestStateName;
  //TODO:GRZE: these are to be cleaned up into a separate object rather than being munged in and possibly null.
  String migrationStateName;
  String migrationSource;
  String migrationDestination;
  List<NetworkConfigType> secondaryNetConfigList = new ArrayList<NetworkConfigType>();
  
  ArrayList<String> productCodes = new ArrayList<String>();
  
  @Override
  public String toString( ) {
    return "VmInfo ${reservationId} ${instanceId} ${ownerId} ${stateName} ${instanceType} ${imageId} ${kernelId} ${ramdiskId} ${launchIndex} ${serviceTag} ${netParams} ${volumes} ${migrationStateName} ${secondaryNetConfigList}";
  }
}

public class VirtualBootRecord extends EucalyptusData implements Cloneable {
  String id = "none";
  String resourceLocation = "none";
  String type;
  String guestDeviceName = "none";
  Long size = -1l;
  String format = "none";
  
  def VirtualBootRecord() {
  }
  
  def VirtualBootRecord(String id, String resourceLocation, String type, String guestDeviceName, Long sizeBytes, String format) {
    this.id = id;
    this.resourceLocation = resourceLocation;
    this.type = type;
    this.guestDeviceName = guestDeviceName;
    this.size = sizeBytes;
    this.format = format;
  }

    public boolean hasValidId() {
        return !"none".equals(this.id);
    }
  
  public VirtualBootRecord clone( ) {
    return (VirtualBootRecord) super.clone();
  }
}

public class VmKeyInfo extends EucalyptusData {
  
  String name = "";
  String value = "";
  String fingerprint = "";
  
  def VmKeyInfo() {
  }
  
  def VmKeyInfo(final name, final value, final fingerprint) {
    this.name = name;
    this.value = value;
    this.fingerprint = fingerprint;
  }
  
  @Override
  public String toString( ) {
    return String.format( "VmKeyInfo [fingerprint=%s, name=%s, value=%s]", this.fingerprint, this.name, this.value );
  }
}

public class NodeInfo implements Comparable {
  String iqn;
  String serviceTag;
  String name;
  String partition;
  Hypervisor hypervisor = Hypervisor.Unknown;
  Boolean hasClusterCert = false;
  Boolean hasNodeCert = false;
  Component.State lastState;
  Faults.CheckException lastException;
  String lastMessage;
  Date lastSeen;
  NodeCertInfo certs = new NodeCertInfo();
  NodeLogInfo logs = new NodeLogInfo();
  
  def NodeInfo() {
  }
  
  def NodeInfo(final String serviceTag ) {
    this.serviceTag = serviceTag;
    this.name = (new URI(this.serviceTag)).getHost();
    this.lastSeen = new Date();
    this.certs.setServiceTag(this.serviceTag);
    this.logs.setServiceTag(this.serviceTag);
  }
  
  def NodeInfo(final String partition, final NodeType nodeType) {
    this.partition = partition;
    this.serviceTag = nodeType.getServiceTag( );
    this.iqn = nodeType.getIqn( );
	this.hypervisor = Hypervisor.fromString(nodeType.getHypervisor());
    this.name = (new URI(this.serviceTag)).getHost();
    this.lastSeen = new Date();
    this.certs.setServiceTag(this.serviceTag);
    this.logs.setServiceTag(this.serviceTag);
  }
  
  def NodeInfo(NodeInfo orig, final NodeCertInfo certs) {
    this(orig.serviceTag);
    this.logs = orig.logs;
    this.certs = certs;
  }
  
  def NodeInfo(NodeInfo orig, final NodeLogInfo logs) {
    this(orig.serviceTag);
    this.certs = orig.certs;
    this.logs = logs;
  }
  
  def NodeInfo(final NodeCertInfo certs) {
    this(certs.getServiceTag());
    this.certs = certs;
  }
  
  def NodeInfo(final NodeLogInfo logs) {
    this(logs.getServiceTag());
    this.logs = logs;
  }
  
  
  public void touch(Component.State lastState, String lastMessage, Faults.CheckException lastEx ) {
    this.lastSeen = new Date();
    this.lastException = lastEx;
    this.lastState = lastState;
    this.lastMessage = lastMessage;
  }
  
  public int compareTo(Object o) {
    return this.serviceTag.compareTo(((NodeInfo) o).serviceTag);
  }
  
  boolean equals(final o) {
    if ( this.is( o ) ) return true;
    if ( !(o instanceof NodeInfo) ) return false;
    NodeInfo nodeInfo = (NodeInfo) o;
    if ( !serviceTag.equals(nodeInfo.serviceTag) ) return false;
    return true;
  }
  
  int hashCode() {
    return serviceTag.hashCode();
  }
  
  @Override
  public String toString( ) {
    return "NodeInfo name=${name} lastSeen=${lastSeen} serviceTag=${serviceTag} iqn=${iqn} hypervisor=${hypervisor}";
  }
  
  public enum Hypervisor {
	KVM(true), ESXI(false), Unknown(false);

	private final boolean supportsEkiEri;

	public Hypervisor(boolean supportsEkiEri) {
	  this.supportsEkiEri = supportsEkiEri;
	}

    public static Hypervisor fromString(String hypervisor) {
	  try {
	    return Hypervisor.valueOf(hypervisor);
	  } catch (IllegalArgumentException ex) {
	    return Unknown;
	  }
	}

    public boolean supportEkiEri() {
      return supportsEkiEri;
    }
  }
}
