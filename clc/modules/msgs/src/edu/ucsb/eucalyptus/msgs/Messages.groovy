package edu.ucsb.eucalyptus.msgs

import org.jibx.runtime.BindingDirectory
import org.jibx.runtime.IBindingFactory
import org.jibx.runtime.IMarshallingContext

public class INTERNAL extends EucalyptusMessage {

  def INTERNAL() {
    super();
    this.userId = "eucalyptus";
    this.effectiveUserId = "eucalyptus";
  }
}

public class AddClusterType extends ClusterMessage {
  String name;
  String host;
  int port;
}
public class AddClusterResponseType extends ClusterMessage {}

public class ClusterStateType extends EucalyptusMessage{
  String name;
  String host;
  int port;

  def ClusterStateType() {
  }

  def ClusterStateType(final name, final host, final port) {
    this.name = name;
    this.host = host;
    this.port = port;
  }
}


public class EucalyptusMessage implements Cloneable, Serializable {

  String correlationId;
  String userId;
  String effectiveUserId;
  boolean _return;
  String statusMessage;

  public EucalyptusMessage()
  {
    this.correlationId = UUID.randomUUID();
  }

  public EucalyptusMessage( EucalyptusMessage msg )
  {
    this();
    this.userId = msg.userId;
    this.effectiveUserId = msg.effectiveUserId;
    this.correlationId = msg.correlationId;
  }

  def EucalyptusMessage(final String userId) {
    this();
    this.userId = userId;
    this.effectiveUserId = userId;
  }




  public MetaClass getMetaClass()
  {
    return metaClass;
  }

  public String getEffectiveUserId()
  {
    if ( isAdministrator() ) return "eucalyptus";
    return effectiveUserId;
  }

  public boolean isAdministrator()
  {
    return "eucalyptus".equals(this.effectiveUserId);
  }

  public String toString()
  {
    ByteArrayOutputStream temp = new ByteArrayOutputStream();
    Class targetClass = this.getClass();
    while ( !targetClass.getSimpleName().endsWith("Type") ) targetClass = targetClass.getSuperclass();
    IBindingFactory bindingFactory = BindingDirectory.getFactory("msgs_eucalyptus_ucsb_edu", targetClass);
    IMarshallingContext mctx = bindingFactory.createMarshallingContext();
    mctx.setIndent(2);
    mctx.marshalDocument(this, "UTF-8", null, temp);
    return temp.toString();
  }

  public String toString(String namespace)
  {
    ByteArrayOutputStream temp = new ByteArrayOutputStream();
    Class targetClass = this.getClass();
    while ( !targetClass.getSimpleName().endsWith("Type") ) targetClass = targetClass.getSuperclass();
    IBindingFactory bindingFactory = BindingDirectory.getFactory(namespace, targetClass);
    IMarshallingContext mctx = bindingFactory.createMarshallingContext();
    mctx.setIndent(2);
    mctx.marshalDocument(this, "UTF-8", null, temp);
    return temp;
  }

  public Object clone()
  {
    return super.clone();
  }

  public EucalyptusMessage getReply()
  {
    Class msgClass = this.getClass();
    if ( !this.getClass().getSimpleName().endsWith("Type") )
    msgClass = msgClass.getSuperclass();
    Class responseClass = Class.forName(msgClass.getName().replaceAll("Type", "") + "ResponseType");
    EucalyptusMessage reply = (EucalyptusMessage) responseClass.newInstance();
    reply.setCorrelationId(this.getCorrelationId());
    reply.setUserId(this.getUserId());
    reply.setEffectiveUserId(this.getEffectiveUserId());
    return reply;
  }

}
public class EucalyptusErrorMessageType extends EucalyptusMessage {

  String source;
  String message;
  String requestType = "not available";

  public EucalyptusErrorMessageType() {}

  public EucalyptusErrorMessageType(String source, String message)
  {
    this.source = source;
    this.message = message;
  }

