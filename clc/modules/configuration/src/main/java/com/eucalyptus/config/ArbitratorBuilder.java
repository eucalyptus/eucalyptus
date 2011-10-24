package com.eucalyptus.config;

import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentId.ComponentPart;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.empyrean.Empyrean.Arbitrator;

@ComponentPart( Arbitrator.class )
@Handles( { RegisterArbitratorType.class,
           DeregisterArbitratorType.class,
           DescribeArbitratorsType.class,
           ModifyArbitratorAttributeType.class } )
public class ArbitratorBuilder extends AbstractServiceBuilder<ArbitratorConfiguration> {
  private static Logger LOG = Logger.getLogger( ArbitratorBuilder.class );
  
  @Override
  public ComponentId getComponentId( ) {
    return ComponentIds.lookup( Arbitrator.class );
  }
  
  @Override
  public ArbitratorConfiguration newInstance( ) {
    return new ArbitratorConfiguration( );
  }
  
  @Override
  public ArbitratorConfiguration newInstance( final String partition, final String name, final String host, final Integer port ) {
    return new ArbitratorConfiguration( partition, name, host, port );
  }
  
  @Override
  public void fireStop( final ServiceConfiguration config ) throws ServiceRegistrationException {}
  
  @Override
  public void fireStart( final ServiceConfiguration config ) throws ServiceRegistrationException {}
  
  @Override
  public void fireEnable( final ServiceConfiguration config ) throws ServiceRegistrationException {}
  
  @Override
  public void fireDisable( final ServiceConfiguration config ) throws ServiceRegistrationException {}
  
  @Override
  public void fireCheck( final ServiceConfiguration config ) throws ServiceRegistrationException {}
  
}
