package com.eucalyptus.cloud;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.NoSuchElementException;
import net.sf.hajdbc.InactiveDatabaseMBean;
import net.sf.hajdbc.sql.DriverDatabaseClusterMBean;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.bootstrap.Handles;
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
import com.google.common.collect.ImmutableMap;

@ComponentPart( Eucalyptus.class )
@Handles( { RegisterEucalyptusType.class, DeregisterEucalyptusType.class, DescribeEucalyptusType.class, EucalyptusConfiguration.class,
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
    if ( !config.isHostLocal( ) ) {
      startDbPool( config );
    }
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
  
//  static class ConnectionPool {
//    private static final String HA_JDBC_CLUSTER = "cluster";
//    enum SyncStrategy {
//      full,passive;
//      public static SyncStrategy get( ) {
//        return ( Hosts.isCoordinator( ) ? full : passive );
//      }
//    }
//    private final String persistenceContext; 
//    private void getMBean( ) {
//      DriverDatabaseClusterMBean cluster = Mbeans.lookup( jdbcJmxDomain, ImmutableMap.builder( ).put( HA_JDBC_CLUSTER, this.persistenceContext ).build( ),
//                                                          DriverDatabaseClusterMBean.class );
//    }
//  }
  
  @Override
  public void fireCheck( ServiceConfiguration config ) throws ServiceRegistrationException {
    if( !Bootstrap.isFinished( ) ) {
      return;
    }
    for ( String ctx : PersistenceContexts.list( ) ) {
      try {
        DriverDatabaseClusterMBean cluster = Mbeans.lookup( jdbcJmxDomain, ImmutableMap.builder( ).put( "cluster", ctx ).build( ),
                                                            DriverDatabaseClusterMBean.class );
        for( String dbId : cluster.getActiveDatabases( ) ) {
          cluster.getDatabase( dbId );
          if( !cluster.isAlive( dbId ) ) {
            cluster.deactivate( dbId );
          }
        }
        for( String dbId : cluster.getInactiveDatabases( ) ) {
          if( cluster.isAlive( dbId ) ) {
            cluster.activate( dbId );
          }
        }
      } catch ( Exception ex ) {
        LOG.error( ex , ex );
      }
    }
  }
  
  private void startDbPool( ServiceConfiguration config ) {
    String dbPass = SystemIds.databasePassword( );
    final String hostName = config.getHostName( );
    String realJdbcDriver = Databases.getDriverName( );
    
    for ( String ctx : PersistenceContexts.list( ) ) {
      final String contextName = ctx.startsWith( "eucalyptus_" )
        ? ctx
        : "eucalyptus_" + ctx;
      
      String dbUrl = "jdbc:" + ServiceUris.remote( Database.class, config.getInetAddress( ), contextName );
      
      try {
        DriverDatabaseClusterMBean cluster = Mbeans.lookup( jdbcJmxDomain, ImmutableMap.builder( ).put( "cluster", contextName ).build( ),
                                                            DriverDatabaseClusterMBean.class );
        if ( !cluster.getActiveDatabases( ).contains( hostName ) && !cluster.getInactiveDatabases( ).contains( hostName ) ) {
          cluster.add( hostName, realJdbcDriver, dbUrl );
        } else if ( cluster.getActiveDatabases( ).contains( hostName ) ) {
          continue;
        }
        InactiveDatabaseMBean database = Mbeans.lookup( jdbcJmxDomain,
                                                        ImmutableMap.builder( ).put( "cluster", contextName ).put( "database", hostName ).build( ),
                                                        InactiveDatabaseMBean.class );
        database.setUser( "eucalyptus" );
        database.setPassword( dbPass );
        if ( BootstrapArgs.isCloudController( ) ) {
          cluster.activate( hostName, "full" );
        } else {
          cluster.activate( hostName, "passive" );
        }
      } catch ( NoSuchElementException ex1 ) {
        LOG.error( ex1, ex1 );
      } catch ( Exception ex1 ) {
        LOG.error( ex1, ex1 );
      }
    }
  }
  private void stopDbPool( ServiceConfiguration config ) {
    final String hostName = config.getHostName( );
    
    for ( String ctx : PersistenceContexts.list( ) ) {
      final String contextName = ctx.startsWith( "eucalyptus_" )
        ? ctx
        : "eucalyptus_" + ctx;
      
      try {
        DriverDatabaseClusterMBean cluster = Mbeans.lookup( jdbcJmxDomain, ImmutableMap.builder( ).put( "cluster", contextName ).build( ),
                                                            DriverDatabaseClusterMBean.class );

        try {
          if( cluster.getActiveDatabases( ).contains( hostName ) ) {
            cluster.deactivate( hostName );
          }
          if( cluster.getInactiveDatabases( ).contains( hostName ) ) {
            cluster.remove( hostName );
          }
        } catch ( Exception ex ) {
          LOG.error( ex , ex );
        }
      } catch ( NoSuchElementException ex1 ) {
        LOG.error( ex1, ex1 );
      } catch ( Exception ex1 ) {
        LOG.error( ex1, ex1 );
      }
    }
  }
  
}
