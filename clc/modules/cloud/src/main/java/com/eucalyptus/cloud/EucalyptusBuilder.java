package com.eucalyptus.cloud;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.bootstrap.Host;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentId.ComponentPart;
import com.eucalyptus.component.Faults;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Internets;
import com.google.common.base.Predicate;

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
      throw Faults.failure( config, Exceptions.error( "There is no host in the system (yet) for the given cloud controller configuration: "
        + config.getFullName( )
        + ".\nHosts are: "
        + Hosts.list( ) ) );
    } else if ( config.isVmLocal( ) && !Hosts.isCoordinator( ) ) {
      throw Faults.failure( config, Exceptions.error( "This cloud controller "
        + config.getFullName( )
        + " is not currently the coordinator "
        + Hosts.list( ) ) );
    }
  }
  
  @Override
  public void fireDisable( ServiceConfiguration config ) throws ServiceRegistrationException {}
  
  @Override
  public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException {}
  
  @Override
  public void fireCheck( ServiceConfiguration config ) throws ServiceRegistrationException {
    Host coordinator = Hosts.getCoordinator( );
    if ( coordinator == null ) {
      throw Faults.failure( config, Exceptions.error( config.getFullName( ) + ":fireCheck(): failed to lookup coordinator (" + coordinator + ")." ) );
    } else if ( coordinator.isLocalHost( ) ) {
      Check.COORDINATOR.apply( config );
    } else if ( !coordinator.isLocalHost( ) ) {
      Check.SECONDARY.apply( config );
    }
  }
  
  enum Check implements Predicate<ServiceConfiguration> {
    COORDINATOR {
      
      @Override
      public boolean apply( ServiceConfiguration config ) {
//GRZE: No service check makes sense here.
        return true;
      }
    },
    SECONDARY {
      
      @Override
      public boolean apply( ServiceConfiguration config ) {
        if ( config.isVmLocal( ) ) {
          if ( !Databases.isSynchronized( ) ) {
            throw Faults.failure( config,
                                  Exceptions.error( config.getFullName( )
                                    + ":fireCheck(): eucalyptus service " + config.getFullName( ) + " is currently synchronizing: "
                                    + Hosts.getCoordinator( ) ) );
          } else if ( Topology.isEnabledLocally( Eucalyptus.class ) ) {
            throw Faults.failure( config,
                                  Exceptions.error( config.getFullName( )
                                    + ":fireCheck(): eucalyptus service " + config.getFullName( ) + " cant be enabled when it is not the coordinator: "
                                    + Hosts.getCoordinator( ) ) );
          } else {
            LOG.debug( config.getFullName( ) + ":fireCheck() completed with coordinator currently: " + Hosts.getCoordinator( ) );
          }
        }
        return true;
      }
    };
    
    @Override
    public abstract boolean apply( ServiceConfiguration input );
  }
}
