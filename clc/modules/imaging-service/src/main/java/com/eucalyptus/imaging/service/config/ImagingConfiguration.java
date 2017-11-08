/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.imaging.service.config;

import static com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import java.io.Serializable;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;

import org.apache.log4j.Logger;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceBuilder;
import com.eucalyptus.component.ServiceBuilders;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.imaging.common.Imaging;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.config.ComponentConfiguration;
import com.eucalyptus.upgrade.Upgrades;
import com.google.common.base.Predicate;

/**
 *
 */
@Entity
@PersistenceContext( name="eucalyptus_config" )
@ComponentPart( Imaging.class )
public class ImagingConfiguration extends ComponentConfiguration implements Serializable {
  private static final long serialVersionUID = 1L;

  public static final String SERVICE_PATH= "/services/Imaging";

  public ImagingConfiguration() { }

  public ImagingConfiguration( String partition, String name, String hostName, Integer port ) {
    super( partition, name, hostName, port, SERVICE_PATH );
  }

  @EntityUpgrade( entities = ImagingConfiguration.class, value = Imaging.class, since = Upgrades.Version.v4_1_0 )
  public enum Imaging410RegistrationUpgrade implements Predicate<Class> {
    INSTANCE;

    private static final Logger logger = Logger.getLogger( Imaging410RegistrationUpgrade.class );

    @Override
    public boolean apply( @Nullable final Class entityClass ) {
      try {
        if ( ServiceConfigurations.list( Imaging.class ).isEmpty() ) {
          final String imaging = ComponentIds.lookup( Imaging.class ).name( );
          final String compute = ComponentIds.lookup( Compute.class ).name( );
          final ServiceBuilder builder = ServiceBuilders.lookup( Imaging.class );
          for ( final ServiceConfiguration configuration : ServiceConfigurations.list( Compute.class ) ) {
            final String imagingServiceName;
            if ( configuration.getName( ).equals( configuration.getPartition( ) + "." + compute ) ) {
              imagingServiceName = configuration.getPartition( ) + "." + imaging;
            } else { // use host based naming
              imagingServiceName = configuration.getHostName( ) + "_" + imaging;
            }
            try {
              ServiceConfigurations.lookupByName( Imaging.class, imagingServiceName );
              logger.warn( "Existing imaging service found with name: " + imagingServiceName );
            } catch ( final NoSuchElementException e ) {
              logger.info( "Registering imaging service on host " + configuration.getHostName() );
              ServiceConfigurations.store( builder.newInstance(
                  configuration.getPartition( ),
                  imagingServiceName,
                  configuration.getHostName( ),
                  configuration.getPort( ) ) );
            }
          }
        } else {
          logger.info( "Not registering imaging services on upgrade, existing service found" );
        }
      } catch ( final Exception e ) {
        logger.error( "Error registering imaging services on upgrade", e );
      }
      return true;
    }
  }

}
