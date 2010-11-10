package com.eucalyptus.config;

import java.util.List;
import org.apache.log4j.Logger;

import com.eucalyptus.component.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.DatabaseServiceBuilder;
import com.eucalyptus.component.DiscoverableServiceBuilder;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.LogUtil;

import edu.ucsb.eucalyptus.msgs.DeregisterArbitratorType;
import edu.ucsb.eucalyptus.msgs.DeregisterStorageControllerType;
import edu.ucsb.eucalyptus.msgs.DescribeArbitratorType;
import edu.ucsb.eucalyptus.msgs.DescribeStorageControllersType;
import edu.ucsb.eucalyptus.msgs.RegisterArbitratorType;
import edu.ucsb.eucalyptus.msgs.RegisterStorageControllerType;

@DiscoverableServiceBuilder( com.eucalyptus.bootstrap.Component.bootstrap )
@Handles( { RegisterArbitratorType.class, DeregisterArbitratorType.class, DescribeArbitratorType.class, ArbitratorConfiguration.class } )
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
  public Boolean checkAdd( String name, String host, Integer port ) throws ServiceRegistrationException {
	//TODO: add any checks here
    return super.checkAdd( name, host, port );
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
  public Boolean checkRemove( String name ) throws ServiceRegistrationException {
    return super.checkRemove( name );
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
