package edu.ucsb.eucalyptus.cloud

import edu.ucsb.eucalyptus.constants.HasName
import edu.ucsb.eucalyptus.msgs.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

public class ClusterStateType {

  String name;
  String host;
  int port;


  def ClusterStateType(final name, final host, final port) {
    this.name = name;
    this.host = host;
    this.port = port;
  }

}

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
public interface RequestTransactionScript extends Serializable {
  public EucalyptusMessage getRequest();
}
public class VmAllocationInfo extends RequestTransactionScript {

  RunInstancesType request;
  RunInstancesResponseType reply;
  String userData;
  Long reservationIndex;
  String reservationId;
  VmImageInfo imageInfo;
  VmKeyInfo keyInfo;
  VmTypeInfo vmTypeInfo;

  List<Network> networks = new ArrayList<Network>();

  List<ResourceToken> allocationTokens = new ArrayList<ResourceToken>();

  def VmAllocationInfo() {}

  def VmAllocationInfo(final RunInstancesType request) {
    this.request = request;
    this.reply = request.getReply();
  }
}

public class VmDescribeType extends EucalyptusMessage {

  ArrayList<String> instancesSet = new ArrayList<String>();
}
public class VmDescribeResponseType extends EucalyptusMessage {

  String originCluster;
  ArrayList<VmInfo> vms = new ArrayList<VmInfo>();
}

public class VmRunResponseType extends EucalyptusMessage {

  ArrayList<VmInfo> vms = new ArrayList<VmInfo>();
}

public class VmInfo extends EucalyptusData {

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
  String reservationId;
  String serviceTag;
  String userData;
  String launchIndex;
  ArrayList<String> groupNames = new ArrayList<String>();
  ArrayList<AttachedVolume> volumes = new ArrayList<AttachedVolume>();

  String placement;

  ArrayList<String> productCodes = new ArrayList<String>();
}

public class VmRunType extends EucalyptusMessage {

  /** these are for more convenient binding later on but really should be done differently... sigh    **/

  String reservationId, userData;
  int min, max, vlan, launchIndex;

  VmImageInfo imageInfo;
  VmTypeInfo vmTypeInfo;
  VmKeyInfo keyInfo;

  List<String> instanceIds = new ArrayList<String>();
  List<String> macAddresses = new ArrayList<String>();
  List<String> networkNames = new ArrayList<String>();


  def VmRunType() {}

  def VmRunType(final RunInstancesType request,
                final String reservationId, final String userData, final int amount,
                final VmImageInfo imageInfo, final VmTypeInfo vmTypeInfo, final VmKeyInfo keyInfo,
                final List<String> instanceIds, final List<String> macAddresses,
                final int vlan, final List<String> networkNames) {
    this.correlationId = request.correlationId;
    this.userId = request.userId;
    this.effectiveUserId = request.effectiveUserId;
    this.reservationId = reservationId;
    this.userData = userData;
    this.min = amount;
    this.max = amount;
    this.vlan = vlan;
    this.imageInfo = imageInfo;
    this.vmTypeInfo = vmTypeInfo;
    this.keyInfo = keyInfo;
    this.instanceIds = instanceIds;
    this.macAddresses = macAddresses;
    this.networkNames = networkNames;
  }

  def VmRunType(RunInstancesType request) {
    this.effectiveUserId = request.effectiveUserId;
    this.correlationId = request.correlationId;
    this.userId = request.userId;
  }




  public String toString() {
    /** TODO-1.4: do something reasonable here    **/
    return this.correlationId;
  }

}

public class VmImageInfo {

  String imageId;
  String kernelId;
  String ramdiskId;
  String imageLocation;
  String kernelLocation;
  String ramdiskLocation;

  def VmImageInfo(final imageId, final kernelId, final ramdiskId, final imageLocation, final kernelLocation, final ramdiskLocation) {
    this.imageId = imageId;
    this.kernelId = kernelId;
    this.ramdiskId = ramdiskId;
    this.imageLocation = imageLocation;
    this.kernelLocation = kernelLocation;
    this.ramdiskLocation = ramdiskLocation;
  }

  def VmImageInfo() {}

}

public class VmKeyInfo {

  String name = "";
  String value = "";
  String fingerprint = "";

  def VmKeyInfo(final name, final value, final fingerprint) {
    this.name = name;
    this.value = value;
    this.fingerprint = fingerprint;
  }

  def VmKeyInfo() {}

}

public class Network implements HasName {

  String name;
  String networkName;
  String userName;
  ArrayList<PacketFilterRule> rules = new ArrayList<PacketFilterRule>();
  ConcurrentMap<String, NetworkToken> networkTokens = new ConcurrentHashMap<String, NetworkToken>();

  def Network() {}

  def Network(final String userName, final String networkName) {
    this.userName = userName;
    this.networkName = networkName;
    this.name = this.userName + "-" + this.networkName;
  }


  public NetworkToken addTokenIfAbsent(NetworkToken token) {
    this.networkTokens.putIfAbsent(token.getCluster(), token);
  }

  public NetworkToken getToken(String cluster) {
    return this.networkTokens.get(cluster);
  }

