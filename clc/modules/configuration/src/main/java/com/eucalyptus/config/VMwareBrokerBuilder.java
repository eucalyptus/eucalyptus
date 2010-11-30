package com.eucalyptus.config;

import java.util.List;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.DatabaseServiceBuilder;
import com.eucalyptus.component.DiscoverableServiceBuilder;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.util.EucalyptusCloudException;

import edu.ucsb.eucalyptus.msgs.DeregisterVMwareBrokerType;
import edu.ucsb.eucalyptus.msgs.DescribeVMwareBrokersType;
import edu.ucsb.eucalyptus.msgs.ModifyVMwareBrokerAttributeType;
import edu.ucsb.eucalyptus.msgs.RegisterVMwareBrokerType;

@DiscoverableServiceBuilder(com.eucalyptus.bootstrap.Component.vmwarebroker)
@Handles( { RegisterVMwareBrokerType.class, DeregisterVMwareBrokerType.class, DescribeVMwareBrokersType.class, ModifyVMwareBrokerAttributeType.class } )
public class VMwareBrokerBuilder extends DatabaseServiceBuilder<VMwareBrokerConfiguration> {
  
  @Override
  public VMwareBrokerConfiguration newInstance( ) {
    return new VMwareBrokerConfiguration( );
  }
  
  @Override
  public VMwareBrokerConfiguration newInstance( String partition, String name, String host, Integer port ) {
    return new VMwareBrokerConfiguration( partition, name, host, port );
  }

  @Override
  public com.eucalyptus.component.Component getComponent( ) {
    return Components.lookup( Component.vmwarebroker );
  }

  @Override
  public Boolean checkAdd( String partition, String name, String host, Integer port ) throws ServiceRegistrationException {
    try {
      Configuration.getClusterConfiguration( name );
    } catch ( Exception e1 ) {
      throw new ServiceRegistrationException( "Vmwarebroker may only be registered with a corresponding Cluster of the same name."
                                              + "  No cluster found with the name: " + name );
    }
    return super.checkAdd( partition, name, host, port );
  }

  @Override
  public Boolean checkRemove( String partition, String name ) throws ServiceRegistrationException {
    return super.checkRemove( partition, name );
  }
  
  @Override
  public List<VMwareBrokerConfiguration> list( ) throws ServiceRegistrationException {
    try {
      return Configuration.getVMwareBrokerConfigurations( );
    } catch ( EucalyptusCloudException e ) {
      return super.list( );
    }
  }
}
