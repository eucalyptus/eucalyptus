package edu.ucsb.eucalyptus.msgs;

public class ComponentInfoType extends EucalyptusData {
  String name;
  String detail;
  public ComponentInfoType(){}
  public ComponentInfoType(String name, String detail){this.name = name; this.detail = detail;}
}

public class ConfigurationMessage extends EucalyptusMessage {
  String getComponentName(){
    String className = this.getClass().getSimpleName();
    return className.replaceAll("Describe","").replaceAll("Deregister","").replaceAll("Register","").substring(0,6);
  }
}
public class RegisterComponentType extends ConfigurationMessage {
  String name;
  String host;
  Integer port;
  def RegisterComponentType() {}
  def RegisterComponentType(final String name, final String host, final Integer port) {
    this.name = name;
    this.host = host;
    this.port = port;
  }
}
public class RegisterComponentResponseType extends ConfigurationMessage {}
public class DeregisterComponentType extends ConfigurationMessage {
  String name;
}
public class DeregisterComponentResponseType extends ConfigurationMessage {}

public class DescribeComponentsType extends ConfigurationMessage {}
public class DescribeComponentsResponseType extends ConfigurationMessage {
  ArrayList<ComponentInfoType> registered = new ArrayList<ComponentInfoType>();
}

public class RegisterClusterType extends RegisterComponentType {
  public RegisterClusterType( ) {}
  public RegisterClusterType( String name, String host, Integer port ) {
    super( name, host, port );
  }  
}

public class RegisterClusterResponseType extends RegisterComponentResponseType {}
public class DeregisterClusterType extends DeregisterComponentType {}
public class DeregisterClusterResponseType extends DeregisterComponentResponseType {}
public class DescribeClustersType extends DescribeComponentsType {}
public class DescribeClustersResponseType extends DescribeComponentsResponseType {}

public class RegisterStorageControllerType extends RegisterComponentType {}
public class RegisterStorageControllerResponseType extends RegisterComponentResponseType {}
public class DeregisterStorageControllerType extends DeregisterComponentType {}
public class DeregisterStorageControllerResponseType extends DeregisterComponentResponseType {}
public class DescribeStorageControllersType extends DescribeComponentsType {}
public class DescribeStorageControllersResponseType extends DescribeComponentsResponseType {}

public class RegisterWalrusType extends RegisterComponentType {}
public class RegisterWalrusResponseType extends RegisterComponentResponseType {}
public class DeregisterWalrusType extends DeregisterComponentType {}
public class DeregisterWalrusResponseType extends DeregisterComponentResponseType {}
public class DescribeWalrusesType extends DescribeComponentsType {}
public class DescribeWalrusesResponseType extends DescribeComponentsResponseType {}


















