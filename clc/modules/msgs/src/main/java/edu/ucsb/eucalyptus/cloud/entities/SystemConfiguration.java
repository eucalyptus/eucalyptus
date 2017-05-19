/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package edu.ucsb.eucalyptus.cloud.entities;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.entities.EntityCache;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.google.common.base.MoreObjects;
import javaslang.collection.Stream;
import javaslang.control.Option;

/**
 *
 */
public class SystemConfiguration implements Comparable<SystemConfiguration> {

  private static final AtomicReference<SystemConfiguration> instance = new AtomicReference<>(
      new SystemConfiguration( PersistentSystemConfiguration.getSystemConfiguration( ) )
  );
  private static Logger logger = Logger.getLogger( SystemConfiguration.class );

  private final String dnsDomain;
  private final String nameserver;
  private final String nameserverAddress;

  public SystemConfiguration( final PersistentSystemConfiguration persistentSystemConfiguration ) {
    this(
        persistentSystemConfiguration.getDnsDomain( ),
        persistentSystemConfiguration.getNameserver( ),
        persistentSystemConfiguration.getNameserverAddress( )
    );
  }

  public SystemConfiguration( final String dnsDomain, final String nameserver, final String nameserverAddress ) {
    this.dnsDomain = dnsDomain;
    this.nameserver = nameserver;
    this.nameserverAddress = nameserverAddress;
  }

  @Nonnull
  public String getDnsDomain( ) {
    return dnsDomain;
  }

  @Nonnull
  public String getNameserver( ) {
    return nameserver;
  }

  @Nonnull
  public String getNameserverAddress( ) {
    return nameserverAddress;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final SystemConfiguration that = (SystemConfiguration) o;
    return Objects.equals( dnsDomain, that.dnsDomain ) &&
        Objects.equals( nameserver, that.nameserver ) &&
        Objects.equals( nameserverAddress, that.nameserverAddress );
  }

  @Override
  public int hashCode() {
    return Objects.hash( dnsDomain, nameserver, nameserverAddress );
  }

  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "dnsDomain", getDnsDomain( ) )
        .add( "nameserver", getNameserver( ) )
        .add( "nameserverAddress", getNameserverAddress( ) )
        .toString( );
  }

  @Override
  public int compareTo( final SystemConfiguration other ) {
    return toString( ).compareTo( String.valueOf( other ) );
  }

  @Nonnull
  public static SystemConfiguration getSystemConfiguration() {
    return instance.get( );
  }

  public static final class SystemConfigurationEventListener implements EventListener<ClockTick> {
    private static final EntityCache<PersistentSystemConfiguration,SystemConfiguration> systemConfigCache =
        new EntityCache<PersistentSystemConfiguration, SystemConfiguration>(
            new PersistentSystemConfiguration( ),
            SystemConfiguration::new
        );

    public static void register( ) {
      Listeners.register( ClockTick.class, new SystemConfigurationEventListener( ) );
    }

    @Override
    public void fireEvent( final ClockTick event ) {
      if ( !Databases.isVolatile( ) ) {
        final Option<SystemConfiguration> configOption = Stream.ofAll( systemConfigCache.get( ) ).headOption( );
        if ( configOption.isEmpty( ) ) {
          // was deleted from db?
          logger.info( "Recreating system configuration" );
          instance.set( new SystemConfiguration( PersistentSystemConfiguration.getSystemConfiguration( ) ) );
        } else {
          configOption.forEach( config -> {
            if ( !instance.get( ).equals( config ) ) {
              logger.info( "Reloaded system configuration: " + config );
              instance.set( config );
            }
          } );
        }
      }
    }
  }
}
