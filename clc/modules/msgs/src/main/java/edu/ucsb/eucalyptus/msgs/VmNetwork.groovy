package edu.ucsb.eucalyptus.msgs

public class StartNetworkType extends EucalyptusMessage {

  int vlan;
  String netName;
  ArrayList<String> clusterControllers;

  def StartNetworkType(final msg, final vlan, final netName) {
    super(msg);
    this.vlan = vlan;
    this.netName = netName;
  }



  def StartNetworkType(final EucalyptusMessage msg, final Integer vlan, final String netName)
  {
    super(msg);
    this.vlan = vlan;
    this.netName = netName;
  }

  def StartNetworkType(){}

  def StartNetworkType(final Integer vlan, final String netName)
  {
    this.vlan = vlan;
    this.netName = netName;
  }
}

public class StartNetworkResponseType extends EucalyptusMessage {
}
public class StopNetworkType extends EucalyptusMessage {
  Integer vlan;
  String netName;

  def StopNetworkType(){}

  def StopNetworkType(final String userId, final String netName, final Integer vlan)
  {
    super( userId );
    this.vlan = vlan;
    this.netName = netName;
  }

  def StopNetworkType(final StartNetworkType msg)
  {
    super(msg);
    this.vlan = msg.vlan;
    this.netName = msg.netName;
  }

}

public class StopNetworkResponseType extends EucalyptusMessage {
  boolean _return;
}

//TODO: new message type
public class DescribeNetworksType extends EucalyptusMessage {
  ArrayList<String> clusterControllers;
}
//TODO: new message type
public class DescribeNetworksResponeType extends EucalyptusMessage {
  Integer mode;
  Integer addrsPerNetwork;
  ArrayList<NetworkInfoType> activeNetworks = new ArrayList<NetworkInfoType>();
}
//TODO: new message type
public class NetworkInfoType extends EucalyptusData {
  Integer vlan;
  String networkName;
  String userName;
  ArrayList<String> allocatedAddresses = new ArrayList<String>();
}


public class AssignAddressType extends EucalyptusMessage {

  String source;
  String destination;
  def AssignAddressType(final source, final destination)
  {
    this.source = source;
    this.destination = destination;
  }

  def AssignAddressType(final msg, final source, final destination)
  {
    super(msg);
    this.source = source;
    this.destination = destination;
  }

  def AssignAddressType()
  {
  }



}
public class AssignAddressResponseType extends EucalyptusMessage {
}
public class UnassignAddressType extends EucalyptusMessage {

  String source;
  String destination;

  def UnassignAddressType(final msg, final source, final destination)
  {
    super(msg);
    this.source = source;
    this.destination = destination;
  }
  def UnassignAddressType(final source, final destination)
  {

    this.source = source;
    this.destination = destination;
  }


  def UnassignAddressType()
  {
  }

}
public class UnassignAddressResponseType extends EucalyptusMessage {
}
public class DescribePublicAddressesType extends EucalyptusMessage {
}
public class DescribePublicAddressesResponseType extends EucalyptusMessage {
  ArrayList<String> addresses;
  ArrayList<String> mapping;
}

public class ConfigureNetworkType extends EucalyptusMessage {

  ArrayList<PacketFilterRule> rules = new ArrayList<PacketFilterRule>();

  def ConfigureNetworkType(final EucalyptusMessage msg, final ArrayList<PacketFilterRule> rules)
  {
    super(msg);
    this.rules = rules;
  }

  def ConfigureNetworkType(final ArrayList<PacketFilterRule> rules)
  {
    this.rules = rules;
  }

  def ConfigureNetworkType(){}
}
public class ConfigureNetworkResponseType extends EucalyptusMessage {
}