  public EucalyptusErrorMessageType(String source, EucalyptusMessage msg, String message)
  {
    this(source, message);
    this.correlationId = msg.getCorrelationId();
    this.userId = msg.getUserId();
    this.requestType = msg != null ? msg.getClass().getSimpleName() : this.requestType;
  }

  public String toString()
  {
    return String.format("SERVICE: %s PROBLEM: %s MSG-TYPE: %s", this.source, this.message, this.requestType);
  }

}

public class EucalyptusData implements Cloneable, Serializable {
  public MetaClass getMetaClass() {
    return metaClass;
  }

  public String toString() {
    return this.getProperties().toMapString();
  }

  public Object clone(){
    return super.clone();
  }
}
/** *******************************************************************************/
public class DescribeResourcesType extends EucalyptusMessage {

  ArrayList<VmTypeInfo> instanceTypes = new ArrayList<VmTypeInfo>();
}
public class DescribeResourcesResponseType extends EucalyptusMessage {

  ArrayList<ResourceType> resources = new ArrayList<ResourceType>();
  ArrayList<String> serviceTags = new ArrayList<String>();
}

public class VmTypeInfo extends EucalyptusData {

  String name;
  Integer memory;
  Integer disk;
  Integer cores;

  def VmTypeInfo(){}

  def VmTypeInfo(final name, final memory, final disk, final cores)
  {
    this.name = name;
    this.memory = memory;
    this.disk = disk;
    this.cores = cores;
  }

  @Override
  public String toString() {
    return "VmTypeInfo{" +
            "name='" + name + '\'' +
            ", memory=" + memory +
            ", disk=" + disk +
            ", cores=" + cores +
            '}';
  }

}
public class ResourceType extends EucalyptusData {

  VmTypeInfo instanceType;
  int maxInstances;
  int availableInstances;
}
public class NetworkConfigType extends EucalyptusData {
  String macAddress;
  String ignoredMacAddress;
  String ipAddress;
  String ignoredPublicIp;
  int vlan;

  def NetworkConfigType()
  {
  }

  @Override
  public String toString() {
    return "NetworkConfigType{" +
            "macAddress='" + macAddress + '\'' +
            ", ipAddress='" + ipAddress + '\'' +
            ", publicIp='" + ignoredPublicIp + '\'' +
            ", vlan=" + vlan +
            '}';
  }


}

public class NetworkParameters extends EucalyptusData {

  String privateMacAddress;
  String publicMacAddress;
  int macLimit;
  int vlan;
}

public class PacketFilterRule {
  public static String ACCEPT = "firewall-open";
  public static String DENY = "firewall-close";

  public static PacketFilterRule revoke( PacketFilterRule  existingRule ) {
    PacketFilterRule pf = new PacketFilterRule();
    pf.destName = existingRule.getDestName();
    pf.policy = DENY;
    pf.portMin = existingRule.getPortMin();
    pf.portMax = existingRule.getPortMax();
    pf.protocol = existingRule.getProtocol();
    pf.setPeers( existingRule.getPeers() );
    pf.setSourceCidrs( existingRule.getSourceCidrs() );
    pf.setSourceNetworkNames( existingRule.getSourceNetworkNames() );
    pf.setSourceUserNames( existingRule.getSourceUserNames() );
    return pf;
  }

  String destName;
  String policy = "firewall-open";
  String protocol;
  int portMin;
  int portMax;
  ArrayList<String> sourceCidrs = new ArrayList<String>();
  ArrayList<VmNetworkPeer> peers = new ArrayList<VmNetworkPeer>();
  ArrayList<String> sourceNetworkNames = new ArrayList<String>();
  ArrayList<String> sourceUserNames = new ArrayList<String>();

  def PacketFilterRule(final destName, final protocol, final portMin, final portMax)
  {
    this.destName = destName;
    this.protocol = protocol;
    this.portMin = portMin;
    this.portMax = portMax;
  }

  def PacketFilterRule(){}

