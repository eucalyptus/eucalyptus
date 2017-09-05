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
  static enum Dns420RegistrationUpgrade implements Predicate<Class> {
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