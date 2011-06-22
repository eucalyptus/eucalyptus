package com.eucalyptus.cloud;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.DiscoverableServiceBuilder;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceChecks.CheckException;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.Internets;

@DiscoverableServiceBuilder( Eucalyptus.class )
@Handles( { RegisterEucalyptusType.class, DeregisterEucalyptusType.class, DescribeEucalyptusType.class, EucalyptusConfiguration.class, ModifyEucalyptusAttributeType.class } )
public class EucalyptusBuilder extends AbstractServiceBuilder<EucalyptusConfiguration> {
  static Logger LOG = Logger.getLogger( EucalyptusBuilder.class );
  
  @Override
  public Boolean checkAdd( String partition, String name, String host, Integer port ) throws ServiceRegistrationException {
    return super.checkAdd( partition, name, host, port );
  }
  
  @Override
  public EucalyptusConfiguration newInstance( ) {
    return new EucalyptusConfiguration( );
  }
  
  @Override
  public EucalyptusConfiguration newInstance( String partition, String name, String host, Integer port ) {
    InetAddress addr;
    try {
      addr = InetAddress.getByName( host );
      return new EucalyptusConfiguration( host, host );
    } catch ( UnknownHostException e ) {
      return this.newInstance( );
    }
  }
  
  @Override
  public Component getComponent( ) {
    return Components.lookup( Eucalyptus.class );
  }
  
  @Override
  public EucalyptusConfiguration add( String partitionName, String name, String host, Integer port ) throws ServiceRegistrationException {
    return super.add( partitionName, name, host, port );
  }
  
  @Override
  public EucalyptusConfiguration remove( ServiceConfiguration config ) throws ServiceRegistrationException {
    return super.remove( config );
  }
  
  @Override
  public void fireStart( ServiceConfiguration config ) throws ServiceRegistrationException {
    EventRecord.here( EucalyptusBuilder.class, EventType.COMPONENT_SERVICE_START, config.toString( ) ).info( );
  }
  
  @Override
  public void fireEnable( ServiceConfiguration config ) throws ServiceRegistrationException {
    EventRecord.here( EucalyptusBuilder.class, EventType.COMPONENT_SERVICE_ENABLED, config.toString( ) ).info( );
  }
  
  @Override
  public void fireDisable( ServiceConfiguration config ) throws ServiceRegistrationException {
    EventRecord.here( EucalyptusBuilder.class, EventType.COMPONENT_SERVICE_DISABLED, config.toString( ) ).info( );
  }
  
  @Override
  public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException {
    EventRecord.here( EucalyptusBuilder.class, EventType.COMPONENT_SERVICE_STOPPED, config.toString( ) ).info( );
  }
  
  @Override
  public void fireCheck( ServiceConfiguration config ) throws ServiceRegistrationException, CheckException {
    EventRecord.here( EucalyptusBuilder.class, EventType.COMPONENT_SERVICE_CHECK, config.toString( ) ).info( );//TODO:GRZE: host checks here.
  }
  
}
