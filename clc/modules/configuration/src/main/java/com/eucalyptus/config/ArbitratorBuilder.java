package com.eucalyptus.config;

import java.util.List;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.DatabaseServiceBuilder;
import com.eucalyptus.component.DiscoverableServiceBuilder;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.util.EucalyptusCloudException;

import edu.ucsb.eucalyptus.msgs.DeregisterArbitratorType;
import edu.ucsb.eucalyptus.msgs.DescribeArbitratorsType;
import edu.ucsb.eucalyptus.msgs.ModifyArbitratorAttributeType;
import edu.ucsb.eucalyptus.msgs.RegisterArbitratorType;

@DiscoverableServiceBuilder( com.eucalyptus.bootstrap.Component.bootstrap )
@Handles( { RegisterArbitratorType.class, DeregisterArbitratorType.class, DescribeArbitratorsType.class, ArbitratorConfiguration.class, ModifyArbitratorAttributeType.class } )
public class ArbitratorBuilder extends DatabaseServiceBuilder<ArbitratorConfiguration> {
  private static Logger LOG = Logger.getLogger( ArbitratorBuilder.class );

  @Override
  public Component getComponent( ) {
    return Components.lookup( com.eucalyptus.bootstrap.Component.bootstrap );
  }
  
  @Override
  public ArbitratorConfiguration newInstance( ) {
    return new ArbitratorConfiguration( );
  }
  
  @Override
  public ArbitratorConfiguration newInstance( String partition, String name, String host, Integer port ) {
    return new ArbitratorConfiguration( partition, name, host, port );
  }
  
  @Override
  public Boolean checkAdd( String partition, String name, String host, Integer port ) throws ServiceRegistrationException {
    return super.checkAdd( partition, name, host, port );
  }

  @Override
  public List<ArbitratorConfiguration> list( ) throws ServiceRegistrationException {
    try {
      return Configuration.getArbitratorConfigurations( );
    } catch ( EucalyptusCloudException e ) {
      return super.list( );
    }
  }

  @Override
  public Boolean checkRemove( String partition, String name ) throws ServiceRegistrationException {
    return super.checkRemove( partition, name );
  }

  @Override
  public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException {
    super.fireStop( config );
  }
  
  
  @Override
  public void fireStart( ServiceConfiguration config ) throws ServiceRegistrationException {
    super.fireStart( config );
  }
  
}
