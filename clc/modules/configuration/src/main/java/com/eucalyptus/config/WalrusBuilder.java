package com.eucalyptus.config;

import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentId.ComponentPart;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.id.Walrus;

@ComponentPart(Walrus.class)
@Handles( { RegisterWalrusType.class, DeregisterWalrusType.class, DescribeWalrusesType.class, ModifyWalrusAttributeType.class } )
public class WalrusBuilder extends AbstractServiceBuilder<WalrusConfiguration> {
  
  @Override
  public WalrusConfiguration newInstance( ) {
    return new WalrusConfiguration( );
  }
  
  @Override
  public WalrusConfiguration newInstance( String partition, String name, String host, Integer port ) {
    return new WalrusConfiguration( name, host, port );
  }

  @Override
  public ComponentId getComponentId( ) {
    return ComponentIds.lookup( Walrus.class );
  }

  @Override
  public void fireStart( ServiceConfiguration config ) throws ServiceRegistrationException {}

  @Override
  public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException {}

  @Override
  public void fireEnable( ServiceConfiguration config ) throws ServiceRegistrationException {}

  @Override
  public void fireDisable( ServiceConfiguration config ) throws ServiceRegistrationException {}

  @Override
  public void fireCheck( ServiceConfiguration config ) throws ServiceRegistrationException {}

  
}
