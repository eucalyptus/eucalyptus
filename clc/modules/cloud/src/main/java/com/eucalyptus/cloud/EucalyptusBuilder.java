package com.eucalyptus.cloud;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.bootstrap.Host;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentId.ComponentPart;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.Internets;

@ComponentPart( Eucalyptus.class )
@Handles( { RegisterEucalyptusType.class,
           DeregisterEucalyptusType.class,
           DescribeEucalyptusType.class,
           ModifyEucalyptusAttributeType.class } )
public class EucalyptusBuilder extends AbstractServiceBuilder<EucalyptusConfiguration> {
  static Logger               LOG           = Logger.getLogger( EucalyptusBuilder.class );
  private static final String jdbcJmxDomain = "net.sf.hajdbc";
  
  @Override
  public EucalyptusConfiguration newInstance( ) {
    return new EucalyptusConfiguration( );
  }
  
  @Override
  public EucalyptusConfiguration newInstance( String partition, String name, String host, Integer port ) {
    try {
      InetAddress.getByName( host );
      return new EucalyptusConfiguration( host, host );
    } catch ( UnknownHostException e ) {
      return new EucalyptusConfiguration( Internets.localHostAddress( ), Internets.localHostAddress( ) );
    }
  }
  
  @Override
  public ComponentId getComponentId( ) {
    return Eucalyptus.INSTANCE;
  }
  
  @Override
  public void fireStart( ServiceConfiguration config ) throws ServiceRegistrationException {}
  
  @Override
  public void fireEnable( ServiceConfiguration config ) throws ServiceRegistrationException {
    if ( !config.isVmLocal( ) ) {
      for ( Host h : Hosts.list( ) ) {
        if ( h.getHostAddresses( ).contains( config.getInetAddress( ) ) ) {
          EventRecord.here( EucalyptusBuilder.class, EventType.COMPONENT_SERVICE_ENABLED, config.toString( ) ).info( );
          return;
        }
      }
      throw new ServiceRegistrationException( "There is no host in the system (yet) for the given cloud controller configuration: "
                                              + config.getFullName( )
                                              + ".\nHosts are: "
                                              + Hosts.list( ) );
    } else if ( config.isVmLocal( ) && !Hosts.isCoordinator( ) ) {
      throw new ServiceRegistrationException( "This cloud controller "
                                              + config.getFullName( )
                                              + " is not currently the coordinator "
                                              + Hosts.list( ) );
    }
  }
  
  @Override
  public void fireDisable( ServiceConfiguration config ) throws ServiceRegistrationException {}
  
  @Override
  public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException {}
  
  @Override
  public void fireCheck( ServiceConfiguration config ) throws ServiceRegistrationException {}
  
}
