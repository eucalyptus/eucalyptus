package com.eucalyptus.config;

import com.eucalyptus.bootstrap.Component;
import edu.ucsb.eucalyptus.msgs.DeregisterWalrusType;
import edu.ucsb.eucalyptus.msgs.DescribeWalrusesType;
import edu.ucsb.eucalyptus.msgs.RegisterComponentType;
import edu.ucsb.eucalyptus.msgs.RegisterWalrusType;

@Handles( { RegisterWalrusType.class, DeregisterWalrusType.class, DescribeWalrusesType.class } )
public class WalrusBuilder extends AbstractServiceBuilder<WalrusConfiguration> {
  
  @Override
  public Boolean isLocal( ) {
    return Component.walrus.isLocal( );
  }
  
  @Override
  public WalrusConfiguration newInstance( ) {
    return new WalrusConfiguration( );
  }
  
  @Override
  public WalrusConfiguration newInstance( String name, String host, Integer port, RegisterComponentType request ) {
    return new WalrusConfiguration( name, host, port );
  }

  @Override
  public Boolean checkRemove( String name ) throws ServiceRegistrationException {
    return true;
  }
  
}
