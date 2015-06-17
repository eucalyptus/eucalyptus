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
package com.eucalyptus.dns

import com.eucalyptus.component.id.Dns
import com.eucalyptus.dns.service.config.DnsConfiguration
import com.eucalyptus.component.ComponentIds
import com.eucalyptus.component.ServiceBuilder
import com.eucalyptus.component.ServiceBuilders
import com.eucalyptus.component.ServiceConfiguration
import com.eucalyptus.component.ServiceConfigurations
import com.eucalyptus.component.id.Eucalyptus
import com.eucalyptus.upgrade.Upgrades
import com.google.common.base.Predicate

import javax.annotation.Nullable
import static com.eucalyptus.upgrade.Upgrades.EntityUpgrade
import org.apache.log4j.Logger

class DnsUpgrades {
  @EntityUpgrade( entities = DnsConfiguration.class, value = Dns.class, since = Upgrades.Version.v4_2_0 )
  enum Dns420RegistrationUpgrade implements Predicate<Class> {
    INSTANCE

    protected static final Logger logger = Logger.getLogger( Dns420RegistrationUpgrade )

    @Override
    boolean apply( @Nullable final Class entityClass ) {
      try {
        if ( !ServiceConfigurations.list( Eucalyptus ).isEmpty( ) &&
        ServiceConfigurations.list( Dns ).isEmpty( ) ) {
          final String eucalyptus = ComponentIds.lookup( Eucalyptus ).name( )
          final String dns = ComponentIds.lookup( Dns ).name( )
          final ServiceBuilder builder = ServiceBuilders.lookup( Dns )
          ServiceConfigurations.list( Eucalyptus ).each{ ServiceConfiguration configuration ->
            final String dnsServiceName
            if ( configuration.name.equals( "${configuration.partition}.${eucalyptus}" as String ) ) {
              dnsServiceName = "${configuration.partition}.${dns}"
            } else { // use host based naming
              dnsServiceName = "${configuration.hostName}_${dns}"
            }
            try {
              ServiceConfigurations.lookupByName( Dns, dnsServiceName )
              logger.warn( "Existing DNS service found with name: " + dnsServiceName )
            } catch ( final NoSuchElementException nsee ) {
              logger.info( "Registering DNS service on host " + configuration.hostName )
              ServiceConfigurations.store( builder.newInstance(
                  configuration.partition,
                  dnsServiceName,
                  configuration.hostName,
                  configuration.port ) )
            }
          }
        } else {
          logger.info( "Not registering DNS services on upgrade" )
        }
      } catch ( final Exception e ) {
        logger.error( "Error registering DNS services on upgrade", e )
      }
      true
    }
  }

}