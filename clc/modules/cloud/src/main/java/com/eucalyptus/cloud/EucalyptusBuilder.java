package com.eucalyptus.cloud;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import net.sf.hajdbc.InactiveDatabaseMBean;
import net.sf.hajdbc.sql.DriverDatabaseClusterMBean;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.bootstrap.Host;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.bootstrap.SystemIds;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentId.ComponentPart;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.component.id.Eucalyptus.Database;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.Mbeans;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

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
  public void fireCheck( ServiceConfiguration config ) throws ServiceRegistrationException {}
  
}