  public boolean hasToken(String cluster) {
    return this.networkTokens.get(cluster);
  }


  public NetworkToken removeToken(String cluster) {
    return this.networkTokens.remove(cluster);
  }

  public int compareTo(final Object o) {
    Network that = (Network) o;
    return this.getName().compareTo(that.getName());
  }

  @Override
  public String toString() {
    return "Network{" +
           "name='" + name + '\'' +
           ", networkName='" + networkName + '\'' +
           ", userName='" + userName + '\'' +
           ", rules=" + rules +
           ", networkTokens=" + networkTokens +
           '}';
  }

}

public class NetworkToken implements Comparable {

  String networkName;
  String cluster;
  int vlan;
  String userName;
  String name;

  def NetworkToken(final String cluster, final String userName, final String networkName, final int vlan) {
    this.networkName = networkName;
    this.cluster = cluster;
    this.vlan = vlan;
    this.userName = userName;
    this.name = this.userName + "-" + this.networkName;
  }

  @Override
  boolean equals(final Object o) {
    if ( this == o ) return true;
    if ( !(o instanceof NetworkToken) ) return false;
    NetworkToken that = (NetworkToken) o;

    if ( !cluster.equals(that.cluster) ) return false;
    if ( !networkName.equals(that.networkName) ) return false;
    if ( !userName.equals(that.userName) ) return false;

    return true;
  }

  @Override
  int hashCode() {
    int result;

    result = networkName.hashCode();
    result = 31 * result + cluster.hashCode();
    result = 31 * result + userName.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "NetworkToken{" +
           "networkName='" + networkName + '\'' +
           ", cluster='" + cluster + '\'' +
           ", vlan=" + vlan +
           ", userName='" + userName + '\'' +
           ", name='" + name + '\'' +
           '}';
  }

  @Override
  public int compareTo(Object o) {
    NetworkToken that = (NetworkToken) o;
    return (!this.cluster.equals(that.cluster) && (this.vlan == that.vlan)) ? this.vlan - that.vlan : this.cluster.compareTo(that.cluster);
  }

  public StopNetworkType getStopMessage() {
    return new StopNetworkType(this.vlan, this.name);
  }

}

public class ResourceToken implements Comparable {

  String cluster;
  String correlationId;
  String userName;
  ArrayList<String> instanceIds = new ArrayList<String>();
  ArrayList<NetworkToken> networkTokens = new ArrayList<NetworkToken>();
  int amount;
  String vmType;
  Date creationTime;
  int sequenceNumber;

  public ResourceToken(final String cluster, final String correlationId, final String userName, final int amount, final int sequenceNumber, final String vmType) {
    this.cluster = cluster;
    this.correlationId = correlationId;
    this.userName = userName;
    this.amount = amount;
    this.sequenceNumber = sequenceNumber;
    this.creationTime = Calendar.getInstance().getTime();
    this.vmType = vmType;
  }



  @Override
  public boolean equals(final Object o) {
    if ( this == o ) return true;
    if ( !(o instanceof ResourceToken) ) return false;

    ResourceToken that = (ResourceToken) o;

    if ( amount != that.amount ) return false;
    if ( !cluster.equals(that.cluster) ) return false;
    if ( !correlationId.equals(that.correlationId) ) return false;
    if ( !creationTime.equals(that.creationTime) ) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = cluster.hashCode();
    result = 31 * result + correlationId.hashCode();
    result = 31 * result + amount;
    result = 31 * result + creationTime.hashCode();
    return result;
  }

  @Override
  public int compareTo(final Object o) {
    ResourceToken that = (ResourceToken) o;
    return this.getSequenceNumber() - that.getSequenceNumber();
  }

  @Override
  public String toString() {
    return String.format("ResourceToken={ cluster=%10s, vmType=%10s, amount=%04d ]", this.cluster, this.vmType, this.amount);
  }

}

public class NodeInfo implements Comparable {

  String serviceTag;
  String name;
  Date lastSeen;
  NodeCertInfo certs = new NodeCertInfo();
  NodeLogInfo logs = new NodeLogInfo();

  @Override
  public String toString() {
    return "NodeInfo{" +
           "serviceTag='" + serviceTag.replaceAll("services/EucalyptusNC","") + '\'' +
           ", name='" + name + '\'' +
           ", lastSeen=" + lastSeen +
           ", certs=" + certs +
           ", logs=" + logs +
           '}';
  }

  def NodeInfo(final String serviceTag) {
    this.name = (new URI(serviceTag)).getHost();
    this.serviceTag = serviceTag;
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



  public void touch() {
    this.lastSeen = new Date();
  }

  public int compareTo(Object o) {
    return this.serviceTag.compareTo(((NodeInfo) o).serviceTag);
  }

  boolean equals(final o) {
    if ( this == o ) return true;
    if ( !(o instanceof NodeInfo) ) return false;
    NodeInfo nodeInfo = (NodeInfo) o;
    if ( !serviceTag.equals(nodeInfo.serviceTag) ) return false;
    return true;
  }

  int hashCode() {
    return serviceTag.hashCode();
  }

}
