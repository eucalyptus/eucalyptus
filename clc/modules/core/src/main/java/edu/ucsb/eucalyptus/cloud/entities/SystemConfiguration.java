/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2017 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
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
import io.vavr.collection.Stream;
import io.vavr.control.Option;

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
