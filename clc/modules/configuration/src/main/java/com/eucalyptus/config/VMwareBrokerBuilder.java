package com.eucalyptus.config;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.DatabaseServiceBuilder;
import com.eucalyptus.component.DiscoverableServiceBuilder;
import edu.ucsb.eucalyptus.msgs.DeregisterVMwareBrokerType;
import edu.ucsb.eucalyptus.msgs.DescribeVMwareBrokersType;
import edu.ucsb.eucalyptus.msgs.RegisterVMwareBrokerType;

@DiscoverableServiceBuilder(com.eucalyptus.bootstrap.Component.vmwarebroker)
@Handles( { RegisterVMwareBrokerType.class, DeregisterVMwareBrokerType.class, DescribeVMwareBrokersType.class } )
public class VMwareBrokerBuilder extends DatabaseServiceBuilder<VMwareBrokerConfiguration> {
  
  @Override
  public VMwareBrokerConfiguration newInstance( ) {
    return new VMwareBrokerConfiguration( );
  }
  
  @Override
  public VMwareBrokerConfiguration newInstance( String name, String host, Integer port ) {
    return new VMwareBrokerConfiguration( name, host, port );
  }

  @Override
  public com.eucalyptus.component.Component getComponent( ) {
    return Components.lookup( Component.vmwarebroker );
  }

  
}
