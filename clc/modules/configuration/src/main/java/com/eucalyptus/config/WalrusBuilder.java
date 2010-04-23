package com.eucalyptus.config;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.DatabaseServiceBuilder;
import com.eucalyptus.component.DiscoverableServiceBuilder;
import edu.ucsb.eucalyptus.msgs.DeregisterWalrusType;
import edu.ucsb.eucalyptus.msgs.DescribeWalrusesType;
import edu.ucsb.eucalyptus.msgs.RegisterWalrusType;

@DiscoverableServiceBuilder(com.eucalyptus.bootstrap.Component.walrus)
@Handles( { RegisterWalrusType.class, DeregisterWalrusType.class, DescribeWalrusesType.class } )
public class WalrusBuilder extends DatabaseServiceBuilder<WalrusConfiguration> {
  
  @Override
  public WalrusConfiguration newInstance( ) {
    return new WalrusConfiguration( );
  }
  
  @Override
  public WalrusConfiguration newInstance( String name, String host, Integer port ) {
    return new WalrusConfiguration( name, host, port );
  }

  @Override
  public com.eucalyptus.component.Component getComponent( ) {
    return Components.lookup( Component.walrus );
  }

  
}
