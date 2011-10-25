package com.eucalyptus.cloud;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Driver;
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
import com.google.common.collect.Lists;

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
    startDbPool( config );
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
    if ( !config.isHostLocal( ) ) {
      stopDbPool( config );
    }
  }
  
  @Override
  public void fireCheck( ServiceConfiguration config ) throws ServiceRegistrationException {
    if ( !Bootstrap.isFinished( ) ) {
      return;
    }
    this.updateDatabaseConnections( );
  }
  
  private synchronized void updateDatabaseConnections( ) {
    for ( String ctx : PersistenceContexts.list( ) ) {
      try {
        DriverDatabaseClusterMBean cluster = findDbClusterMBean( ctx );
        for ( String dbId : cluster.getActiveDatabases( ) ) {
          try {
            Iterables.find( Hosts.list( ), filterDbHost( dbId ) );
            if ( !cluster.isAlive( dbId ) ) {
              cluster.deactivate( dbId );
            }
          } catch ( NoSuchElementException ex ) {
//            cluster.deactivate( dbId );
          }
        }
        for ( final String dbId : cluster.getInactiveDatabases( ) ) {
          try {
            Iterables.find( Hosts.list( ), filterDbHost( dbId ) );
            if ( cluster.isAlive( dbId ) ) {
              cluster.activate( dbId );
            }
          } catch ( NoSuchElementException ex ) {
//            cluster.remove( dbId );
          }
        }
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
      }
    }
  }

  private static DriverDatabaseClusterMBean findDbClusterMBean( String ctx ) throws NoSuchElementException {
    DriverDatabaseClusterMBean cluster = Mbeans.lookup( jdbcJmxDomain,
                                                        ImmutableMap.builder( ).put( "cluster", ctx ).build( ),
                                                        DriverDatabaseClusterMBean.class );
    return cluster;
  }
  
  private static Predicate<Host> filterDbHost( final String dbId ) {
    Predicate<Host> filter = new Predicate<Host>( ) {
      
      @Override
      public boolean apply( Host input ) {
        return input.getHostAddresses( ).contains( Internets.toAddress( dbId ) );
      }
    };
    return filter;
  }
  
  private synchronized void startDbPool( ServiceConfiguration config ) {
    if ( Hosts.Coordinator.INSTANCE.isLocalhost( ) || config.isHostLocal( ) ) {
      return;
    }
    while ( !Hosts.Coordinator.INSTANCE.isLocalhost( ) && !Hosts.Coordinator.INSTANCE.get( ).hasBootstrapped( ) ) {
      LOG.info( "Waiting for primary cloud controller to bootstrap: " + Hosts.Coordinator.INSTANCE.get( ) );
      try {
        TimeUnit.SECONDS.sleep( 1 );
      } catch ( InterruptedException ex ) {
        Thread.currentThread( ).interrupt( );
      }
    }
    try {
      Iterables.find( Hosts.list( ), filterDbHost( config.getHostName( ) ) );
    } catch ( NoSuchElementException ex ) {
      return;
    }
    for ( String ctx : PersistenceContexts.list( ) ) {
      final String contextName = ctx.startsWith( "eucalyptus_" )
        ? ctx
        : "eucalyptus_" + ctx;
      
      String dbUrl = "jdbc:" + ServiceUris.remote( Database.class, config.getInetAddress( ), contextName );
      
      try {
        DriverDatabaseClusterMBean cluster = findDbClusterMBean( contextName );
        String dbPass = SystemIds.databasePassword( );
        final String hostName = config.getHostName( );
        String realJdbcDriver = Databases.getDriverName( );
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
        if ( Hosts.Coordinator.INSTANCE.isLocalhost( ) ) {
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
  
  private synchronized void stopDbPool( ServiceConfiguration config ) {
    if ( Hosts.Coordinator.INSTANCE.isLocalhost( ) || config.isHostLocal( ) ) {
      return;
    }
    final String hostName = config.getHostName( );
    
    for ( String ctx : PersistenceContexts.list( ) ) {
      final String contextName = ctx.startsWith( "eucalyptus_" )
        ? ctx
        : "eucalyptus_" + ctx;
      
      try {
        DriverDatabaseClusterMBean cluster = findDbClusterMBean( contextName );
        
        try {
          if ( cluster.getActiveDatabases( ).contains( hostName ) ) {
            cluster.deactivate( hostName );
          }
          if ( cluster.getInactiveDatabases( ).contains( hostName ) ) {
            cluster.remove( hostName );
          }
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      } catch ( NoSuchElementException ex1 ) {
        LOG.error( ex1, ex1 );
      } catch ( Exception ex1 ) {
        LOG.error( ex1, ex1 );
      }
    }
  }
  
}