  @Override
  public String toString() {
    return "PacketFilterRule{" +
            "destName='" + destName + '\'' +
            ", policy='" + policy + '\'' +
            ", protocol='" + protocol + '\'' +
            ", portMin=" + portMin +
            ", portMax=" + portMax +
            ", sourceCidrs=" + sourceCidrs +
            ", peers=" + peers +
            ", sourceNetworkNames=" + sourceNetworkNames +
            ", sourceUserNames=" + sourceUserNames +
            '}';
  }


  public void addPeer( String queryKey, String groupName )
  {
    VmNetworkPeer peer = new VmNetworkPeer( queryKey, groupName );
    this.peers.add(peer);
    this.sourceNetworkNames.add( peer.getSourceNetworkName() );
    this.sourceUserNames.add(peer.userName);
  }

}

public class VmNetworkPeer {

  String userName;
  String sourceNetworkName;

  def VmNetworkPeer()
  {
  }

  def VmNetworkPeer(final userName, final sourceNetworkName)
  {
    this.userName = userName;
    this.sourceNetworkName = sourceNetworkName;
  }

}



public class EventRecord extends EucalyptusMessage {

  String host = "cloud";
  String service;
  long timestamp = System.currentTimeMillis();
  String eventUserId;
  String eventCorrelationId;
  String eventId;
  String other;

  def EventRecord(final service, final eventUserId, final eventCorrelationId, final eventId, final other)
  {
    this.service = service;
    this.eventUserId = eventUserId;
    this.eventCorrelationId = eventCorrelationId;
    this.eventId = eventId;
    this.other = other;
  }

  public EventRecord() {}

  public String toString()
  {
    return String.format("%s/%s:%s:%s:%s:%7.4f:%s", this.host, this.service, this.eventUserId, this.eventCorrelationId, this.eventId, this.timestamp / 1000.0f, this.other != null ? this.other : "");
  }

  public static EventRecord create(final service, final eventUserId, final eventCorrelationId, final eventId, final other)
  {
    return new EventRecord(service, eventUserId, eventCorrelationId, eventId, other);
  }

}

public class GetLogsType extends EucalyptusMessage implements Comparable {
  String serviceTag;
  def GetLogsType(){}
  def GetLogsType(final serviceTag)
  {
    this.serviceTag = serviceTag;
  }
  public int compareTo(Object o)
  {
    return this.serviceTag.compareTo(((GetLogsType)o).serviceTag);
  }
}
public class GetLogsResponseType extends EucalyptusMessage {
  NodeLogInfo logs = new NodeLogInfo();
}
public class GetKeysType extends EucalyptusMessage implements Comparable {
  String serviceTag;
  def GetKeysType(){}
  def GetKeysType(final serviceTag)
  {
    this.serviceTag = serviceTag;
  }

  public int compareTo(Object o)
  {
    return this.serviceTag.compareTo(((GetKeysType)o).serviceTag);
  }

}
public class GetKeysResponseType extends EucalyptusMessage {
  NodeCertInfo certs = new NodeCertInfo();
}

public class NodeCertInfo extends EucalyptusData implements Comparable {
  String serviceTag;
  String ccCert = "";
  String ncCert = "";

  public int compareTo(Object o)
  {
    return this.serviceTag.compareTo(((NodeCertInfo)o).serviceTag);
  }

  @Override
  public String toString() {
    return "NodeCertInfo{" +
            "serviceTag='" + serviceTag.replaceAll("services/EucalyptusNC","") + '\'' +
            ", ccCert='" + ccCert + '\'' +
            ", ncCert='" + ncCert + '\'' +
            '}';
  }

}

public class NodeLogInfo extends EucalyptusData implements Comparable {
  String serviceTag;
  String ccLog = "";
  String ncLog = "";
  String httpdLog = "";
  String axis2Log = "";

  public int compareTo(Object o)
  {
    return this.serviceTag.compareTo(((NodeLogInfo)o).serviceTag);
  }

}

public class HeartbeatMessage implements Cloneable, Serializable {
  String heartbeatId;

  def HeartbeatMessage(final String heartbeatId) {
    this.heartbeatId = heartbeatId;
  }

  def HeartbeatMessage() {}


}

