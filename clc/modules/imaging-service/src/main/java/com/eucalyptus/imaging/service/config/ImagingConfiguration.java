/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
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
