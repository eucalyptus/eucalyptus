package edu.ucsb.eucalyptus.msgs;

public class ConfigurationMessage extends EucalyptusMessage{}

public class RegisterClusterType extends ConfigurationMessage {
  String name;
  String host;
  Integer port;

  def RegisterClusterType() {
  }

  def RegisterClusterType(final String name, final String host, final Integer port) {
    this.name = name;
    this.host = host;
    this.port = port;
  }
}
public class RegisterClusterResponseType extends ConfigurationMessage {}
public class DeregisterClusterType extends ConfigurationMessage {
  String name;
}
public class DeregisterClusterResponseType extends ConfigurationMessage {}
public class DescribeClustersType extends ConfigurationMessage {}
public class DescribeClustersResponseType extends ConfigurationMessage {
  ArrayList<ClusterInfoType> clusters = new ArrayList<ClusterInfoType>();
}




















